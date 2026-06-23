package com.buldreinfo.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
	private static final Set<String> LOGGED_HEADER_ALLOWLIST = Set.of("user-agent", "x-forwarded-for", "x-real-ip", "cf-connecting-ip", "accept-language", "origin", "referer");
	private static final Set<String> REDACTED_HEADER_NAMES = Set.of("authorization", "cookie", "set-cookie", "x-api-key");
	private final ObjectMapper objectMapper;
	private final RegionRepository regionRepo;
	private final TokenService tokenService;

	public JwtFilter(ObjectMapper objectMapper, TokenService tokenService, RegionRepository regionRepo) {
		this.objectMapper = objectMapper;
		this.tokenService = tokenService;
		this.regionRepo = regionRepo;
	}

	private String extractToken(HttpServletRequest request) {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!Strings.isNullOrEmpty(authHeader) && authHeader.length() > 7) {
			return authHeader.substring(7);
		}
		return request.getParameter("access_token");
	}

	private Map<String, String> getHeaders(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		Set<String> seen = new HashSet<>();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String lower = name.toLowerCase(Locale.ROOT);
			if (REDACTED_HEADER_NAMES.contains(lower)) {
				map.put(name, "[REDACTED]");
				seen.add(lower);
			}
			else if (LOGGED_HEADER_ALLOWLIST.contains(lower)) {
				map.put(name, request.getHeader(name));
				seen.add(lower);
			}
		}
		if (!seen.contains("authorization") && request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			map.put(HttpHeaders.AUTHORIZATION, "[REDACTED]");
		}
		return map;
	}

	private Setup getSetup(HttpServletRequest request) throws Exception {
		return regionRepo.getSetups().stream()
				.filter(s -> s.domain().equalsIgnoreCase(request.getServerName()))
				.findFirst()
				.orElseThrow();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String accessToken = extractToken(request);
		if (!Strings.isNullOrEmpty(accessToken)) {
			try {
				Setup setup = getSetup(request);
				String headerJson = objectMapper.writeValueAsString(getHeaders(request));
				tokenService.processAuthentication(accessToken, setup, headerJson)
				.ifPresent(userId -> {
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
					SecurityContextHolder.getContext().setAuthentication(auth);
				});
			} catch (Exception e) {
				logger.error("JWT Authentication failed", e);
			}
		}
		filterChain.doFilter(request, response);
	}
}