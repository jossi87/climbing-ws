package com.buldreinfo.helpers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.buldreinfo.beans.Auth0Profile;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.User;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;

public class AuthHelper {
	private static final Logger logger = LogManager.getLogger();
	private static final String DOMAIN = "climbing.eu.auth0.com";
	private static final Set<String> LOGGED_HEADER_ALLOWLIST = Set.of(
			"user-agent",
			"x-forwarded-for",
			"x-real-ip",
			"cf-connecting-ip",
			"accept-language",
			"origin",
			"referer"
			);
	private static final Set<String> REDACTED_HEADER_NAMES = Set.of(
			"authorization",
			"cookie",
			"set-cookie",
			"x-api-key"
			);
	private static final Gson gson = new Gson();
	private static final JwkProvider jwkProvider = new UrlJwkProvider("https://" + DOMAIN + "/");
	private static final LoadingCache<String, Auth0Profile> cache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(Duration.ofHours(12))
			.build(new CacheLoader<String, Auth0Profile>() {
				@Override
				public Auth0Profile load(String accessToken) throws Exception {
					DecodedJWT jwt = JWT.decode(accessToken);
					RSAPublicKey publicKey = (RSAPublicKey) jwkProvider.get(jwt.getKeyId()).getPublicKey();
					Algorithm algorithm = Algorithm.RSA256(publicKey, null);
					JWTVerifier verifier = JWT.require(algorithm)
							.withIssuer("https://" + DOMAIN + "/")
							.withAudience("https://buldreinfo.com")
							.acceptLeeway(5)
							.build();
					DecodedJWT verifiedJwt = verifier.verify(accessToken);
					return Auth0Profile.from(verifiedJwt);
				}
			});

	public Optional<Integer> getAuthUserId(UserRepository userRepo, MediaRepository mediaRepo, HttpServletRequest request, Setup setup) {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String accessToken = null;
		if (!Strings.isNullOrEmpty(authHeader) && authHeader.length() > 7) {
			accessToken = authHeader.substring(7);
		} 
		else {
			accessToken = request.getParameter("access_token");
		}
		if (Strings.isNullOrEmpty(accessToken) || accessToken.isBlank()) {
			return Optional.empty();
		}
		return getAuthUserId(userRepo, mediaRepo, request, setup, accessToken);
	}

	private Optional<Integer> getAuthUserId(UserRepository userRepo, MediaRepository mediaRepo, HttpServletRequest request, Setup setup, String accessToken) {
	    Stopwatch stopwatch = Stopwatch.createStarted();
	    try {
	        boolean isNewToken = cache.getIfPresent(accessToken) == null;
	        Auth0Profile profile = cache.get(accessToken);
	        int userId = userRepo.getAuthUserId(profile).orElseThrow();
	        if (isNewToken) {
	            String headers = gson.toJson(getHeaders(request));
	            userRepo.upsertUserLogin(setup, userId, headers);
	            if (profile.picture() != null && !userRepo.hasAvatar(userId)) {
	                applyAvatarFromUrl(mediaRepo, userId, profile.picture());
	            }
	        }
	        logger.info("getAuthUserId() - authUserId={}, duration={}", userId, stopwatch);
	        return Optional.of(userId);
	    } catch (Exception e) {
	        logger.warn("getAuthUserId() - Auth failed: {} - duration={}", e.getMessage(), stopwatch);
	        return Optional.empty();
	    }
	}

	private void applyAvatarFromUrl(MediaRepository mediaRepo, int userId, String url) {
	    try {
	        byte[] avatarBytes;
	        try (var remoteStream = URI.create(url).toURL().openStream()) {
	            avatarBytes = StorageManager.getInstance().readBoundedStream(remoteStream);
	        }
	        var photographer = User.from(userId, null);
	        var m = new Media(null, false, 0, 0, false, false, null, null, photographer, null, null, null, 0, null, null, 0, false, null, null, null, null, 0, userId);
	        mediaRepo.addMediaImage(Optional.of(userId), m, StorageType.JPG, () -> new ByteArrayInputStream(avatarBytes));
	    } catch (Exception e) {
	        logger.error("Failed to apply login avatar", e);
	    }
	}

	private Map<String, String> getHeaders(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		Set<String> seen = new HashSet<>();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String lower = name.toLowerCase(Locale.ROOT);
			if (REDACTED_HEADER_NAMES.contains(lower)) {
				map.put(name, "[REDACTED]");
				seen.add(lower);
			}
			else if (LOGGED_HEADER_ALLOWLIST.contains(lower)) {
				map.put(name, request.getHeader(name));
				seen.add(lower);
			}
		}
		if (!seen.contains("authorization") && request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			map.put(HttpHeaders.AUTHORIZATION, "[REDACTED]");
		}
		return map;
	}
}