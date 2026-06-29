package com.buldreinfo.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buldreinfo.infrastructure.CacheConstants;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(
				CacheConstants.AUTH_CACHE_NAME,
				CacheConstants.HIT_COOLDOWN_CACHE_NAME,
				CacheConstants.EXISTS_CACHE_NAME,
				CacheConstants.REGION_CACHE_NAME,
				CacheConstants.CORS_CACHE_NAME
				);

		cacheManager.setCaffeine(Caffeine.newBuilder()
				.recordStats()
				.expireAfterWrite(30, TimeUnit.MINUTES)
				.maximumSize(200000));

		return cacheManager;
	}
}