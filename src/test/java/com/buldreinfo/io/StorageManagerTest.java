package com.buldreinfo.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.AppConfig;
import com.buldreinfo.infrastructure.CacheConstants;

/**
 * Guards the storage client wiring: the S3 client is built with the Apache 5 HTTP client and the presigner
 * (which has no HTTP client of its own and falls back to whatever the SDK discovers on the classpath) still
 * builds, now that Apache HttpClient 4.x is excluded in the pom. Building the clients and signing a URL needs
 * no network, no credentials and no database, so this stays a fast unit test.
 */
public class StorageManagerTest {
	private static final AppConfig APP_CONFIG = new AppConfig(
			"test-access-key", "test-secret-key", "test-apify-token", null, "test-google-key", "test-vegvesen-auth");

	@Test
	public void buildsStorageClientsAndPresignsUploadUrl() {
		StorageManager storage = new StorageManager(APP_CONFIG, new ConcurrentMapCacheManager(CacheConstants.EXISTS_CACHE_NAME));
		storage.init();
		try {
			String key = S3KeyGenerator.getOriginalMp4(12345, StorageType.MP4);
			String url = storage.generatePresignedPutUrl(key, StorageType.MP4.getMimeType(), 1024);

			assertTrue(url.startsWith("https://"), url);
			assertTrue(url.contains("climbing-web"), url);
			assertTrue(url.contains("original/mp4/12300/12345.mp4"), url);
			assertTrue(url.contains("X-Amz-Signature="), url);
			// 15 minute expiry, as configured in StorageManager.
			assertTrue(url.contains("X-Amz-Expires=900"), url);
			// The frontend sends this header itself when it uploads to the URL.
			assertTrue(url.contains("x-amz-acl"), url);
		} finally {
			storage.cleanup();
		}
	}
}
