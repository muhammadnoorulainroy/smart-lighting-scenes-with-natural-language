package com.example.smart.lighting.scenes.with_natural.language.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        String targetUrl = getFailureUrl(exception);

        log.error("OAuth2 authentication failed", exception);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getFailureUrl(AuthenticationException exception) {
        // Get the frontend URL
        String frontendUrl = allowedOrigins.length > 0 ? allowedOrigins[0] : "http://localhost:5173";
        
        String errorMessage = exception.getMessage();
        
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("error", "authentication_failed")
                .queryParam("error_description", errorMessage)
                .build().toUriString();
    }
}
