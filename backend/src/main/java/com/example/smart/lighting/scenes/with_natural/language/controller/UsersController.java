package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.CreateUserRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.UpdateUserRoleRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.entity.User.OAuthProvider;
import com.example.smart.lighting.scenes.with_natural.language.entity.User.UserRole;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasRole('OWNER')")
public class UsersController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Admin creating user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Create user failed: email already exists: {}", request.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider(OAuthProvider.LOCAL)
                .role(UserRole.valueOf(request.getRole()))
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created by admin: {}", savedUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedUser));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getId().equals(currentUser.getUser().getId())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            user.setRole(User.UserRole.valueOf(request.getRole()));
            user = userRepository.save(user);
            return ResponseEntity.ok(toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<UserDto> disableUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getId().equals(currentUser.getUser().getId())) {
            return ResponseEntity.badRequest().build();
        }

        user.setIsActive(false);
        user = userRepository.save(user);
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<UserDto> enableUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        user = userRepository.save(user);
        return ResponseEntity.ok(toDto(user));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .pictureUrl(user.getPictureUrl())
            .role(user.getRole().name())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
