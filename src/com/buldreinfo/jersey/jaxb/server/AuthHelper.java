package com.buldreinfo.jersey.jaxb.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.auth0.net.Response;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;

public class AuthHelper {
	private static Logger logger = LogManager.getLogger();
	private static AuthAPI auth = new AuthAPI.Builder("climbing.eu.auth0.com", "DNJNVzhxbF7PtaBFh7H6iBSNLh2UJWHt").withClientSecret(BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_AUTH0_CLIENT_SECRET)).build();
	private static LoadingCache<String, Auth0Profile> cache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(4, TimeUnit.HOURS)
			.build(new CacheLoader<String, Auth0Profile>() {
				@Override
				public Auth0Profile load(String authorization) throws Exception {
					try {
						Request<UserInfo> req = auth.userInfo(authorization);
						Response<UserInfo> info = req.execute();
						Map<String, Object> values = info.getBody().getValues();
						return Auth0Profile.from(values);
					} catch (Exception ex) {
						logger.warn("Login failed: " + ex.getMessage());
						return null;
					}
				}
			});

	private Optional<Integer> getAuthUserId(Connection c, HttpServletRequest request, Setup setup, String accessToken) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			boolean update = cache.getIfPresent(accessToken) == null;
			Auth0Profile profile = cache.get(accessToken);
			Optional<Integer> authUserId = Server.getDao().getAuthUserId(c, profile);
			if (update) {
				Gson gson = new Gson();
				String headers = gson.toJson(getHeadersInfo(request));
				// Log login
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_login (user_id, region_id, headers) VALUES (?, ?, ?)")) {
					ps.setInt(1, authUserId.orElseThrow());
					ps.setInt(2, setup.getIdRegion());
					ps.setString(3, headers);
					ps.execute();
				}
			}
			logger.info("getAuthUserId(accessToken={}) - authUserId={}, duration={}", accessToken, authUserId, stopwatch);
			return authUserId;
		} catch (Exception e) {
			logger.warn("getAuthUserId(accessToken={}) - authentication failed, login required - duration={}", accessToken, stopwatch);
			return Optional.empty();
		}
	}

	private Map<String, String> getHeadersInfo(HttpServletRequest request) {
		Map<String, String> map = new HashMap<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}
		return map;
	}

	protected Optional<Integer> getAuthUserId(Connection c, HttpServletRequest request, Setup setup) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Strings.isNullOrEmpty(authorization)) {
			return Optional.empty();
		}
		String accessToken = authorization.substring(7); // Remove "Bearer "
		return getAuthUserId(c, request, setup, accessToken);
	}
}