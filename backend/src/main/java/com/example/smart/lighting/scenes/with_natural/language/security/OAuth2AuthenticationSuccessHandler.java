package com.example.smart.lighting.scenes.with_natural.language.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) {

        // Get the frontend URL (default to first allowed origin)
        String frontendUrl = allowedOrigins.length > 0 ? allowedOrigins[0] : "http://localhost:5173";

        // Get user information
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String userRole = oAuth2User.getRole().name();

        log.info("User {} logged in successfully with role {}", oAuth2User.getEmail(), userRole);

        // Redirect to frontend auth callback page which will handle the post-auth flow
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("success", "true")
                .build().toUriString();
    }
}
