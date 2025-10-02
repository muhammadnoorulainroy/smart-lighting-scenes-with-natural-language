package com.example.smart.lighting.scenes.with_natural.language.security;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }

    private OidcUser processOAuth2User(OidcUserRequest oAuth2UserRequest, OidcUser oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String providerId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        String providerSub = extractProviderSub(attributes, providerId);
        String email = extractEmail(attributes);
        String name = extractName(attributes);
        String pictureUrl = extractPictureUrl(attributes);
        
        User user = userRepository.findByProviderAndProviderSub(
                User.OAuthProvider.valueOf(providerId.toUpperCase()), 
                providerSub
            )
            .map(existingUser -> updateExistingUser(existingUser, name, pictureUrl))
            .orElseGet(() -> registerNewUser(providerId, providerSub, email, name, pictureUrl));
        
        return new CustomOAuth2User(oAuth2User, user);
    }

    private String extractProviderSub(Map<String, Object> attributes, String providerId) {
        if ("google".equals(providerId)) {
            return (String) attributes.get("sub");
        }
        throw new OAuth2AuthenticationException("Unknown provider: " + providerId);
    }

    private String extractEmail(Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }

    private String extractName(Map<String, Object> attributes) {
        return (String) attributes.get("name");
    }

    private String extractPictureUrl(Map<String, Object> attributes) {
        return (String) attributes.get("picture");
    }

    private User updateExistingUser(User existingUser, String name, String pictureUrl) {
        existingUser.setName(name);
        existingUser.setPictureUrl(pictureUrl);
        return userRepository.save(existingUser);
    }

    private User registerNewUser(String providerId, String providerSub, String email, String name, String pictureUrl) {
        log.info("Creating new user: {}", email);
        
        User newUser = User.builder()
            .email(email)
            .name(name)
            .pictureUrl(pictureUrl)
            .provider(User.OAuthProvider.valueOf(providerId.toUpperCase()))
            .providerSub(providerSub)
            .role(User.UserRole.GUEST) // Default role for new users
            .isActive(true)
            .build();
        
        return userRepository.save(newUser);
    }
}
