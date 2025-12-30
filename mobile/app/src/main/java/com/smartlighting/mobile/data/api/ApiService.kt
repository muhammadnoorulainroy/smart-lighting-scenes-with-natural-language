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
    
    @POST("${Constants.Endpoints.SCENES}/{sceneId}/activate")
    suspend fun activateScene(@Path("sceneId") sceneId: String): Response<Map<String, Any>>
    
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
}
