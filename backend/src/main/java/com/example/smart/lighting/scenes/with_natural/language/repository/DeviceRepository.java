package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    List<Device> findByRoomId(UUID roomId);
    
    List<Device> findByType(Device.DeviceType type);
    
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.deviceState WHERE d.id = :id")
    Optional<Device> findByIdWithState(@Param("id") UUID id);
    
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.deviceState WHERE d.room.id = :roomId")
    List<Device> findByRoomIdWithState(@Param("roomId") UUID roomId);
    
    Optional<Device> findByRoomIdAndName(UUID roomId, String name);
    
    boolean existsByRoomIdAndName(UUID roomId, String name);
}
