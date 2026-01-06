package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.ScheduleDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing lighting schedules/automations.
 *

 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class SchedulesController {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Get all schedules.
     * All authenticated users can view schedules.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDto>> getAllSchedules() {
        List<Schedule> schedules = scheduleRepository.findAll();
        List<ScheduleDto> dtos = schedules.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a schedule by ID.
     * All authenticated users can view schedules.
     */
    @GetMapping("/{scheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleDto> getScheduleById(@PathVariable UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        return ResponseEntity.ok(toDto(schedule));
    }

    /**
     * Create a new schedule.
     * OWNER and RESIDENT can create schedules.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<ScheduleDto> createSchedule(@RequestBody ScheduleDto scheduleDto, Authentication auth) {
        log.info("Creating schedule: {}", scheduleDto.getName());

        User user = getCurrentUser(auth);

        Schedule schedule = Schedule.builder()
            .name(scheduleDto.getName())
            .description(scheduleDto.getDescription())
            .enabled(scheduleDto.getEnabled() == null || scheduleDto.getEnabled())
            .triggerType(scheduleDto.getTriggerType())
            .triggerConfig(scheduleDto.getTriggerConfig() != null ? scheduleDto.getTriggerConfig() : new HashMap<>())
            .conditions(scheduleDto.getConditions() != null ? scheduleDto.getConditions() : new ArrayList<>())
            .actions(scheduleDto.getActions() != null ? scheduleDto.getActions() : new ArrayList<>())
            .createdBy(user)
            .build();

        schedule = scheduleRepository.save(schedule);
        log.info("Schedule created: {}", schedule.getId());

        // Broadcast WebSocket event for real-time sync
        webSocketEventService.broadcastScheduleCreated(schedule.getId(), schedule.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(schedule));
    }

    /**
     * Update an existing schedule.
     * OWNER and RESIDENT can update schedules.
     */
    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<ScheduleDto> updateSchedule(
            @PathVariable UUID scheduleId, @RequestBody ScheduleDto scheduleDto) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (scheduleDto.getName() != null) {
            schedule.setName(scheduleDto.getName());
        }
        if (scheduleDto.getDescription() != null) {
            schedule.setDescription(scheduleDto.getDescription());
        }
        if (scheduleDto.getEnabled() != null) {
            schedule.setEnabled(scheduleDto.getEnabled());
        }
        if (scheduleDto.getTriggerType() != null) {
            schedule.setTriggerType(scheduleDto.getTriggerType());
        }
        if (scheduleDto.getTriggerConfig() != null) {
            schedule.setTriggerConfig(scheduleDto.getTriggerConfig());
        }
        if (scheduleDto.getConditions() != null) {
            schedule.setConditions(scheduleDto.getConditions());
        }
        if (scheduleDto.getActions() != null) {
            schedule.setActions(scheduleDto.getActions());
        }

        schedule = scheduleRepository.save(schedule);

        // Broadcast WebSocket event for real-time sync
        webSocketEventService.broadcastScheduleUpdated(schedule.getId(), schedule.getName());

        return ResponseEntity.ok(toDto(schedule));
    }

    /**
     * Toggle schedule enabled/disabled.
     * OWNER and RESIDENT can toggle schedules.
     */
    @PostMapping("/{scheduleId}/toggle")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<ScheduleDto> toggleSchedule(@PathVariable UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        schedule.setEnabled(!schedule.getEnabled());
        schedule = scheduleRepository.save(schedule);

        log.info("Schedule {} {}", schedule.getId(), schedule.getEnabled() ? "enabled" : "disabled");

        // Broadcast WebSocket event for real-time sync
        webSocketEventService.broadcastScheduleToggled(
            schedule.getId(), schedule.getName(), schedule.getEnabled());

        return ResponseEntity.ok(toDto(schedule));
    }

    /**
     * Delete a schedule.
     * OWNER and RESIDENT can delete schedules.
     */
    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        scheduleRepository.deleteById(scheduleId);
        log.info("Schedule deleted: {}", scheduleId);

        // Broadcast WebSocket event for real-time sync
        webSocketEventService.broadcastScheduleDeleted(scheduleId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user from authentication.
     */
    private User getCurrentUser(Authentication auth) {
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    /**
     * Convert entity to DTO.
     */
    private ScheduleDto toDto(Schedule schedule) {
        ScheduleDto.ScheduleDtoBuilder builder = ScheduleDto.builder()
            .id(schedule.getId())
            .name(schedule.getName())
            .description(schedule.getDescription())
            .enabled(schedule.getEnabled())
            .triggerType(schedule.getTriggerType())
            .triggerConfig(schedule.getTriggerConfig())
            .conditions(schedule.getConditions())
            .actions(schedule.getActions())
            .lastTriggeredAt(schedule.getLastTriggeredAt())
            .triggerCount(schedule.getTriggerCount())
            .createdAt(schedule.getCreatedAt())
            .updatedAt(schedule.getUpdatedAt());

        if (schedule.getCreatedBy() != null) {
            builder.createdBy(schedule.getCreatedBy().getId())
                .createdByName(schedule.getCreatedBy().getName());
        }

        return builder.build();
    }
}
