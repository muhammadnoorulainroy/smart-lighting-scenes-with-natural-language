package com.example.smart.lighting.scenes.with_natural.language.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

/**
 * Service for interacting with OpenAI API for natural language processing.
 *
 * <p>Uses GPT models to parse natural language lighting commands into
 * structured JSON format that can be executed by the system.</p>
 *
 * <h3>Supported Commands:</h3>
 * <ul>
 *   <li>Turn on/off lights by room</li>
 *   <li>Set brightness levels</li>
 *   <li>Set colors (by name or RGB)</li>
 *   <li>Create and apply scenes</li>
 * </ul>
 *

 * @see ParsedCommand
 */
@Slf4j
@Service
public class OpenAIService {

    private final OpenAiService openAiService;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public OpenAIService(
        @Value("${openai.api-key}") String apiKey,
        @Value("${openai.model}") String model,
        @Value("${openai.max-tokens}") int maxTokens,
        @Value("${openai.temperature}") double temperature
    ) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        log.info("OpenAI Service initialized with model: {}", model);
    }

    /**
     * Parse natural language command into structured format.
     */
    public ParsedCommand parseCommand(String userInput, List<String> availableRooms) {
        String systemPrompt = buildSystemPrompt(availableRooms);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userInput));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .maxTokens(maxTokens)
            .temperature(temperature)
            .build();

        try {
            var response = openAiService.createChatCompletion(request);
            String result = response.getChoices().get(0).getMessage().getContent();
            log.debug("OpenAI Response: {}", result);
            return parseResponse(result);
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            return new ParsedCommand("error", null, null, null, null, "Failed to parse command");
        }
    }

    private String buildSystemPrompt(List<String> availableRooms) {
        return String.format("""
            You are a smart home lighting assistant. Parse user commands into JSON format.

            Available rooms: %s

            Output ONLY valid JSON in this format:
            {
              "action": "set_color|set_brightness|turn_on|turn_off|create_scene",
              "room": "room_name or null for all",
              "color": "color_name or null",
              "brightness": number 0-100 or null,
              "rgb": [r,g,b] or null
            }

            Examples:
            - "turn on bedroom lights" -> {"action":"turn_on","room":"bedroom"}
            - "set living room to red" -> {"action":"set_color","room":"living","color":"red","rgb":[255,0,0]}
            - "dim kitchen to 30%%" -> {"action":"set_brightness","room":"kitchen","brightness":30}
            - "turn off all lights" -> {"action":"turn_off","room":null}
            - "make bedroom purple 50%%" -> {"action":"set_color","room":"bedroom","brightness":50,"rgb":[128,0,128]}

            Color mappings:
            - red: [255,0,0], green: [0,255,0], blue: [0,0,255]
            - yellow: [255,255,0], cyan: [0,255,255], magenta: [255,0,255]
            - orange: [255,128,0], purple: [128,0,255], pink: [255,0,128]
            - white: [255,255,255], warm: [255,200,150], cool: [200,220,255]

            Always respond with valid JSON only, no explanations.
            """, String.join(", ", availableRooms));
    }

    private ParsedCommand parseResponse(String response) {
        try {
            // Remove markdown code blocks if present
            String json = response.replaceAll("```json\n?", "").replaceAll("```\n?", "").trim();

            // Simple JSON parsing (you can use Jackson for more robust parsing)
            String action = extractValue(json, "action");
            String room = extractValue(json, "room");
            String color = extractValue(json, "color");
            Integer brightness = extractIntValue(json, "brightness");
            int[] rgb = extractRgbValue(json, "rgb");

            return new ParsedCommand(action, room, color, brightness, rgb, null);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return new ParsedCommand("error", null, null, null, null, "Failed to parse response");
        }
    }

    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private Integer extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*([0-9]+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }

    private int[] extractRgbValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([0-9]+),\\s*([0-9]+),\\s*([0-9]+)\\]";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
            };
        }
        return null;
    }

    public record ParsedCommand(
        String action,
        String room,
        String color,
        Integer brightness,
        int[] rgb,
        String error
    ) { }
}
