package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client for communicating with OpenAI API for NLP command parsing.
 *

 */
@Component
@Slf4j
public class NlpOpenAiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openaiModel;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Check if OpenAI API is configured.
     *
     * @return true if API key is configured
     */
    public boolean isConfigured() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }

    /**
     * Call OpenAI API with the given prompt.
     *
     * @param prompt the prompt to send
     * @return the response content
     */
    @SuppressWarnings("unchecked")
    public String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = Map.of(
            "model", openaiModel,
            "messages", List.of(
                Map.of("role", "system",
                       "content", "You are a JSON-only response bot. Never include markdown or explanations."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            request,
            Map.class
        );

        if (response.getBody() != null) {
            List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("No response from OpenAI");
    }

    /**
     * Build the prompt for parsing a natural language command.
     *
     * @param userInput the user's command text
     * @param sceneNames available scene names
     * @return the formatted prompt
     */
    public String buildParsePrompt(String userInput, List<String> sceneNames) {
        return """
            You are a smart home lighting assistant. Parse the user's command into a structured JSON.

            Available rooms: bedroom, living_room, kitchen, bathroom, hallway, all
            Available scenes: %s
            Available actions:
            - light.on: Turn lights on
            - light.off: Turn lights off
            - light.brightness: Set brightness (0-100)
            - light.color: Set RGB color
            - light.color_temp: Set color temperature (2700-6500 Kelvin)
            - scene.apply: Apply a predefined scene
            - scene.create: Create a new scene

            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "intent": "light.brightness",
              "target": "bedroom",
              "params": {"brightness": 30},
              "scene": null,
              "schedule": null,
              "confidence": 0.95
            }

            For scheduled commands, include:
            {
              "intent": "light.off",
              "target": "living_room",
              "params": {},
              "scene": null,
              "schedule": {
                "time": "07:00",
                "trigger": null,
                "offsetMinutes": null,
                "recurrence": "daily"
              },
              "confidence": 0.9
            }

            For sun-based triggers (sunset/sunrise):
            {
              "schedule": {
                "time": null,
                "trigger": "sunset",
                "offsetMinutes": -30,
                "recurrence": "daily"
              }
            }

            Rules:
            - For "all lights" or no room specified, use target: "all"
            - For colors, convert to RGB array [r, g, b]
            - For "warm" colors, use color_temp: 2700-3000
            - For "cool" colors, use color_temp: 5000-6500
            - For "every day" or "daily", use recurrence: "daily"
            - For "weekdays", use recurrence: "weekdays"
            - Set confidence based on how certain you are (0.0 to 1.0)

            User command: "%s"
            """.formatted(String.join(", ", sceneNames), userInput);
    }
}

