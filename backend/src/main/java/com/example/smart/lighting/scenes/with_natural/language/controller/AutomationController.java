package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.entity.Automation;
import com.example.smart.lighting.scenes.with_natural.language.service.AutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing Home Assistant-style automations.
 *
 * <p>Provides endpoints to view, execute, and reload YAML-based automations
 * that define complex lighting behaviors with triggers, conditions, and actions.</p>
 *

 * @see AutomationService
 * @see Automation
 */
@Slf4j
@RestController
@RequestMapping("/api/automations")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    /**
     * Get all automations.
     */
    @GetMapping
    public ResponseEntity<List<Automation>> getAllAutomations() {
        return ResponseEntity.ok(automationService.getAllAutomations());
    }

    /**
     * Get automation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAutomation(@PathVariable String id) {
        Automation automation = automationService.getAutomation(id);
        if (automation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(automation);
    }

    /**
     * Execute automation.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeAutomation(@PathVariable String id) {
        Automation automation = automationService.getAutomation(id);
        if (automation == null) {
            return ResponseEntity.notFound().build();
        }

        automationService.executeAutomation(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "automation", id,
            "alias", automation.getAlias()
        ));
    }

    /**
     * Reload automations from file.
     */
    @PostMapping("/reload")
    public ResponseEntity<?> reloadAutomations() {
        automationService.reloadAutomations();
        List<Automation> automations = automationService.getAllAutomations();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", automations.size(),
            "automations", automations.stream().map(Automation::getAlias).toList()
        ));
    }
}
