package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.LoginRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.SignupRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;
import com.example.smart.lighting.scenes.with_natural.language.service.LocalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Register a new user with email and password.
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        try {
            User user = localAuthService.signup(request);
            
            // Create session for the new user
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(LOCAL_USER_SESSION_KEY, user);
            
            log.info("User signed up and session created: {}", user.getEmail());
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
            
            // Create session for the user
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(LOCAL_USER_SESSION_KEY, user);
            
            log.info("User logged in and session created: {}", user.getEmail());
            return ResponseEntity.ok(localAuthService.toUserDto(user));
        } catch (BadCredentialsException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current authenticated user profile.
     * Works for both OAuth2 and local auth users.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal CustomOAuth2User oauthPrincipal,
            HttpServletRequest request) {
        
        // Check OAuth2 user first
        if (oauthPrincipal != null) {
            User user = oauthPrincipal.getUser();
            log.debug("OAuth user profile accessed: {}", user.getEmail());
            return ResponseEntity.ok(buildUserDto(user));
        }
        
        // Check local auth user in session
        HttpSession session = request.getSession(false);
        if (session != null) {
            User localUser = (User) session.getAttribute(LOCAL_USER_SESSION_KEY);
            if (localUser != null) {
                log.debug("Local user profile accessed: {}", localUser.getEmail());
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
    public ResponseEntity<Boolean> checkAuthentication(
            @AuthenticationPrincipal CustomOAuth2User oauthPrincipal,
            HttpServletRequest request) {
        
        // Check OAuth2 user
        if (oauthPrincipal != null) {
            log.debug("Auth check: OAuth user authenticated: {}", oauthPrincipal.getEmail());
            return ResponseEntity.ok(true);
        }
        
        // Check local auth user
        HttpSession session = request.getSession(false);
        if (session != null) {
            User localUser = (User) session.getAttribute(LOCAL_USER_SESSION_KEY);
            if (localUser != null) {
                log.debug("Auth check: Local user authenticated: {}", localUser.getEmail());
                return ResponseEntity.ok(true);
            }
        }
        
        log.debug("Auth check: not authenticated");
        return ResponseEntity.ok(false);
    }

    /**
     * Debug endpoint for authentication troubleshooting.
     */
    @GetMapping("/auth/debug")
    public ResponseEntity<?> debugAuth(
            @AuthenticationPrincipal CustomOAuth2User oauthPrincipal,
            HttpServletRequest request) {
        
        HttpSession session = request.getSession(false);
        User localUser = session != null ? (User) session.getAttribute(LOCAL_USER_SESSION_KEY) : null;
        
        Map<String, Object> debug = Map.of(
            "oauthAuthenticated", oauthPrincipal != null,
            "oauthUser", oauthPrincipal != null ? oauthPrincipal.getEmail() : null,
            "localAuthenticated", localUser != null,
            "localUser", localUser != null ? localUser.getEmail() : null,
            "sessionId", session != null ? session.getId() : null
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
