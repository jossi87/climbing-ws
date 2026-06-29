package com.buldreinfo.infrastructure;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.filters.HitTrackingFilter;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestContext {
	private final RegionRepository regionRepo;

	public RequestContext(RegionRepository regionRepo) {
		this.regionRepo = regionRepo;
	}

	public boolean acceptsWebm(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("video/webm")) return true;

		String ua = request.getHeader("User-Agent");
		if (ua != null && (ua.contains("Chrome") || ua.contains("Firefox") || ua.contains("Edg/"))) {
			return !ua.contains("Like Mac OS X");
		}
		return false;
	}

	public boolean acceptsWebp(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("image/webp")) return true;

		String ua = request.getHeader("User-Agent");
		if (ua != null && ua.contains("Safari") && !ua.contains("Chrome")) {
			int idx = ua.indexOf("Version/");
			if (idx >= 0) {
				String versionPart = ua.substring(idx + 8).split(" ")[0].split("\\.")[0];
				try {
					return Integer.parseInt(versionPart) >= 14;
				} catch (NumberFormatException _) {
					return false;
				}
			}
		}
		return false;
	}

	public Optional<Integer> getAuthenticatedUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.getPrincipal() instanceof Integer userId) ? Optional.of(userId) : Optional.empty();
	}

	public Setup getSetup(HttpServletRequest request) {
		String serverName = request.getServerName();
		return regionRepo.getSetups().stream()
				.filter(s -> s.domain().equalsIgnoreCase(serverName))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
	}

	public boolean isHitTrackingEnabled(HttpServletRequest request) {
		Object attr = request.getAttribute(HitTrackingFilter.SHOULD_UPDATE_HITS_KEY);
		return (attr instanceof Boolean) && (Boolean) attr;
	}
}