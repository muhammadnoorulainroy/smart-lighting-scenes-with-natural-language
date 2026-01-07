package com.smartlighting.mobile.data.model

import com.google.gson.annotations.SerializedName

data class Schedule(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("triggerType")
    val triggerType: String,  // "time" or "sun"
    @SerializedName("triggerConfig")
    val triggerConfig: Map<String, Any>? = null,
    @SerializedName("actionType")
    val actionType: String? = null,  // "scene" or "device"
    @SerializedName("actionConfig")
    val actionConfig: Map<String, Any>? = null,
    @SerializedName("conditions")
    val conditions: List<Map<String, Any>>? = null,
    @SerializedName("actions")
    val actions: List<ScheduleAction>? = null,
    @SerializedName("lastTriggeredAt")
    val lastTriggeredAt: String? = null,
    @SerializedName("triggerCount")
    val triggerCount: Int? = 0,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdByName")
    val createdByName: String? = null
)

data class ScheduleAction(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("intent")
    val intent: String? = null,
    @SerializedName("target")
    val target: String? = null,
    @SerializedName("params")
    val params: Map<String, Any>? = null,
    @SerializedName("scene")
    val scene: String? = null,
    @SerializedName("scene_id")
    val sceneId: String? = null
)

data class NlpCommand(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("parsed")
    val parsed: ParsedCommand? = null,
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("preview")
    val preview: String? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("isScheduled")
    val isScheduled: Boolean? = false,
    @SerializedName("executed")
    val executed: Boolean? = false,
    @SerializedName("conflictAnalysis")
    val conflictAnalysis: ConflictAnalysis? = null,
    @SerializedName("result")
    val result: String? = null
)

data class ParsedCommand(
    @SerializedName("intent")
    val intent: String,
    @SerializedName("target")
    val target: String? = null,
    @SerializedName("params")
    val params: Map<String, Any>? = null,
    @SerializedName("scene")
    val scene: String? = null,
    @SerializedName("schedule")
    val schedule: ScheduleConfig? = null,
    @SerializedName("confidence")
    val confidence: Double? = null
)

/**
 * Schedule configuration parsed from natural language.
 * Matches backend's NlpCommandDto.ScheduleConfig
 */
data class ScheduleConfig(
    @SerializedName("time")
    val time: String? = null,           // Time in HH:MM format (for time triggers)
    @SerializedName("trigger")
    val trigger: String? = null,        // Sun event: "sunset" or "sunrise"
    @SerializedName("offsetMinutes")
    val offsetMinutes: Int? = null,     // Offset in minutes for sun events
    @SerializedName("recurrence")
    val recurrence: Any? = null         // Recurrence: "once", "daily", "weekdays", "weekends", or list of days
)

data class ConflictAnalysis(
    @SerializedName("hasConflicts")
    val hasConflicts: Boolean,
    @SerializedName("summary")
    val summary: String? = null,
    @SerializedName("conflicts")
    val conflicts: List<Conflict>? = null
)

data class Conflict(
    @SerializedName("scheduleId2")
    val scheduleId2: String,
    @SerializedName("scheduleName2")
    val scheduleName2: String,
    @SerializedName("severity")
    val severity: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("resolutions")
    val resolutions: List<Resolution>? = null
)

data class Resolution(
    @SerializedName("id")
    val id: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("changes")
    val changes: Map<String, Any>? = null
)

