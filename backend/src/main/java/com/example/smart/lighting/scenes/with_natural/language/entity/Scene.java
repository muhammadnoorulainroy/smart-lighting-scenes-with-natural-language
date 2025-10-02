package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scenes", schema = "smartlighting",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<SceneAction> actionsJson = new ArrayList<>();

    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneAction {
        private UUID deviceId;
        private Boolean on;
        private Integer brightness;
        private Integer colorTemp;
        private String rgbColor;
    }
}
