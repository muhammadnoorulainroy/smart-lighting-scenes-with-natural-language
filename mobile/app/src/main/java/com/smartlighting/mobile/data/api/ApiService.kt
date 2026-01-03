package com.smartlighting.mobile.data.api

import com.smartlighting.mobile.data.model.*
import com.smartlighting.mobile.util.Constants
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @GET(Constants.Endpoints.ROOMS)
    suspend fun getRooms(): Response<List<Room>>
    
    @GET("${Constants.Endpoints.ROOMS}/{roomId}")
    suspend fun getRoom(@Path("roomId") roomId: String): Response<Room>
    
    @GET(Constants.Endpoints.SCENES)
    suspend fun getScenes(): Response<List<Scene>>
    
    @GET("${Constants.Endpoints.SCENES}/{roomId}")
    suspend fun getScenesByRoom(@Path("roomId") roomId: String): Response<List<Scene>>
    
    @POST(Constants.Endpoints.SCENES)
    suspend fun createScene(@Body scene: Scene): Response<Scene>
    
    @POST("${Constants.Endpoints.SCENES}/{sceneId}/apply")
    suspend fun activateScene(@Path("sceneId") sceneId: String): Response<Map<String, Any>>
    
    @DELETE("${Constants.Endpoints.SCENES}/{sceneId}")
    suspend fun deleteScene(@Path("sceneId") sceneId: String): Response<Unit>
    
    @PUT("${Constants.Endpoints.SCENES}/{sceneId}")
    suspend fun updateScene(@Path("sceneId") sceneId: String, @Body scene: Scene): Response<Scene>
    
    @POST(Constants.Endpoints.CONTROL)
    suspend fun sendLightCommand(@Body command: LightCommand): Response<ApiResponse<CommandResult>>
    
    @POST("api/lighting/command")
    suspend fun sendNaturalLanguageCommand(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @GET(Constants.Endpoints.GROUPS)
    suspend fun getGroups(): Response<ApiResponse<List<Room>>>
    
    @POST("api/auth/mobile/google")
    suspend fun authenticateWithGoogle(@Body payload: Map<String, String>): Response<Map<String, Any>>
    
    @GET("api/auth/config")
    suspend fun getAuthConfig(): Response<Map<String, String>>
    
    @POST("api/auth/signup")
    suspend fun signup(@Body payload: Map<String, String>): Response<Map<String, Any>>
    
    @POST("api/auth/login")
    suspend fun login(@Body payload: Map<String, String>): Response<Map<String, Any>>
    
    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, Any>>
    
    @GET("api/schedules")
    suspend fun getSchedules(): Response<List<com.smartlighting.mobile.data.model.Schedule>>
    
    @GET("api/schedules/{scheduleId}")
    suspend fun getSchedule(@Path("scheduleId") scheduleId: String): Response<com.smartlighting.mobile.data.model.Schedule>
    
    @POST("api/schedules")
    suspend fun createSchedule(@Body schedule: com.smartlighting.mobile.data.model.Schedule): Response<com.smartlighting.mobile.data.model.Schedule>
    
    @PUT("api/schedules/{scheduleId}")
    suspend fun updateSchedule(@Path("scheduleId") scheduleId: String, @Body schedule: com.smartlighting.mobile.data.model.Schedule): Response<com.smartlighting.mobile.data.model.Schedule>
    
    @POST("api/schedules/{scheduleId}/toggle")
    suspend fun toggleSchedule(@Path("scheduleId") scheduleId: String): Response<com.smartlighting.mobile.data.model.Schedule>
    
    @DELETE("api/schedules/{scheduleId}")
    suspend fun deleteSchedule(@Path("scheduleId") scheduleId: String): Response<Unit>
    
    @POST("api/nlp/parse")
    suspend fun parseNlpCommand(@Body payload: Map<String, String>): Response<com.smartlighting.mobile.data.model.NlpCommand>
    
    @POST("api/nlp/confirm")
    suspend fun confirmNlpCommand(@Body payload: com.smartlighting.mobile.data.model.NlpCommand): Response<com.smartlighting.mobile.data.model.NlpCommand>
    
    @POST("api/nlp/resolve-conflict")
    suspend fun resolveConflict(@Body payload: Map<String, Any>): Response<Map<String, Any>>
}
