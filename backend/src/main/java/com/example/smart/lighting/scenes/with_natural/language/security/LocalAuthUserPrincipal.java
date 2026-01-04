package com.example.smart.lighting.scenes.with_natural.language.security;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal for locally authenticated users.
 *
 * <p>Implements {@link UserDetails} to integrate local email/password
 * authentication with Spring Security's authentication framework.
 * Wraps the application's {@link User} entity.</p>
 *

 * @see UserDetails
 * @see User
 */
@Getter
@RequiredArgsConstructor
public class LocalAuthUserPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }

    public String getEmail() {
        return user.getEmail();
    }
}
