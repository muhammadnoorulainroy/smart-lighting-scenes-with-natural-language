package com.smartlighting.mobile.data.repository

import com.smartlighting.mobile.data.api.ApiService
import com.smartlighting.mobile.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LightingRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    suspend fun getRooms(): Result<List<Room>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRooms()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No rooms data received"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRoom(roomId: String): Result<Room> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRoom(roomId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No room data received"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getScenes(): Result<List<Scene>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScenes()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No scenes data received"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createScene(scene: Scene): Result<Scene> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createScene(scene)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create scene: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun activateScene(sceneId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.activateScene(sceneId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to activate scene: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendNaturalLanguageCommand(command: String, roomId: String?): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
            try {
                val payload = mutableMapOf<String, String>("command" to command)
                roomId?.let { payload["roomId"] = it }
                val response = apiService.sendNaturalLanguageCommand(payload)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to process command: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun sendLightCommand(command: LightCommand): Result<CommandResult> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendLightCommand(command)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.error ?: "Failed to execute command"))
                    }
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun getGroups(): Result<List<Room>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGroups()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to fetch groups"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun authenticateWithGoogle(idToken: String): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
            try {
                val payload = mapOf("idToken" to idToken)
                val response = apiService.authenticateWithGoogle(payload)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    } catch (e: Exception) {
                        "HTTP ${response.code()}: ${e.message}"
                    }
                    Result.failure(Exception("Authentication failed: $errorMessage"))
                }
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Cannot reach backend. Check if backend is running and phone is on same WiFi network."))
            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("Connection refused. Backend might not be running on port 8080."))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    
    suspend fun getAuthConfig(): Result<Map<String, String>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAuthConfig()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch auth config: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
