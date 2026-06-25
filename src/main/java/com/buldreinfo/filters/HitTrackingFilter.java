package com.buldreinfo.filters;

import java.io.IOException;
import java.time.Duration;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buldreinfo.infrastructure.CacheConstants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HitTrackingFilter extends OncePerRequestFilter {
	public static final String SHOULD_UPDATE_HITS_KEY = "shouldUpdateHits";
	private static final long HITS_COOLDOWN_MILLIS = Duration.ofMinutes(30).toMillis();

	private final Cache cache;

	public HitTrackingFilter(CacheManager cacheManager) {
		this.cache = cacheManager.getCache(CacheConstants.HIT_COOLDOWN_CACHE_NAME);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		request.setAttribute(SHOULD_UPDATE_HITS_KEY, shouldUpdateHits(request));
		filterChain.doFilter(request, response);
	}

	private String getHitsCooldownKey(HttpServletRequest request) {
		String ip = request.getHeader("CF-Connecting-IP");
		if (ip == null || ip.isBlank()) ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
		if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();

		String uri = request.getRequestURI();
		String query = request.getQueryString();
		String ua = request.getHeader("User-Agent");

		return String.join("|",
				(uri == null ? "" : uri),
				(query == null ? "" : query),
				(ip == null ? "" : ip),
				(ua == null ? "" : ua));
	}

	private boolean isBotRequest(HttpServletRequest request) {
		String ua = request.getHeader("User-Agent");
		if (ua == null) return false;
		String x = ua.toLowerCase();
		return x.contains("bot") || x.contains("crawler") || x.contains("spider") ||
				x.contains("slurp") || x.contains("bingpreview") || x.contains("headless");
	}

	private boolean shouldUpdateHits(HttpServletRequest request) {
		if (isBotRequest(request)) return false;

		String key = getHitsCooldownKey(request);
		long now = System.currentTimeMillis();

		Cache.ValueWrapper wrapper = cache.get(key);

		if (wrapper == null) {
			cache.put(key, now);
			return true;
		}

		long lastHit = (long) wrapper.get();
		if (now - lastHit >= HITS_COOLDOWN_MILLIS) {
			cache.put(key, now);
			return true;
		}

		return false;
	}
}