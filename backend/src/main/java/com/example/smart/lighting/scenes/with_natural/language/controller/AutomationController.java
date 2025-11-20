package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.entity.Automation;
import com.example.smart.lighting.scenes.with_natural.language.service.AutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/automations")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    /**
     * Get all automations
     */
    @GetMapping
    public ResponseEntity<List<Automation>> getAllAutomations() {
        return ResponseEntity.ok(automationService.getAllAutomations());
    }

    /**
     * Get automation by ID
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
     * Execute automation
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
     * Reload automations from file
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

