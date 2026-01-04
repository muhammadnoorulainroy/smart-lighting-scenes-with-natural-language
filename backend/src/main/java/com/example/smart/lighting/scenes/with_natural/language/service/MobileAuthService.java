package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for authenticating mobile app users with Google ID tokens.
 *
 * <p>Mobile apps cannot use the standard OAuth2 redirect flow, so they
 * obtain a Google ID token directly and send it to this service for
 * verification and session creation.</p>
 *
 * <h3>Authentication Flow:</h3>
 * <ol>
 *   <li>Mobile app authenticates with Google Sign-In</li>
 *   <li>Mobile app sends ID token to backend</li>
 *   <li>This service verifies the token with Google</li>
 *   <li>User is created/updated in database</li>
 *   <li>Session is created for subsequent requests</li>
 * </ol>
 *

 * @see GoogleIdTokenVerifier
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAuthService {

    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * Authenticates a mobile user using a Google ID token.
     *
     * @param idTokenString the Google ID token from the mobile app
     * @param request the HTTP request for session creation
     * @return the authenticated user's DTO
     * @throws Exception if token verification fails or email is not verified
     */
    @Transactional
    public UserDto authenticateWithGoogleToken(String idTokenString, HttpServletRequest request) throws Exception {
        log.info("Verifying Google ID token with client ID: {}", googleClientId != null ? googleClientId.substring(0, Math.min(20, googleClientId.length())) + "..." : "null");

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            log.error("Google ID token verification failed: token is null or invalid");
            throw new SecurityException("Invalid ID token - verification failed");
        }

        log.info("Google ID token verified successfully");

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String sub = payload.getSubject();

        if (!payload.getEmailVerified()) {
            throw new SecurityException("Email not verified");
        }

        User user = userRepository.findByProviderAndProviderSub(User.OAuthProvider.GOOGLE, sub)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existingUser -> {
                            existingUser.setProvider(User.OAuthProvider.GOOGLE);
                            existingUser.setProviderSub(sub);
                            existingUser.setName(name);
                            existingUser.setPictureUrl(pictureUrl);
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setName(name);
                            newUser.setPictureUrl(pictureUrl);
                            newUser.setProvider(User.OAuthProvider.GOOGLE);
                            newUser.setProviderSub(sub);
                            newUser.setRole(User.UserRole.GUEST);
                            return userRepository.save(newUser);
                        }));

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("picture", pictureUrl);
        claims.put("email_verified", true);

        OidcIdToken oidcIdToken = new OidcIdToken(
                idTokenString,
                Instant.ofEpochSecond(payload.getIssuedAtTimeSeconds()),
                Instant.ofEpochSecond(payload.getExpirationTimeSeconds()),
                claims
        );

        OidcUserInfo userInfo = new OidcUserInfo(claims);
        OidcUser oidcUser = new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                oidcIdToken,
                userInfo
        );

        CustomOAuth2User customUser = new CustomOAuth2User(oidcUser, user);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                customUser,
                null,
                customUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (request.getSession(false) == null) {
            request.getSession(true);
        }
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        log.info("Mobile authentication successful for user: {} with role: {}", email, user.getRole());

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
