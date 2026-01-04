package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Schedule} entity persistence operations.
 *
 * <p>Provides queries for finding enabled schedules by trigger type.</p>
 *

 * @see Schedule
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    /**
     * Finds all enabled schedules.
     */
    List<Schedule> findByEnabledTrue();

    /**
     * Finds all schedules of a specific trigger type.
     */
    List<Schedule> findByTriggerTypeAndEnabledTrue(String triggerType);

    /**
     * Finds a schedule by name.
     */
    Optional<Schedule> findByNameIgnoreCase(String name);

    /**
     * Finds all time-based schedules that are enabled.
     */
    @Query("SELECT s FROM Schedule s WHERE s.triggerType = 'time' AND s.enabled = true")
    List<Schedule> findEnabledTimeSchedules();

    /**
     * Finds all sun-based schedules (sunset/sunrise) that are enabled.
     */
    @Query("SELECT s FROM Schedule s WHERE s.triggerType = 'sun' AND s.enabled = true")
    List<Schedule> findEnabledSunSchedules();
}
