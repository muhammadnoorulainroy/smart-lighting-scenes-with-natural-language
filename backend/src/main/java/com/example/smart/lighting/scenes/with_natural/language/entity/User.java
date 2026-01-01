package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user in the smart lighting system.
 *
 * <p>Users authenticate via OAuth (Google) and are assigned roles that
 * determine their access level:</p>
 * <ul>
 *   <li>{@link UserRole#OWNER} - Full system access, can manage users and settings</li>
 *   <li>{@link UserRole#RESIDENT} - Can control devices and create scenes</li>
 *   <li>{@link UserRole#GUEST} - Limited read-only access</li>
 * </ul>
 *
 * @author Smart Lighting Team
 * @version 1.0
 * @see UserRole
 * @see OAuthProvider
 */
@Entity
@Table(name = "users", schema = "smartlighting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_sub")
    private String providerSub;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * User roles defining access levels in the system.
     */
    public enum UserRole {
        /** Full administrative access. */
        OWNER,
        /** Can control devices and manage scenes. */
        RESIDENT,
        /** Limited read-only access. */
        GUEST
    }

    /**
     * Supported OAuth authentication providers.
     */
    public enum OAuthProvider {
        /** Google OAuth 2.0. */
        GOOGLE,
        /** Local authentication */
        LOCAL
    }
}
