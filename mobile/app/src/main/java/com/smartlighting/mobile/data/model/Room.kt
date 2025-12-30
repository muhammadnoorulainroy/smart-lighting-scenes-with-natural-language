package com.smartlighting.mobile.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model representing a room/group with smart lights
 */
data class Room(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("devices")
    val devices: List<Device>? = null,
    @SerializedName("devicesList")
    val devicesList: List<Device>? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdByName")
    val createdByName: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)


