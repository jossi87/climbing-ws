package com.buldreinfo.security;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class TokenService {
    private static final Logger logger = LogManager.getLogger();
    private static final String DOMAIN = "climbing.eu.auth0.com";
    
    private final JwkProvider jwkProvider;
    private final LoadingCache<String, Auth0Profile> cache;
    private final UserRepository userRepo;
    private final MediaRepository mediaRepo;
    private final StorageManager storage;

    public TokenService(UserRepository userRepo, MediaRepository mediaRepo, StorageManager storage) {
        this.userRepo = userRepo;
        this.mediaRepo = mediaRepo;
        this.storage = storage;
        this.jwkProvider = new UrlJwkProvider("https://" + DOMAIN + "/");
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(12))
                .build(new CacheLoader<>() {
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
                        return Auth0Profile.from(verifier.verify(accessToken));
                    }
                });
    }

    public Optional<Integer> processAuthentication(String accessToken, Setup setup, String headerJson) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            boolean isNewToken = cache.getIfPresent(accessToken) == null;
            Auth0Profile profile = cache.get(accessToken);
            int userId = userRepo.getAuthUserId(profile).orElseThrow();
            
            if (isNewToken) {
                userRepo.upsertUserLogin(setup, userId, headerJson);
                if (profile.picture() != null && !userRepo.hasAvatar(userId)) {
                    applyAvatarFromUrl(userId, profile.picture());
                }
            }
            logger.info("TokenService.processAuthentication() - userId={}, duration={}", userId, stopwatch);
            return Optional.of(userId);
        } catch (Exception e) {
            logger.warn("TokenService.processAuthentication() - Auth failed: {} - duration={}", e.getMessage(), stopwatch);
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
}