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

/**
 * Service for local email/password authentication.
 *
 * <p>Provides user registration and login functionality using BCrypt
 * password hashing. Works alongside OAuth2 authentication for users
 * who prefer not to use Google login.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>User registration with email validation</li>
 *   <li>Secure password hashing with BCrypt</li>
 *   <li>Login with credential verification</li>
 *   <li>Account status checking (active/disabled)</li>
 * </ul>
 *

 * @see User
 * @see PasswordEncoder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with email/password credentials.
     *
     * @param request the signup request containing email, password, and name
     * @return the newly created user
     * @throws IllegalArgumentException if email is already registered
     */
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

    /**
     * Authenticates a user with email and password.
     *
     * @param request the login request containing email and password
     * @return the authenticated user
     * @throws BadCredentialsException if credentials are invalid or account is disabled
     */
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

    /**
     * Converts a User entity to a UserDto.
     *
     * @param user the user entity
     * @return the user DTO
     */
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
