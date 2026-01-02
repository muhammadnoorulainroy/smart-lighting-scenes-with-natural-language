package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for color name to RGB conversion.
 *

 */
@Component
@Slf4j
public class NlpColorUtils {

    /**
     * Convert a color name to RGB values.
     *
     * @param colorName the color name (e.g., "red", "blue", "warm white")
     * @return RGB values as a list [R, G, B], or null if unknown
     */
    public List<Integer> colorNameToRgb(String colorName) {
        if (colorName == null) {
            return null;
        }
        String color = colorName.toLowerCase().trim();
        return switch (color) {
            case "red" -> List.of(255, 0, 0);
            case "green" -> List.of(0, 255, 0);
            case "blue" -> List.of(0, 0, 255);
            case "yellow" -> List.of(255, 255, 0);
            case "orange" -> List.of(255, 165, 0);
            case "purple", "violet" -> List.of(128, 0, 128);
            case "pink" -> List.of(255, 105, 180);
            case "cyan", "aqua" -> List.of(0, 255, 255);
            case "magenta" -> List.of(255, 0, 255);
            case "white" -> List.of(255, 255, 255);
            case "warm white", "warm" -> List.of(255, 244, 229);
            case "cool white", "cool" -> List.of(200, 220, 255);
            case "gold" -> List.of(255, 215, 0);
            case "lime" -> List.of(0, 255, 0);
            case "coral" -> List.of(255, 127, 80);
            case "salmon" -> List.of(250, 128, 114);
            case "teal" -> List.of(0, 128, 128);
            case "indigo" -> List.of(75, 0, 130);
            case "turquoise" -> List.of(64, 224, 208);
            default -> null;
        };
    }

    /**
     * Parse an RGB string like "[255, 0, 0]" into a list of integers.
     *
     * @param rgbStr the RGB string
     * @return RGB values as a list [R, G, B], or null if invalid
     */
    public List<Integer> parseRgbString(String rgbStr) {
        if (rgbStr == null) {
            return null;
        }
        try {
            String cleaned = rgbStr.replaceAll("[\\[\\]\\s]", "");
            String[] parts = cleaned.split(",");
            if (parts.length >= 3) {
                return List.of(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
                );
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse RGB string: {}", rgbStr);
        }
        return null;
    }
}

