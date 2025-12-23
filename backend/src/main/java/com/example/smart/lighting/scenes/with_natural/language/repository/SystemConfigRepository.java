package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for system configuration settings.
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    
    /**
     * Find all config entries ordered by key.
     */
    List<SystemConfig> findAllByOrderByKeyAsc();
}

