package com.buldreinfo.jersey.jaxb.helpers;

import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.buldreinfo.jersey.jaxb.beans.Auth0Profile;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;
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
    private static final JwkProvider jwkProvider = new UrlJwkProvider("https://" + DOMAIN + "/");

    private static final LoadingCache<String, Auth0Profile> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(12, TimeUnit.HOURS)
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

    public Optional<Integer> getAuthUserId(Dao dao, Connection c, HttpServletRequest request, Setup setup) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;
        if (!Strings.isNullOrEmpty(authHeader) && authHeader.length() > 7) {
            accessToken = authHeader.substring(7);
        } 
        else {
            accessToken = request.getParameter("access_token");
        }
        if (Strings.isNullOrEmpty(accessToken)) {
            return Optional.empty();
        }
        return getAuthUserId(dao, c, request, setup, accessToken);
    }

    private Optional<Integer> getAuthUserId(Dao dao, Connection c, HttpServletRequest request, Setup setup, String accessToken) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            boolean isNewToken = cache.getIfPresent(accessToken) == null;
            Auth0Profile profile = cache.get(accessToken);
            Optional<Integer> authUserId = dao.getAuthUserId(c, profile);
            if (isNewToken && authUserId.isPresent()) {
                logLogin(c, request, setup, authUserId.get());
            }
            logger.info("getAuthUserId() - authUserId={}, duration={}", authUserId.orElse(null), stopwatch);
            return authUserId;
        } catch (Exception e) {
            logger.warn("getAuthUserId() - Auth failed or email missing: {} - duration={}", e.getMessage(), stopwatch);
            return Optional.empty();
        }
    }

    private void logLogin(Connection c, HttpServletRequest request, Setup setup, int userId) throws Exception {
        Gson gson = new Gson();
        String headers = gson.toJson(getHeaders(request));
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_login (user_id, region_id, headers) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, setup.idRegion());
            ps.setString(3, headers);
            ps.execute();
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getHeader(name));
        }
        return map;
    }
}