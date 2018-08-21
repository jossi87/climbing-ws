package com.buldreinfo.jersey.jaxb.helpers;

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

	public int getUserId(DbConnection c, HttpServletRequest request) {
		try {
			String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (Strings.isNullOrEmpty(authorization)) {
				return -1;
			}
			Auth0Profile profile = cache.get(authorization);
			return c.getBuldreinfoRepo().getAuthUserId(profile);
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
			return -1;
		}
	}
}