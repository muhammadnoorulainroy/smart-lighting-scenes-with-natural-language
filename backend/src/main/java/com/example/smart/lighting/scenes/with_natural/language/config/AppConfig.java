package com.example.smart.lighting.scenes.with_natural.language.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * General application configuration for common beans.
 *
 * <p>Provides shared utility beans used across the application:</p>
 * <ul>
 *   <li>{@link ModelMapper} - Object-to-object mapping for DTO conversions</li>
 *   <li>{@link RestTemplate} - HTTP client for external API calls (e.g., OpenAI)</li>
 * </ul>
 *

 */
@Configuration
public class AppConfig {

    /**
     * Creates a ModelMapper bean for entity-to-DTO conversions.
     *
     * @return configured ModelMapper instance
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /**
     * Creates a RestTemplate bean for making HTTP requests to external services.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
