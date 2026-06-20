package com.buldreinfo.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HitTrackingFilter extends OncePerRequestFilter {
    public static final String SHOULD_UPDATE_HITS_KEY = "shouldUpdateHits";
    private static final int HITS_COOLDOWN_CACHE_MAX_SIZE = 200000;
    private static final long HITS_COOLDOWN_MILLIS = Duration.ofMinutes(30).toMillis();
    private static final Map<String, Long> hitsCooldownMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        request.setAttribute(SHOULD_UPDATE_HITS_KEY, shouldUpdateHits(request));
        filterChain.doFilter(request, response);
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
        long now = System.currentTimeMillis();
        String key = getHitsCooldownKey(request);
        Long previous = hitsCooldownMap.putIfAbsent(key, now);

        if (previous == null) {
            if (hitsCooldownMap.size() >= HITS_COOLDOWN_CACHE_MAX_SIZE) cleanupHitsCooldownMap(now);
            return true;
        }

        if (now - previous >= HITS_COOLDOWN_MILLIS) {
            if (hitsCooldownMap.replace(key, previous, now)) {
                cleanupHitsCooldownMap(now);
                return true;
            }
        }
        return false;
    }
}