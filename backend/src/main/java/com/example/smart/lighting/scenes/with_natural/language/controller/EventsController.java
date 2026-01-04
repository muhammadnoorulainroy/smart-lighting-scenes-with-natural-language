package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.EventDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Event;
import com.example.smart.lighting.scenes.with_natural.language.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for accessing system event history.
 *
 * <p>Provides paginated access to the event log which tracks all
 * significant system activities including:</p>
 * <ul>
 *   <li>Device state changes</li>
 *   <li>Scene applications</li>
 *   <li>Schedule executions</li>
 *   <li>User actions</li>
 *   <li>System events</li>
 * </ul>
 *

 * @see Event
 * @see EventDto
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('OWNER', 'RESIDENT', 'GUEST')")
public class EventsController {

    private final EventRepository eventRepository;

    /**
     * Retrieves paginated event history.
     *
     * @param page page number (0-based, default 0)
     * @param size page size (default 10)
     * @param limit optional limit override for size
     * @return paginated list of events with metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer limit) {

        int effectiveSize = size;
        if (limit != null && limit > 0) {
            effectiveSize = limit;
        }

        Pageable pageable = PageRequest.of(page, effectiveSize);
        Page<Event> eventPage = eventRepository.findAllByOrderByTimestampDesc(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", eventPage.getContent().stream()
            .map(this::toDto)
            .collect(Collectors.toList()));
        response.put("totalElements", eventPage.getTotalElements());
        response.put("totalPages", eventPage.getTotalPages());
        response.put("size", eventPage.getSize());
        response.put("number", eventPage.getNumber());

        return ResponseEntity.ok(response);
    }

    private EventDto toDto(Event event) {
        EventDto.EventDtoBuilder builder = EventDto.builder()
            .id(event.getId())
            .timestamp(event.getTimestamp())
            .type(event.getType().name())
            .detailsJson(event.getDetailsJson() != null ? event.getDetailsJson() : new HashMap<>())
            .causeChain(event.getCauseChain());

        if (event.getActorUser() != null) {
            builder.actorUserId(event.getActorUser().getId())
                .actorUserName(event.getActorUser().getName());
        }

        if (event.getDevice() != null) {
            builder.deviceId(event.getDevice().getId())
                .deviceName(event.getDevice().getName());
        }

        if (event.getRuleId() != null) {
            builder.ruleId(event.getRuleId());
        }

        if (event.getScene() != null) {
            builder.sceneId(event.getScene().getId());
        }

        return builder.build();
    }
}
