package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service for mobile app authentication with Google.
 */
@Service
@Slf4j
public class MobileAuthService {

    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier verifier;

    public MobileAuthService(
            UserRepository userRepository,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {

        this.userRepository = userRepository;
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        log.info("MobileAuthService initialized");
    }

    /**
     * Verifies Google ID token and finds or creates user.
     */
    public User authenticateWithGoogle(String idTokenString) throws Exception {
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new SecurityException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String sub = payload.getSubject();

        if (!payload.getEmailVerified()) {
            throw new SecurityException("Email not verified");
        }

        log.info("Verified Google token for: {}", email);

        // Find or create user
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating new mobile user: {}", email);
            User newUser = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .pictureUrl(picture)
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .providerSub(sub)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });
    }
}
