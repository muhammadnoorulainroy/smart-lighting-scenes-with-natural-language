package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    Page<Event> findAllByOrderByTimestampDesc(Pageable pageable);
    
    Page<Event> findByTypeOrderByTimestampDesc(Event.EventType type, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE e.timestamp >= :since ORDER BY e.timestamp DESC")
    Page<Event> findRecentEvents(@Param("since") LocalDateTime since, Pageable pageable);
    
    List<Event> findTop10ByOrderByTimestampDesc();
}

