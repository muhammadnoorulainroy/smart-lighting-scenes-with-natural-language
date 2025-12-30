package com.smartlighting.mobile.data.model

import com.google.gson.annotations.SerializedName

data class Device(
    @SerializedName("id")
    val id: String,
    @SerializedName("roomId")
    val roomId: String? = null,
    @SerializedName("roomName")
    val roomName: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("mqttCmdTopic")
    val mqttCmdTopic: String? = null,
    @SerializedName("mqttStateTopic")
    val mqttStateTopic: String? = null,
    @SerializedName("metaJson")
    val metaJson: Map<String, Any>? = null,
    @SerializedName("isActive")
    val isActive: Boolean = true,
    @SerializedName("deviceState")
    val deviceState: DeviceState? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class DeviceState(
    @SerializedName("isOn")
    val isOn: Boolean = false,
    @SerializedName("brightnessPct")
    val brightnessPct: Int? = null,
    @SerializedName("colorTempMired")
    val colorTempMired: Int? = null,
    @SerializedName("rgbColor")
    val rgbColor: String? = null,
    @SerializedName("lastSeen")
    val lastSeen: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

