package com.example.smart.lighting.scenes.with_natural.language.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for authentication-related endpoints.
 * 
 * <p>Handles user authentication state, profile retrieval, and session
 * management. Works in conjunction with Spring Security OAuth2.</p>
 * 
 * @author Smart Lighting Team
 * @version 1.0
 * @see CustomOAuth2User
 * @see UserDto
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    /**
     * Retrieves the current authenticated user's profile.
     * 
     * @param principal the authenticated OAuth2 user, or null if not authenticated
     * @return the user profile DTO, or 404 if not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null) {
            log.debug("GET /api/me called without authentication");
            return ResponseEntity.notFound().build();
        }
        
        log.info("User profile accessed: email={}, role={}", 
            principal.getUser().getEmail(), principal.getUser().getRole());
        
        UserDto userDto = UserDto.builder()
            .id(principal.getUser().getId())
            .email(principal.getUser().getEmail())
            .name(principal.getUser().getName())
            .pictureUrl(principal.getUser().getPictureUrl())
            .role(principal.getUser().getRole().name())
            .createdAt(principal.getUser().getCreatedAt())
            .build();
        
        return ResponseEntity.ok(userDto);
    }

    /**
     * Checks if the current request is authenticated.
     * 
     * @param principal the authenticated OAuth2 user, or null
     * @return true if authenticated, false otherwise
     */
    @GetMapping("/auth/check")
    public ResponseEntity<Boolean> checkAuthentication(@AuthenticationPrincipal CustomOAuth2User principal) {
        boolean isAuth = principal != null;
        log.debug("Auth check: {}, Principal: {}", isAuth, principal != null ? principal.getEmail() : "null");
        return ResponseEntity.ok(isAuth);
    }
    
    @GetMapping("/auth/debug")
    public ResponseEntity<?> debugAuth(@AuthenticationPrincipal CustomOAuth2User principal, 
                                       jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> debug = new java.util.HashMap<>();
        debug.put("authenticated", principal != null);
        debug.put("user", principal != null ? principal.getEmail() : null);
        debug.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : null);
        debug.put("cookies", request.getCookies() != null ? 
            java.util.Arrays.stream(request.getCookies())
                .collect(java.util.stream.Collectors.toMap(
                    jakarta.servlet.http.Cookie::getName,
                    jakarta.servlet.http.Cookie::getValue
                )) : null);
        return ResponseEntity.ok(debug);
    }
}
