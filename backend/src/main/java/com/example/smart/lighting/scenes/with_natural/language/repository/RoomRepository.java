package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    
    Optional<Room> findByName(String name);
    
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.devices WHERE r.id = :id")
    Optional<Room> findByIdWithDevices(UUID id);
    
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.devices")
    List<Room> findAllWithDevices();
    
    boolean existsByName(String name);
}
