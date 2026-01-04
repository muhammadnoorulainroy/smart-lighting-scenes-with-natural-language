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
 * <p>Stores NLP command history for audit and ML training purposes.</p>
 *

 * @see NlpCommand
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
