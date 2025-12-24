package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Scene} entity persistence operations.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Repository
public interface SceneRepository extends JpaRepository<Scene, UUID> {

    /**
     * Finds all active scenes.
     */
    List<Scene> findByIsActiveTrue();

    /**
     * Finds all preset scenes.
     */
    List<Scene> findByIsPresetTrueAndIsActiveTrue();

    /**
     * Finds all custom (non-preset) scenes.
     */
    List<Scene> findByIsPresetFalseAndIsActiveTrue();

    /**
     * Finds a scene by name (case-insensitive).
     */
    Optional<Scene> findByNameIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Checks if a scene with the given name exists.
     */
    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);
}
