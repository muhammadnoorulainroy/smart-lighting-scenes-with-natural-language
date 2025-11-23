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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('OWNER', 'RESIDENT', 'GUEST')")
public class EventsController {

    private final EventRepository eventRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer limit) {
        
        if (limit != null && limit > 0) {
            size = limit;
        }
        
        Pageable pageable = PageRequest.of(page, size);
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

