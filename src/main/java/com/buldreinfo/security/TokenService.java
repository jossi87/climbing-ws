package com.buldreinfo.security;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.auth0.jwk.InvalidPublicKeyException;
import com.auth0.jwk.JwkException;
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
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.User;

@Service
public class TokenService {
    private static final String DOMAIN = "climbing.eu.auth0.com";
    private static final Logger logger = LogManager.getLogger();
    private final CacheManager cacheManager;
    private final JwkProvider jwkProvider;
    private final MediaRepository mediaRepo;
    private final StorageManager storage;
    private final UserRepository userRepo;

    public TokenService(CacheManager cacheManager, UserRepository userRepo, MediaRepository mediaRepo, StorageManager storage) {
        this.jwkProvider = new UrlJwkProvider("https://" + DOMAIN + "/");
        this.cacheManager = cacheManager;
        this.userRepo = userRepo;
        this.mediaRepo = mediaRepo;
        this.storage = storage;
    }
    
    public Optional<Integer> processAuthentication(String accessToken, Setup setup, String headerJson) {
        try {
            var cache = cacheManager.getCache(CacheConstants.AUTH_CACHE_NAME);
            Auth0Profile profile = cache.get(accessToken, Auth0Profile.class);
            boolean isNewToken = (profile == null);
            if (isNewToken) {
                profile = verifyAndLoad(accessToken);
                cache.put(accessToken, profile);
            }
            int userId = userRepo.getAuthUserId(profile).orElseThrow();
            if (isNewToken) {
                userRepo.upsertUserLogin(setup, userId, headerJson);
                if (profile.picture() != null && !userRepo.hasAvatar(userId)) {
                    applyAvatarFromUrl(userId, profile.picture());
                }
            }
            return Optional.of(userId);
        } catch (Exception e) {
            logger.warn("Authentication failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void applyAvatarFromUrl(int userId, String url) {
        try {
            byte[] avatarBytes;
            try (var remoteStream = URI.create(url).toURL().openStream()) {
                avatarBytes = storage.readBoundedStream(remoteStream);
            }
            var photographer = User.from(userId, null);
            var m = new Media(null, false, 0, 0, false, false, null, null, photographer, null, null, null, 0, null, null, 0, false, null, null, null, null, 0, userId);
            mediaRepo.addMediaImage(Optional.of(userId), m, StorageType.JPG, () -> new ByteArrayInputStream(avatarBytes));
        } catch (Exception e) {
            logger.error("Failed to apply login avatar", e);
        }
    }

    private Auth0Profile verifyAndLoad(String accessToken) throws InvalidPublicKeyException, JwkException {
        DecodedJWT jwt = JWT.decode(accessToken);
        RSAPublicKey publicKey = (RSAPublicKey) jwkProvider.get(jwt.getKeyId()).getPublicKey();
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("https://" + DOMAIN + "/")
                .withAudience("https://buldreinfo.com")
                .acceptLeeway(5)
                .build();
        return Auth0Profile.from(verifier.verify(accessToken));
    }
}