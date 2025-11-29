package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SceneRepository extends JpaRepository<Scene, UUID> {

    List<Scene> findByOwnerId(UUID ownerId);

    List<Scene> findByIsGlobalTrue();

    @Query("SELECT s FROM Scene s WHERE s.owner.id = :ownerId OR s.isGlobal = true")
    List<Scene> findAvailableForUser(@Param("ownerId") UUID ownerId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
