package com.smartlighting.mobile.data.repository

import com.smartlighting.mobile.data.api.ApiService
import com.smartlighting.mobile.data.local.PersistentCookieJar
import com.smartlighting.mobile.data.local.TokenManager
import com.smartlighting.mobile.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LightingRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val persistentCookieJar: PersistentCookieJar
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
    
    suspend fun deleteScene(sceneId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteScene(sceneId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete scene: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateScene(sceneId: String, scene: Scene): Result<Scene> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateScene(sceneId, scene)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update scene: ${response.code()}"))
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
                    val body = response.body()!!
                    // Save user info (auth uses cookies/session)
                    tokenManager.saveToken("authenticated") // Mark as authenticated
                    tokenManager.saveUserInfo(
                        userId = body["id"]?.toString(),
                        email = body["email"]?.toString(),
                        name = body["name"]?.toString()
                    )
                    Result.success(body)
                } else {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("Cannot connect to server. Please check your network connection."))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Cannot reach server. Please check your network connection."))
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("Connection timeout. Please try again."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "An unexpected error occurred"))
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
    
    suspend fun logout(): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.logout()
                // Clear local auth state and cookies
                tokenManager.clearAuth()
                persistentCookieJar.clear()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    // Even if backend fails, we cleared local state
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                // Even if network fails, we cleared local state
                tokenManager.clearAuth()
                persistentCookieJar.clear()
                Result.success(Unit)
            }
        }
    
    // Auth methods
    suspend fun signup(email: String, password: String, name: String): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
            try {
                val payload = mapOf("email" to email, "password" to password, "name" to name)
                val response = apiService.signup(payload)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Save user info (auth uses cookies/session)
                    tokenManager.saveToken("authenticated") // Mark as authenticated
                    tokenManager.saveUserInfo(
                        userId = body["id"]?.toString(),
                        email = body["email"]?.toString() ?: email,
                        name = body["name"]?.toString() ?: name
                    )
                    Result.success(body)
                } else {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("Cannot connect to server. Please check your network connection."))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Cannot reach server. Please check your network connection."))
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("Connection timeout. Please try again."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "An unexpected error occurred"))
            }
        }
    
    suspend fun login(email: String, password: String): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
            try {
                val payload = mapOf("email" to email, "password" to password)
                val response = apiService.login(payload)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Save user info (auth uses cookies/session)
                    tokenManager.saveToken("authenticated") // Mark as authenticated
                    tokenManager.saveUserInfo(
                        userId = body["id"]?.toString(),
                        email = body["email"]?.toString() ?: email,
                        name = body["name"]?.toString()
                    )
                    Result.success(body)
                } else {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("Cannot connect to server. Please check your network connection."))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Cannot reach server. Please check your network connection."))
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("Connection timeout. Please try again."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "An unexpected error occurred"))
            }
        }
    
    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        return try {
            if (errorBody != null) {
                // Try to parse JSON error response
                val json = org.json.JSONObject(errorBody)
                json.optString("error") ?: json.optString("message") ?: "HTTP $statusCode"
            } else {
                when (statusCode) {
                    400 -> "Invalid email or password"
                    401 -> "Invalid credentials"
                    403 -> "Access denied"
                    404 -> "Service not found"
                    409 -> "Email already exists"
                    500 -> "Server error. Please try again later"
                    else -> "HTTP $statusCode"
                }
            }
        } catch (e: Exception) {
            when (statusCode) {
                400 -> "Invalid email or password"
                401 -> "Invalid credentials"
                403 -> "Access denied"
                404 -> "Service not found"
                409 -> "Email already exists"
                500 -> "Server error. Please try again later"
                else -> "HTTP $statusCode"
            }
        }
    }
    
    // Schedule methods
    suspend fun getSchedules(): Result<List<Schedule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSchedules()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No schedules data received"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSchedule(scheduleId: String): Result<Schedule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSchedule(scheduleId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No schedule data received"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createSchedule(schedule: Schedule): Result<Schedule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createSchedule(schedule)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create schedule: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSchedule(scheduleId: String, schedule: Schedule): Result<Schedule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateSchedule(scheduleId, schedule)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update schedule: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleSchedule(scheduleId: String): Result<Schedule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.toggleSchedule(scheduleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to toggle schedule: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteSchedule(scheduleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteSchedule(scheduleId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete schedule: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun parseNlpCommand(text: String): Result<NlpCommand> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("text" to text)
            val response = apiService.parseNlpCommand(payload)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to parse command: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmNlpCommand(command: NlpCommand): Result<NlpCommand> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.confirmNlpCommand(command)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to confirm command: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resolveConflict(scheduleId: String, resolutionId: String, params: Map<String, Any>): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
            try {
                val payload = mapOf("scheduleId" to scheduleId, "resolutionId" to resolutionId, "params" to params)
                val response = apiService.resolveConflict(payload)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to resolve conflict: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
