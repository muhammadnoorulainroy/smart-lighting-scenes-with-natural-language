package com.smartlighting.mobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for sending natural language commands to the backend
 */
data class LightCommand(
    @SerializedName("description")
    val description: String,
    @SerializedName("roomId")
    val roomId: String? = null,
    @SerializedName("groupId")
    val groupId: String? = null,
    @SerializedName("sceneId")
    val sceneId: String? = null
)

/**
 * Response from executing a light command
 */
data class CommandResult(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Any? = null
)

