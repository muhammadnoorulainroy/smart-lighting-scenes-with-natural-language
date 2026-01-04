package com.example.smart.lighting.scenes.with_natural.language.service.conflict;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictAnalysisResult;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictResolution;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ScheduleConflict;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhances conflict analysis using AI for smarter resolutions.
 *

 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConflictAiEnhancer {

    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    /**
     * Check if AI enhancement is available.
     */
    public boolean isConfigured() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }

    /**
     * Enhance conflict analysis with AI-generated suggestions.
     *
     * @param newSchedule the new schedule
     * @param basicConflicts detected conflicts
     * @return enhanced analysis result
     */
    public ConflictAnalysisResult enhanceWithAI(
            Schedule newSchedule, List<ScheduleConflict> basicConflicts) {
        if (!isConfigured()) {
            String summary = String.format(
                "Found %d potential conflict(s). Review the suggested resolutions.",
                basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }

        try {
            String prompt = buildAIPrompt(newSchedule, basicConflicts);
            String aiResponse = callOpenAI(prompt);
            return parseAIResponse(aiResponse, basicConflicts);
        } catch (Exception e) {
            log.warn("AI enhancement failed, using basic analysis: {}", e.getMessage());
            String summary = String.format(
                "Found %d potential conflict(s). Review the suggested resolutions.",
                basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }
    }

    private String buildAIPrompt(Schedule newSchedule, List<ScheduleConflict> conflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are a smart home schedule conflict analyzer. \
            Analyze these lighting schedule conflicts and provide enhanced resolutions.

            New Schedule:
            """);
        sb.append(scheduleToString(newSchedule));
        sb.append("\n\nDetected Conflicts:\n");

        for (int i = 0; i < conflicts.size(); i++) {
            ScheduleConflict c = conflicts.get(i);
            sb.append(String.format("%d. %s vs %s\n", i + 1, c.scheduleName1(), c.scheduleName2()));
            sb.append(String.format("   Type: %s, Severity: %s\n", c.conflictType(), c.severity()));
            sb.append(String.format("   Description: %s\n", c.description()));
        }

        sb.append("""

            Provide a JSON response with:
            {
              "summary": "A user-friendly summary of all conflicts (1-2 sentences)",
              "enhanced_resolutions": [
                {
                  "conflict_index": 0,
                  "best_resolution": "The ID of the best resolution from the existing options",
                  "reasoning": "Why this is the best choice",
                  "additional_suggestion": "Any smart alternative not in the basic options (optional)"
                }
              ],
              "user_tip": "A helpful tip for avoiding future conflicts"
            }

            Be practical and consider real-world lighting usage patterns. For example:
            - Morning routines need gradual wake-up lighting
            - Bedtime should have dimming sequences
            - Contradicting on/off commands are usually user errors
            - Similar scenes close together are often intended as backups
            """);

        return sb.toString();
    }

    private String scheduleToString(Schedule schedule) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", schedule.getName());
            map.put("trigger", schedule.getTriggerConfig());
            map.put("actions", schedule.getActions());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return schedule.getName();
        }
    }

    private String callOpenAI(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = Map.of(
            "model", openaiModel,
            "messages", List.of(
                Map.of("role", "system",
                       "content", "You are a smart home assistant specializing in schedule optimization."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions",
            HttpMethod.POST,
            request,
            String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private ConflictAnalysisResult parseAIResponse(
            String aiResponse, List<ScheduleConflict> basicConflicts) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String summary = root.path("summary").asText(
                String.format("Found %d potential conflict(s).", basicConflicts.size())
            );

            String userTip = root.path("user_tip").asText("");
            if (!userTip.isEmpty()) {
                summary += " Tip: " + userTip;
            }

            JsonNode enhancedResolutions = root.path("enhanced_resolutions");
            List<ScheduleConflict> enhancedConflicts = new ArrayList<>();

            for (int i = 0; i < basicConflicts.size(); i++) {
                ScheduleConflict original = basicConflicts.get(i);
                ScheduleConflict enhanced = enhanceConflict(original, enhancedResolutions, i);
                enhancedConflicts.add(enhanced);
            }

            return new ConflictAnalysisResult(true, enhancedConflicts, summary);

        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            String summary = String.format(
                "Found %d potential conflict(s). Review the suggested resolutions.",
                basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }
    }

    private ScheduleConflict enhanceConflict(
            ScheduleConflict original, JsonNode enhancedResolutions, int index) {
        String additionalSuggestion = null;
        String bestResolutionId = null;

        if (enhancedResolutions.isArray()) {
            for (JsonNode enhancement : enhancedResolutions) {
                if (enhancement.path("conflict_index").asInt(-1) == index) {
                    additionalSuggestion = enhancement.path("additional_suggestion").asText(null);
                    bestResolutionId = enhancement.path("best_resolution").asText(null);
                    break;
                }
            }
        }

        List<ConflictResolution> enhancedResList = new ArrayList<>(original.resolutions());
        if (additionalSuggestion != null && !additionalSuggestion.isBlank()) {
            enhancedResList.add(0, new ConflictResolution(
                "ai_suggested",
                additionalSuggestion,
                "custom",
                Map.of("ai_generated", true)
            ));
        }

        if (bestResolutionId != null) {
            String finalBestId = bestResolutionId;
            enhancedResList.sort((a, b) -> {
                if (a.id().equals(finalBestId)) {
                    return -1;
                }
                if (b.id().equals(finalBestId)) {
                    return 1;
                }
                if (a.id().equals("ai_suggested")) {
                    return -1;
                }
                if (b.id().equals("ai_suggested")) {
                    return 1;
                }
                return 0;
            });
        }

        return new ScheduleConflict(
            original.scheduleId1(),
            original.scheduleName1(),
            original.scheduleId2(),
            original.scheduleName2(),
            original.conflictType(),
            original.description(),
            original.severity(),
            enhancedResList
        );
    }
}

