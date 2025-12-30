package com.smartlighting.mobile.data.model

import com.google.gson.annotations.SerializedName

data class Scene(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("actions")
    val actions: List<SceneAction>? = null,
    @SerializedName("isGlobal")
    val isGlobal: Boolean? = false,
    @SerializedName("ownerId")
    val ownerId: String? = null,
    @SerializedName("ownerName")
    val ownerName: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class SceneAction(
    @SerializedName("deviceId")
    val deviceId: String? = null,
    @SerializedName("on")
    val on: Boolean? = null,
    @SerializedName("brightness")
    val brightness: Int? = null,
    @SerializedName("colorTemp")
    val colorTemp: Int? = null,
    @SerializedName("rgbColor")
    val rgbColor: String? = null
)

