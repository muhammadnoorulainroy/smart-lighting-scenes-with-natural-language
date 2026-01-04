package com.example.smart.lighting.scenes.with_natural.language.security;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2 user principal that wraps both the OIDC user and our domain User entity.
 *
 * <p>Bridges Spring Security's OAuth2 authentication with our application's
 * user management system. Provides access to both OAuth claims and our
 * internal user data including role information.</p>
 *

 * @see OidcUser
 * @see User
 */
@Getter
public class CustomOAuth2User implements OidcUser, Serializable {

    private static final long serialVersionUID = 1L;

    private final OidcUser oidcUser;
    private final User user;

    /**
     * Creates a new CustomOAuth2User wrapping the OIDC user and domain user.
     *
     * @param oidcUser the Spring Security OIDC user
     * @param user the application's User entity
     */
    public CustomOAuth2User(OidcUser oidcUser, User user) {
        this.oidcUser = oidcUser;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    public User.UserRole getRole() {
        return user.getRole();
    }
}
