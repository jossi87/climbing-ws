package com.buldreinfo.jersey.jaxb.helpers;

import java.sql.PreparedStatement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.auth0.net.Response;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

public class AuthHelper {
	private static Logger logger = LogManager.getLogger();
	private static String buldreinfoAuth0ClientSecret = Preconditions.checkNotNull(Strings.emptyToNull(System.getenv("buldreinfo_auth0_client_secret")), "Could not find environment variable \"buldreinfo_auth0_client_secret\"");
	private static AuthAPI auth = new AuthAPI.Builder("climbing.eu.auth0.com", "DNJNVzhxbF7PtaBFh7H6iBSNLh2UJWHt").withClientSecret(buldreinfoAuth0ClientSecret).build();
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
						return new Auth0Profile(values);
					} catch (Exception ex) {
						logger.warn(ex.getMessage(), ex);
						throw ex;
					}
				}
			});

	public int getUserId(DbConnection c, HttpServletRequest request, MetaHelper metaHelper, String accessToken) {
		try {
			boolean update = cache.getIfPresent(accessToken) == null;
			Auth0Profile profile = cache.get(accessToken);
			int userId = c.getBuldreinfoRepo().getAuthUserId(profile);
			if (update) {
				Setup setup = metaHelper.getSetup(request);
				Gson gson = new Gson();
				String headers = gson.toJson(getHeadersInfo(request));
				// Log login
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_login (user_id, region_id, headers) VALUES (?, ?, ?)")) {
					ps.setInt(1, userId);
					ps.setInt(2, setup.getIdRegion());
					ps.setString(3, headers);
					ps.execute();
				}
			}
			return userId;
		} catch (Exception e) {
			logger.warn("getUserId(accessToken={}) - authentication failed, login required", accessToken);
			return -1;
		}
	}

	public int getUserId(DbConnection c, HttpServletRequest request, MetaHelper metaHelper) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Strings.isNullOrEmpty(authorization)) {
			return -1;
		}
		String accessToken = authorization.substring(7); // Remove "Bearer "
		return getUserId(c, request, metaHelper, accessToken);
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
}