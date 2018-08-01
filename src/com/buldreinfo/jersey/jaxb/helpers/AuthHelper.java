package com.buldreinfo.jersey.jaxb.helpers;

import javax.servlet.http.HttpServletRequest;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.google.common.net.HttpHeaders;

public class AuthHelper {
	private final AuthAPI auth;

	public AuthHelper() {
		this.auth = new AuthAPI("buldreinfo.auth0.com", "zexpFfou6HkgNWH5QVi3zyT1rrw6MXAn", "Yi7viH5URp9kJO0LhvSRQS-8Y6F2BR6_UIdx96KhbhtsbOe9HtFtOBcl6v55iT7o");
	}
	
	public int getUserId(HttpServletRequest request) {
		try {
			String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
			Request<UserInfo> req = auth.userInfo(authorization.substring(7));
			UserInfo info = req.execute();
			String sub = (String) info.getValues().get("sub");
			return Integer.parseInt(sub.replace("auth0|",""));
		} catch (Exception e) {
			return -1;
		}
	}
}
