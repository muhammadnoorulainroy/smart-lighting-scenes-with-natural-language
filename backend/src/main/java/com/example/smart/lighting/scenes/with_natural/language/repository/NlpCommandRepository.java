package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.NlpCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link NlpCommand} entity persistence operations.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Repository
public interface NlpCommandRepository extends JpaRepository<NlpCommand, UUID> {

    /**
     * Finds commands by user, ordered by creation date descending.
     */
    Page<NlpCommand> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds all commands ordered by creation date descending.
     */
    Page<NlpCommand> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

