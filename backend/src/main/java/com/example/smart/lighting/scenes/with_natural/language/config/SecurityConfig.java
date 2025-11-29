package com.example.smart.lighting.scenes.with_natural.language.config;

import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2UserService;
import com.example.smart.lighting.scenes.with_natural.language.security.OAuth2AuthenticationSuccessHandler;
import com.example.smart.lighting.scenes.with_natural.language.security.OAuth2AuthenticationFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the Smart Lighting application.
 *
 * <p>Configures OAuth2 login with Google, role-based access control,
 * CORS settings, and session management.</p>
 *
 * <h3>Role Hierarchy:</h3>
 * <ul>
 *   <li>OWNER - Full access to all endpoints</li>
 *   <li>RESIDENT - Access to devices, rooms, and scenes</li>
 *   <li>GUEST - Basic read-only access</li>
 * </ul>
 *
 * @author Smart Lighting Team
 * @version 1.0
 * @see CustomOAuth2UserService
 * @see OAuth2AuthenticationSuccessHandler
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/",
        "/error",
        "/actuator/health",
        "/api/auth/**",
        "/oauth2/**",
        "/login/**",
        "/ws/**",
        "/test-auth.html",
        "/*.html",
        "/css/**",
        "/js/**",
        "/images/**"
    };

    private static final String[] OWNER_ONLY_ENDPOINTS = {
        "/api/users/**",
        "/api/settings/**"
    };

    private static final String[] RESIDENT_ENDPOINTS = {
        "/api/rooms",
        "/api/devices",
        "/api/scenes",
        "/api/rules"
    };

    private static final String[] AUTHENTICATED_ENDPOINTS = {
        "/api/**"
    };

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .sessionFixation().newSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(OWNER_ONLY_ENDPOINTS).hasRole("OWNER")
                .requestMatchers(RESIDENT_ENDPOINTS).hasAnyRole("OWNER", "RESIDENT")
                .requestMatchers(AUTHENTICATED_ENDPOINTS).hasAnyRole("OWNER", "RESIDENT", "GUEST")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/google")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOAuth2UserService)
                )
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
