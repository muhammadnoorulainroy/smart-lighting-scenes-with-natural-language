package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.dto.LoginRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.SignupRequest;
import com.example.smart.lighting.scenes.with_natural.language.dto.UserDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.entity.User.OAuthProvider;
import com.example.smart.lighting.scenes.with_natural.language.entity.User.UserRole;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {
        log.info("Signup attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup failed: email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider(OAuthProvider.LOCAL)
                .role(UserRole.GUEST)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found: {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (user.getProvider() != OAuthProvider.LOCAL) {
            log.warn("Login failed: user registered with OAuth: {}", request.getEmail());
            throw new BadCredentialsException("Please use Google to sign in");
        }

        if (user.getPasswordHash() == null) {
            log.warn("Login failed: no password set for user: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            log.warn("Login failed: user account inactive: {}", request.getEmail());
            throw new BadCredentialsException("Account is disabled");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return user;
    }

    public UserDto toUserDto(User user) {
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

