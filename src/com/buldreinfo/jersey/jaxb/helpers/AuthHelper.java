package com.buldreinfo.jersey.jaxb.helpers;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;

public class AuthHelper {
	private static Logger logger = LogManager.getLogger();
	private static AuthAPI auth = new AuthAPI("buldreinfo.auth0.com", "zexpFfou6HkgNWH5QVi3zyT1rrw6MXAn", "Yi7viH5URp9kJO0LhvSRQS-8Y6F2BR6_UIdx96KhbhtsbOe9HtFtOBcl6v55iT7o");
	private static LoadingCache<String, Integer> cache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(4, TimeUnit.HOURS)
			.build(new CacheLoader<String, Integer>() {
				@Override
				public Integer load(String authorization) throws Exception {
					try {
						Request<UserInfo> req = auth.userInfo(authorization.substring(7));
						UserInfo info = req.execute();
						String sub = (String) info.getValues().get("sub");
						return Integer.parseInt(sub.replace("auth0|",""));
					} catch (Exception ex) {
						logger.warn(ex.getMessage(), ex);
						throw ex;
					}
				}
			});

	public int getUserId(HttpServletRequest request) {
		try {
			String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
			Preconditions.checkArgument(!Strings.isNullOrEmpty(authorization));
			return cache.get(authorization);
		} catch (Exception e) {
			return -1;
		}
	}
}
