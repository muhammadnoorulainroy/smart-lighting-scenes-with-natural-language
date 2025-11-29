package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for User entities.
 *
 * <p>Used for API responses containing user profile information.
 * Excludes sensitive data like OAuth tokens.</p>
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String name;
    private String pictureUrl;
    private String role;
    private LocalDateTime createdAt;
}
