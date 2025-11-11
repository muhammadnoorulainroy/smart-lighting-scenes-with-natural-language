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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.notFound().build();
        }
        
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

    @GetMapping("/auth/check")
    public ResponseEntity<Boolean> checkAuthentication(@AuthenticationPrincipal CustomOAuth2User principal) {
        return ResponseEntity.ok(principal != null);
    }
}
