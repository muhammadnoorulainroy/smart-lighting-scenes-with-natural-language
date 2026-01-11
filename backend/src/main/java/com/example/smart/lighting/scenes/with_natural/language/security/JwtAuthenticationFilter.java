package com.example.smart.lighting.scenes.with_natural.language.security;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT Authentication Filter for validating Bearer tokens.
 *
 * <p>This filter intercepts requests and checks for a valid JWT token
 * in the Authorization header. If valid, it sets the authentication
 * in the SecurityContext.</p>
 *
 * <p>This enables cross-domain authentication where session cookies
 * cannot be shared between different origins.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // Skip if no Authorization header or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if already authenticated (e.g., via session)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // Validate and extract claims from token
            if (!jwtService.isTokenExpired(jwt)) {
                String email = jwtService.getEmailFromToken(jwt);
                String userIdStr = jwtService.getUserIdFromToken(jwt);

                if (email != null) {
                    // Look up user by user ID or email
                    Optional<User> userOpt = Optional.empty();

                    if (userIdStr != null) {
                        try {
                            UUID userId = UUID.fromString(userIdStr);
                            userOpt = userRepository.findById(userId);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid user ID in JWT: {}", userIdStr);
                        }
                    }

                    if (userOpt.isEmpty()) {
                        userOpt = userRepository.findByEmail(email);
                    }

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Create authentication token with user's role
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        );

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("JWT authentication successful for user: {}", email);
                    } else {
                        log.warn("User not found for JWT token: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
