package com.buldreinfo.jersey.jaxb.filters;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HitTrackingFilter implements ContainerRequestFilter {
	public static final String SHOULD_UPDATE_HITS_KEY = "shouldUpdateHits";
	private static final int HITS_COOLDOWN_CACHE_MAX_SIZE = 200000;
	private static final long HITS_COOLDOWN_MILLIS = Duration.ofMinutes(30).toMillis();
	private static final Map<String, Long> hitsCooldownMap = new ConcurrentHashMap<>();

	@Context
	private HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (shouldUpdateHits(request)) {
			requestContext.setProperty(SHOULD_UPDATE_HITS_KEY, true);
		} else {
			requestContext.setProperty(SHOULD_UPDATE_HITS_KEY, false);
		}
	}

	private synchronized void cleanupHitsCooldownMap(long now) {
		if (hitsCooldownMap.size() < HITS_COOLDOWN_CACHE_MAX_SIZE) {
			return;
		}
		hitsCooldownMap.entrySet().removeIf(e -> now - e.getValue() >= HITS_COOLDOWN_MILLIS);
		if (hitsCooldownMap.size() >= HITS_COOLDOWN_CACHE_MAX_SIZE) {
			hitsCooldownMap.clear();
		}
	}

	private String getHitsCooldownKey(HttpServletRequest request) {
		String ip = request.getHeader("CF-Connecting-IP");
		if (ip == null || ip.isBlank()) {
			ip = request.getHeader("X-Forwarded-For");
		}
		if (ip == null || ip.isBlank()) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.isBlank()) {
			ip = request.getRemoteAddr();
		}

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
		if (isBotRequest(request)) {
			return false;
		}
		long now = System.currentTimeMillis();
		String key = getHitsCooldownKey(request);
		Long previous = hitsCooldownMap.putIfAbsent(key, now);

		if (previous == null) {
			if (hitsCooldownMap.size() >= HITS_COOLDOWN_CACHE_MAX_SIZE) {
				cleanupHitsCooldownMap(now);
			}
			return true;
		}

		if (now - previous >= HITS_COOLDOWN_MILLIS) {
			if (hitsCooldownMap.replace(key, previous, now)) {
				cleanupHitsCooldownMap(now);
				return true;
			}
			return false;
		}
		return false;
	}
}