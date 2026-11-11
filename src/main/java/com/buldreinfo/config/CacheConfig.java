package com.buldreinfo.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buldreinfo.infrastructure.CacheConstants;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

@Configuration
@EnableCaching
public class CacheConfig {
	private static final Duration CACHE_TTL = Duration.ofMinutes(30);
	private static final Duration CACHE_NEGATIVE_TTL = Duration.ofMinutes(1);

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(
				CacheConstants.AUTH_CACHE_NAME,
				CacheConstants.HIT_COOLDOWN_CACHE_NAME,
				CacheConstants.EXISTS_CACHE_NAME,
				CacheConstants.REGION_CACHE_NAME,
				CacheConstants.CORS_CACHE_NAME,
				CacheConstants.WEBCAM_CACHE_NAME
				);

		cacheManager.setCaffeine(Caffeine.newBuilder()
				.recordStats()
				.expireAfter(new ValueLifetime())
				.maximumSize(200000));

		cacheManager.registerCustomCache(CacheConstants.FRONTPAGE_CACHE_NAME,
				Caffeine.newBuilder()
				.recordStats()
				.expireAfterWrite(30, TimeUnit.SECONDS)
				.maximumSize(100)
				.build());

		// Windy image URLs are tokenized and expire (10 minutes on the free tier), so keep this cache short-lived.
		cacheManager.registerCustomCache(CacheConstants.WINDY_WEBCAM_CACHE_NAME,
				Caffeine.newBuilder()
				.recordStats()
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.maximumSize(10)
				.build());

		return cacheManager;
	}

	@SuppressWarnings("unused")
	private static final class ValueLifetime implements Expiry<Object, Object> {
		@Override
		public long expireAfterCreate(Object key, Object value, long currentTime) {
			return ttlNanos(value);
		}

		@Override
		public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
			return ttlNanos(value);
		}

		@Override
		public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
			return currentDuration;
		}

		private static long ttlNanos(Object value) {
			return (Boolean.FALSE.equals(value) ? CACHE_NEGATIVE_TTL : CACHE_TTL).toNanos();
		}
	}
}
