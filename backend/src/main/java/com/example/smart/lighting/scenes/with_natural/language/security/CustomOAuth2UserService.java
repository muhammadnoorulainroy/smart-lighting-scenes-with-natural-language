package com.example.smart.lighting.scenes.with_natural.language.security;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oidcUser);
        } catch (OAuth2AuthenticationException ex) {
            log.error("OAuth2 authentication error", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            OAuth2Error oauth2Error = new OAuth2Error("oauth2_processing_error",
                "Error processing OAuth2 user: " + ex.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, ex.getMessage(), ex);
        }
    }

    private OidcUser processOAuth2User(OidcUserRequest oAuth2UserRequest, OidcUser oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        String providerSub = extractProviderSub(attributes, providerId);
        String email = extractEmail(attributes);
        String name = extractName(attributes);
        String pictureUrl = extractPictureUrl(attributes);

        if (email == null || email.isEmpty()) {
            log.error("Email not provided by OAuth provider");
            OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info",
                "Email is required but not provided by OAuth provider", null);
            throw new OAuth2AuthenticationException(oauth2Error);
        }

        if (providerSub == null || providerSub.isEmpty()) {
            log.error("Provider sub not provided for email: {}", email);
            OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info",
                "Provider sub is required but not provided", null);
            throw new OAuth2AuthenticationException(oauth2Error);
        }

        User.OAuthProvider provider;
        try {
            provider = User.OAuthProvider.valueOf(providerId.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unknown provider: {}", providerId);
            OAuth2Error oauth2Error = new OAuth2Error("invalid_provider",
                "Unknown OAuth provider: " + providerId, null);
            throw new OAuth2AuthenticationException(oauth2Error, e.getMessage(), e);
        }

        User user;
        try {
            user = userRepository.findByProviderAndProviderSub(provider, providerSub)
            .map(existingUser -> {
                log.info("Found user by provider+sub: {}", email);
                return updateExistingUser(existingUser, name, pictureUrl, providerSub, provider);
            })
            .orElseGet(() -> userRepository.findByEmail(email)
                    .map(existingUser -> {
                        log.info("Found user by email: {}", email);
                        return updateExistingUser(existingUser, name, pictureUrl, providerSub, provider);
                    })
                    .orElseGet(() -> {
                        log.info("Creating new user: {}", email);
                        return registerNewUser(providerId, providerSub, email, name, pictureUrl);
                    }));

            log.info("User processed successfully: {} with role {}", email, user.getRole());
        } catch (Exception e) {
            log.error("Error processing user {}: {}", email, e.getMessage(), e);
            OAuth2Error oauth2Error = new OAuth2Error("user_processing_error",
                "Failed to process user: " + e.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, e.getMessage(), e);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }

    private String extractProviderSub(Map<String, Object> attributes, String providerId) {
        if ("google".equals(providerId)) {
            return (String) attributes.get("sub");
        }
        OAuth2Error oauth2Error = new OAuth2Error("invalid_provider",
            "Unknown provider: " + providerId, null);
        throw new OAuth2AuthenticationException(oauth2Error);
    }

    private String extractEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }

    private String extractName(Map<String, Object> attributes) {
        Object name = attributes.get("name");
        return name != null ? name.toString() : "User";
    }

    private String extractPictureUrl(Map<String, Object> attributes) {
        Object picture = attributes.get("picture");
        return picture != null ? picture.toString() : null;
    }

    private User updateExistingUser(User existingUser, String name, String pictureUrl,
                                    String providerSub, User.OAuthProvider provider) {
        try {
            existingUser.setName(name);
            existingUser.setPictureUrl(pictureUrl);
            existingUser.setProviderSub(providerSub);
            existingUser.setProvider(provider);

            if (existingUser.getEmail().equals("nimrafayaz9@gmail.com")) {
                existingUser.setRole(User.UserRole.OWNER);
                existingUser.setIsActive(true);
                log.info("Set OWNER role for: {}", existingUser.getEmail());
            }

            return userRepository.save(existingUser);
        } catch (Exception e) {
            log.error("Error updating user {}: {}", existingUser.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    private User registerNewUser(String providerId, String providerSub, String email,
                                 String name, String pictureUrl) {
        log.info("Creating new user: {}", email);

        User.UserRole defaultRole = email.equals("nimrafayaz9@gmail.com")
            ? User.UserRole.OWNER
            : User.UserRole.GUEST;

        User newUser = User.builder()
            .email(email)
            .name(name)
            .pictureUrl(pictureUrl)
            .provider(User.OAuthProvider.valueOf(providerId.toUpperCase()))
            .providerSub(providerSub)
            .role(defaultRole)
            .isActive(true)
            .build();

        return userRepository.save(newUser);
    }
}
