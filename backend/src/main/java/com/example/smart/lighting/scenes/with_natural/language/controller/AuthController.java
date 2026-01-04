package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.LoginRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.SignupRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;
import com.example.smart.lighting.scenes.with_natural.language.security.LocalAuthUserPrincipal;
import com.example.smart.lighting.scenes.with_natural.language.service.LocalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * Supports both OAuth2 (Google) and local email/password authentication.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    private static final String LOCAL_USER_SESSION_KEY = "LOCAL_AUTH_USER";

    private final LocalAuthService localAuthService;

    @org.springframework.beans.factory.annotation.Value(
        "${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    /**
     * Register a new user with email and password.
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        try {
            User user = localAuthService.signup(request);
            authenticateUser(user, httpRequest);
            log.info("User signed up and authenticated: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(localAuthService.toUserDto(user));
        } catch (IllegalArgumentException e) {
            log.warn("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Login with email and password.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = localAuthService.login(request);
            authenticateUser(user, httpRequest);
            log.info("User logged in and authenticated: {}", user.getEmail());
            return ResponseEntity.ok(localAuthService.toUserDto(user));
        } catch (BadCredentialsException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Authenticate user by setting Spring Security context.
     */
    private void authenticateUser(User user, HttpServletRequest request) {
        LocalAuthUserPrincipal principal = new LocalAuthUserPrincipal(user);
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Store in session for persistence across requests
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        session.setAttribute(LOCAL_USER_SESSION_KEY, user);
    }

    /**
     * Get current authenticated user profile.
     * Works for both OAuth2 and local auth users.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            // Check OAuth2 user
            if (principal instanceof CustomOAuth2User oauthUser) {
                log.debug("OAuth user profile accessed: {}", oauthUser.getEmail());
                return ResponseEntity.ok(buildUserDto(oauthUser.getUser()));
            }

            // Check local auth user
            if (principal instanceof LocalAuthUserPrincipal localUser) {
                log.debug("Local user profile accessed: {}", localUser.getEmail());
                return ResponseEntity.ok(buildUserDto(localUser.getUser()));
            }
        }

        // Fallback: check session directly
        HttpSession session = request.getSession(false);
        if (session != null) {
            User localUser = (User) session.getAttribute(LOCAL_USER_SESSION_KEY);
            if (localUser != null) {
                log.debug("Local user from session: {}", localUser.getEmail());
                return ResponseEntity.ok(buildUserDto(localUser));
            }
        }

        log.debug("GET /api/me called without authentication");
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if the current request is authenticated.
     */
    @GetMapping("/auth/check")
    public ResponseEntity<Boolean> checkAuthentication(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomOAuth2User oauthUser) {
                log.debug("Auth check: OAuth user authenticated: {}", oauthUser.getEmail());
                return ResponseEntity.ok(true);
            }

            if (principal instanceof LocalAuthUserPrincipal localUser) {
                log.debug("Auth check: Local user authenticated: {}", localUser.getEmail());
                return ResponseEntity.ok(true);
            }
        }

        // Fallback: check session directly
        HttpSession session = request.getSession(false);
        if (session != null) {
            User localUser = (User) session.getAttribute(LOCAL_USER_SESSION_KEY);
            if (localUser != null) {
                log.debug("Auth check: Local user from session: {}", localUser.getEmail());
                return ResponseEntity.ok(true);
            }
        }

        log.debug("Auth check: not authenticated");
        return ResponseEntity.ok(false);
    }

    /**
     * Logout endpoint - clears session for both OAuth and local auth
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userEmail = null;

            // Get user email before invalidating
            User localUser = (User) session.getAttribute(LOCAL_USER_SESSION_KEY);
            if (localUser != null) {
                userEmail = localUser.getEmail();
            }

            session.invalidate();
            log.info("User logged out: {}", userEmail != null ? userEmail : "unknown");
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Get authentication configuration for mobile app.
     * Returns Google Client ID if configured.
     */
    @GetMapping("/auth/config")
    public ResponseEntity<Map<String, String>> getAuthConfig() {
        boolean hasClientId = googleClientId != null && !googleClientId.isEmpty();
        log.debug("Fetching auth config, Google Client ID present: {}", hasClientId);

        Map<String, String> config = Map.of(
            "googleClientId", googleClientId != null ? googleClientId : ""
        );

        return ResponseEntity.ok(config);
    }

    /**
     * Debug endpoint for authentication troubleshooting.
     */
    @GetMapping("/auth/debug")
    public ResponseEntity<?> debugAuth(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);

        String principalType = authentication != null
            ? authentication.getPrincipal().getClass().getSimpleName() : "none";
        String principalEmail = null;

        if (authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User oauthUser) {
            principalEmail = oauthUser.getEmail();
        } else if (authentication != null
                && authentication.getPrincipal() instanceof LocalAuthUserPrincipal localUser) {
            principalEmail = localUser.getEmail();
        }

        User sessionUser = session != null ? (User) session.getAttribute(LOCAL_USER_SESSION_KEY) : null;

        Map<String, Object> debug = Map.of(
            "authenticated", authentication != null && authentication.isAuthenticated(),
            "principalType", principalType,
            "principalEmail", principalEmail != null ? principalEmail : "none",
            "sessionUser", sessionUser != null ? sessionUser.getEmail() : "none",
            "sessionId", session != null ? session.getId() : "none"
        );

        return ResponseEntity.ok(debug);
    }

    private UserDto buildUserDto(User user) {
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
