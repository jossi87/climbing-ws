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
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

public class AuthHelper {
	private static Logger logger = LogManager.getLogger();
	private static AuthAPI auth = new AuthAPI("climbing.eu.auth0.com", "DNJNVzhxbF7PtaBFh7H6iBSNLh2UJWHt", "gTycciaaWFspUL6tJvGMxFMprMSRypGGlXiwHeFWLCDbO8BRe6Tatz6ItrajwLFm");
	private static LoadingCache<String, Auth0Profile> cache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(4, TimeUnit.HOURS)
			.build(new CacheLoader<String, Auth0Profile>() {
				@Override
				public Auth0Profile load(String authorization) throws Exception {
					try {
						Request<UserInfo> req = auth.userInfo(authorization.substring(7));
						UserInfo info = req.execute();
						Map<String, Object> values = info.getValues();
						return new Auth0Profile(values);
					} catch (Exception ex) {
						logger.warn(ex.getMessage(), ex);
						throw ex;
					}
				}
			});

	public int getUserId(DbConnection c, HttpServletRequest request, int regionId) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Strings.isNullOrEmpty(authorization)) {
			return -1;
		}
		try {
			boolean update = cache.getIfPresent(authorization) == null;
			Auth0Profile profile = cache.get(authorization);
			int userId = c.getBuldreinfoRepo().getAuthUserId(profile);
			if (update) {
				// Log login
				Gson gson = new Gson();
				String headers = gson.toJson(getHeadersInfo(request));
				PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_login (user_id, region_id, headers) VALUES (?, ?, ?)");
				ps.setInt(1, userId);
				ps.setInt(2, regionId);
				ps.setString(3, headers);
				ps.execute();
				ps.close();
			}
			return userId;
		} catch (Exception e) {
			logger.warn("getUserId(authorizationHeader={}) - authentication failed, login required", authorization);
			return -1;
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
}