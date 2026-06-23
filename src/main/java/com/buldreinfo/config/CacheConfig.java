package com.buldreinfo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buldreinfo.infrastructure.CacheConstants;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheConstants.AUTH_CACHE_NAME,
                CacheConstants.EXISTS_CACHE_NAME,
                CacheConstants.REGION_CACHE_NAME,
                CacheConstants.CORS_CACHE_NAME
        );
    }
}