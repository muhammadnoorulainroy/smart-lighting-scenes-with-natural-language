package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Room} entity persistence operations.
 *
 * <p>Provides CRUD operations and custom queries for room management,
 * including eager fetching of associated devices.</p>
 *
 * @author Smart Lighting Team
 * @version 1.0
 * @see Room
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    /**
     * Finds a room by its unique name.
     * @param name the room name
     * @return optional containing the room
     */
    Optional<Room> findByName(String name);

    /**
     * Finds a room by ID with devices eagerly loaded.
     * @param id the room UUID
     * @return optional containing room with devices
     */
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.devices WHERE r.id = :id")
    Optional<Room> findByIdWithDevices(UUID id);

    /**
     * Finds all rooms with devices eagerly loaded.
     * @return list of all rooms with their devices
     */
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.devices")
    List<Room> findAllWithDevices();

    /**
     * Checks if a room with the given name exists.
     * @param name the room name
     * @return true if exists
     */
    boolean existsByName(String name);
}
