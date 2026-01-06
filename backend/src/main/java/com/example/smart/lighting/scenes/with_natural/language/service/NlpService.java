package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.dto.ConflictAnalysisDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.entity.NlpCommand;
import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.NlpCommandRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictAnalysisResult;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictResolution;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ScheduleConflict;
import com.example.smart.lighting.scenes.with_natural.language.service.nlp.NlpCommandExecutor;
import com.example.smart.lighting.scenes.with_natural.language.service.nlp.NlpCommandParser;
import com.example.smart.lighting.scenes.with_natural.language.service.nlp.NlpOpenAiClient;
import com.example.smart.lighting.scenes.with_natural.language.service.nlp.NlpScheduleBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for parsing natural language commands using OpenAI.
 * Converts user commands into structured lighting actions.
 *
 * <p>This service delegates to specialized components:</p>
 * <ul>
 *   <li>{@link NlpOpenAiClient} - OpenAI API communication</li>
 *   <li>{@link NlpCommandParser} - Command parsing and validation</li>
 *   <li>{@link NlpCommandExecutor} - Command execution via MQTT</li>
 *   <li>{@link NlpScheduleBuilder} - Schedule creation from commands</li>
 * </ul>
 *

 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NlpService {

    private final ObjectMapper objectMapper;
    private final NlpCommandRepository nlpCommandRepository;
    private final SceneRepository sceneRepository;
    private final ScheduleConflictService conflictService;
    private final NlpOpenAiClient openAiClient;
    private final NlpCommandParser commandParser;
    private final NlpCommandExecutor commandExecutor;
    private final NlpScheduleBuilder scheduleBuilder;

    /**
     * Parse a natural language command without executing it.
     * Returns a preview of what the command will do.
     *
     * @param text the natural language command text
     * @return parsed command DTO with preview
     */
    public NlpCommandDto parseCommand(String text) {
        log.info("Parsing NLP command: {}", text);

        if (!openAiClient.isConfigured()) {
            return NlpCommandDto.builder()
                .text(text)
                .valid(false)
                .error("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.")
                .timestamp(LocalDateTime.now())
                .build();
        }

        try {
            List<Scene> scenes = sceneRepository.findByIsActiveTrue();
            List<String> sceneNames = scenes.stream().map(Scene::getName).toList();

            String prompt = openAiClient.buildParsePrompt(text, sceneNames);
            String response = openAiClient.callOpenAI(prompt);
            log.debug("OpenAI response: {}", response);

            ParsedCommand parsed = commandParser.parseOpenAIResponse(response);

            if (parsed == null) {
                return NlpCommandDto.builder()
                    .text(text)
                    .valid(false)
                    .error("Could not understand the command. Please try rephrasing.")
                    .timestamp(LocalDateTime.now())
                    .build();
            }

            String validationError = commandParser.validateParsedCommand(parsed);
            if (validationError != null) {
                return NlpCommandDto.builder()
                    .text(text)
                    .parsed(parsed)
                    .valid(false)
                    .error(validationError)
                    .timestamp(LocalDateTime.now())
                    .build();
            }

            String preview = commandParser.generatePreview(parsed);
            boolean isScheduled = parsed.getSchedule() != null;

            ConflictAnalysisDto conflictAnalysis = null;
            if (isScheduled) {
                conflictAnalysis = checkForScheduleConflicts(parsed);
            }

            return NlpCommandDto.builder()
                .text(text)
                .parsed(parsed)
                .preview(preview)
                .valid(true)
                .isScheduled(isScheduled)
                .conflictAnalysis(conflictAnalysis)
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error parsing NLP command: {}", e.getMessage(), e);
            return NlpCommandDto.builder()
                .text(text)
                .valid(false)
                .error("Error processing command: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Execute a parsed command.
     *
     * @param commandDto the parsed command DTO
     * @param user the user executing the command
     * @return updated command DTO with execution result
     */
    public NlpCommandDto executeCommand(NlpCommandDto commandDto, User user) {
        if (!Boolean.TRUE.equals(commandDto.getValid())) {
            return commandDto;
        }

        ParsedCommand parsed = commandDto.getParsed();

        try {
            String result;

            if (Boolean.TRUE.equals(commandDto.getIsScheduled())) {
                // GUEST users cannot create schedules
                if (user != null && user.getRole() == User.UserRole.GUEST) {
                    commandDto.setExecuted(false);
                    commandDto.setResult("Error: Guests cannot create schedules. Please ask an owner or resident.");
                    return commandDto;
                }
                Schedule schedule = scheduleBuilder.createScheduleFromParsed(parsed, user);
                result = "Created schedule: " + schedule.getName();
                commandDto.setResult(result);
            } else {
                result = commandExecutor.executeImmediateCommand(parsed);
                commandDto.setResult(result);
            }

            commandDto.setExecuted(true);
            saveCommandHistory(commandDto, user);

        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
            commandDto.setExecuted(false);
            commandDto.setResult("Error: " + e.getMessage());
        }

        return commandDto;
    }

    /**
     * Apply a conflict resolution.
     *
     * @param scheduleId the schedule ID
     * @param resolutionId the resolution ID
     * @param params resolution parameters
     * @return result message
     */
    public String applyConflictResolution(UUID scheduleId, String resolutionId, Map<String, Object> params) {
        return conflictService.applyResolution(scheduleId, resolutionId, params);
    }

    private ConflictAnalysisDto checkForScheduleConflicts(ParsedCommand parsed) {
        try {
            Schedule tempSchedule = scheduleBuilder.buildTemporarySchedule(parsed);
            ConflictAnalysisResult result = conflictService.detectConflicts(tempSchedule);

            if (!result.hasConflicts()) {
                return null;
            }

            List<ConflictAnalysisDto.ConflictDto> conflictDtos = result.conflicts().stream()
                .map(this::toConflictDto)
                .toList();

            return ConflictAnalysisDto.builder()
                .hasConflicts(true)
                .summary(result.summary())
                .conflicts(conflictDtos)
                .build();

        } catch (Exception e) {
            log.warn("Error checking for schedule conflicts: {}", e.getMessage());
            return null;
        }
    }

    private ConflictAnalysisDto.ConflictDto toConflictDto(ScheduleConflict conflict) {
        List<ConflictAnalysisDto.ResolutionDto> resolutionDtos = conflict.resolutions().stream()
            .map(this::toResolutionDto)
            .toList();

        return ConflictAnalysisDto.ConflictDto.builder()
            .scheduleId1(conflict.scheduleId1().toString())
            .scheduleName1(conflict.scheduleName1())
            .scheduleId2(conflict.scheduleId2().toString())
            .scheduleName2(conflict.scheduleName2())
            .conflictType(conflict.conflictType())
            .description(conflict.description())
            .severity(conflict.severity())
            .resolutions(resolutionDtos)
            .build();
    }

    private ConflictAnalysisDto.ResolutionDto toResolutionDto(ConflictResolution resolution) {
        return ConflictAnalysisDto.ResolutionDto.builder()
            .id(resolution.id())
            .description(resolution.description())
            .action(resolution.action())
            .changes(resolution.changes())
            .build();
    }

    private void saveCommandHistory(NlpCommandDto commandDto, User user) {
        try {
            Map<String, Object> parsedMap = objectMapper.convertValue(
                commandDto.getParsed(), new TypeReference<>() {});

            NlpCommand nlpCommand = NlpCommand.builder()
                .rawInput(commandDto.getText())
                .parsedJson(parsedMap)
                .executed(commandDto.getExecuted())
                .executionResult(commandDto.getResult())
                .isScheduled(commandDto.getIsScheduled())
                .user(user)
                .build();

            nlpCommandRepository.save(nlpCommand);
        } catch (Exception e) {
            log.error("Error saving command history: {}", e.getMessage());
        }
    }
}
