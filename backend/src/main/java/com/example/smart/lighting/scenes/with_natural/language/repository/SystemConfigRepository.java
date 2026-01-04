package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link SystemConfig} entity persistence operations.
 *
 * <p>Provides access to runtime configuration settings that are
 * synced to ESP32 devices via MQTT.</p>
 *

 * @see SystemConfig
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    /**
     * Find all config entries ordered by key.
     */
    List<SystemConfig> findAllByOrderByKeyAsc();
}
