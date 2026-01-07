package com.smartlighting.mobile.data.websocket

import android.util.Log
import com.smartlighting.mobile.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket event types from the backend
 */
sealed class WebSocketEvent {
    // Schedule events
    data class ScheduleCreated(val scheduleId: String, val scheduleName: String) : WebSocketEvent()
    data class ScheduleUpdated(val scheduleId: String, val scheduleName: String) : WebSocketEvent()
    data class ScheduleDeleted(val scheduleId: String) : WebSocketEvent()
    data class ScheduleToggled(val scheduleId: String, val scheduleName: String, val enabled: Boolean) : WebSocketEvent()
    data class ScheduleTriggered(val scheduleId: String, val scheduleName: String, val triggerCount: Int) : WebSocketEvent()
    
    // Scene events
    data class SceneCreated(val sceneId: String, val sceneName: String) : WebSocketEvent()
    data class SceneUpdated(val sceneId: String, val sceneName: String) : WebSocketEvent()
    data class SceneDeleted(val sceneId: String) : WebSocketEvent()
    data class SceneApplied(val sceneId: String, val sceneName: String, val devicesAffected: Int) : WebSocketEvent()
    data class SceneConfirmed(val sceneId: String?, val sceneName: String, val correlationId: String) : WebSocketEvent()
    
    // Device events
    data class DeviceStateChange(val deviceId: String, val state: Map<String, Any>) : WebSocketEvent()
    
    // Connection events
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}

/**
 * Simple WebSocket service for real-time updates
 * Note: This uses a simplified WebSocket approach. For full STOMP support,
 * consider using a STOMP client library.
 */
@Singleton
class WebSocketService @Inject constructor() {
    
    private val TAG = "WebSocketService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    
    /**
     * Connect to the WebSocket server
     * Uses SockJS fallback URL pattern
     */
    fun connect() {
        if (isConnected) {
            Log.d(TAG, "Already connected")
            return
        }
        
        val wsUrl = Constants.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/ws/websocket"
        
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                isConnected = true
                scope.launch { _events.emit(WebSocketEvent.Connected) }
                
                // Subscribe to topics using STOMP-like frames
                subscribeToTopics(webSocket)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message: $text")
                parseAndEmitMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                isConnected = false
                scope.launch { _events.emit(WebSocketEvent.Disconnected) }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                isConnected = false
                scope.launch { _events.emit(WebSocketEvent.Error(t.message ?: "Connection failed")) }
            }
        })
    }
    
    /**
     * Send STOMP CONNECT and SUBSCRIBE frames
     */
    private fun subscribeToTopics(ws: WebSocket) {
        // Send STOMP CONNECT frame
        val connectFrame = "CONNECT\n" +
                "accept-version:1.1,1.2\n" +
                "heart-beat:10000,10000\n\n\u0000"
        ws.send(connectFrame)
        
        // Subscribe to schedules topic
        val subscribeSchedules = "SUBSCRIBE\n" +
                "id:sub-schedules\n" +
                "destination:/topic/schedules\n\n\u0000"
        ws.send(subscribeSchedules)
        
        // Subscribe to scenes topic
        val subscribeScenes = "SUBSCRIBE\n" +
                "id:sub-scenes\n" +
                "destination:/topic/scenes\n\n\u0000"
        ws.send(subscribeScenes)
        
        // Subscribe to device state topic
        val subscribeDevices = "SUBSCRIBE\n" +
                "id:sub-devices\n" +
                "destination:/topic/device-state\n\n\u0000"
        ws.send(subscribeDevices)
        
        Log.d(TAG, "Subscribed to topics")
    }
    
    /**
     * Parse STOMP message and emit event
     */
    private fun parseAndEmitMessage(text: String) {
        try {
            // Parse STOMP MESSAGE frame
            if (text.startsWith("MESSAGE")) {
                val bodyStart = text.indexOf("\n\n")
                if (bodyStart > 0) {
                    val body = text.substring(bodyStart + 2).trimEnd('\u0000')
                    parseJsonMessage(body)
                }
            } else if (text.startsWith("CONNECTED")) {
                Log.d(TAG, "STOMP connected")
            } else if (text.startsWith("ERROR")) {
                Log.e(TAG, "STOMP error: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
        }
    }
    
    /**
     * Parse JSON message body and emit appropriate event
     */
    private fun parseJsonMessage(body: String) {
        try {
            val json = JSONObject(body)
            val type = json.optString("type", "")
            val data = json.optJSONObject("data")
            
            Log.d(TAG, "Received event: $type")
            
            scope.launch {
                when (type) {
                    "SCHEDULE_CREATED" -> {
                        val scheduleId = data?.optString("scheduleId") ?: ""
                        val scheduleName = data?.optString("scheduleName") ?: ""
                        _events.emit(WebSocketEvent.ScheduleCreated(scheduleId, scheduleName))
                    }
                    "SCHEDULE_UPDATED" -> {
                        val scheduleId = data?.optString("scheduleId") ?: ""
                        val scheduleName = data?.optString("scheduleName") ?: ""
                        _events.emit(WebSocketEvent.ScheduleUpdated(scheduleId, scheduleName))
                    }
                    "SCHEDULE_DELETED" -> {
                        val scheduleId = data?.optString("scheduleId") ?: ""
                        _events.emit(WebSocketEvent.ScheduleDeleted(scheduleId))
                    }
                    "SCHEDULE_TOGGLED" -> {
                        val scheduleId = data?.optString("scheduleId") ?: ""
                        val scheduleName = data?.optString("scheduleName") ?: ""
                        val enabled = data?.optBoolean("enabled") ?: false
                        _events.emit(WebSocketEvent.ScheduleToggled(scheduleId, scheduleName, enabled))
                    }
                    "SCHEDULE_TRIGGERED" -> {
                        val scheduleId = data?.optString("scheduleId") ?: ""
                        val scheduleName = data?.optString("scheduleName") ?: ""
                        val triggerCount = data?.optInt("triggerCount") ?: 0
                        _events.emit(WebSocketEvent.ScheduleTriggered(scheduleId, scheduleName, triggerCount))
                    }
                    "SCENE_CREATED" -> {
                        val sceneId = data?.optString("sceneId") ?: ""
                        val sceneName = data?.optString("sceneName") ?: ""
                        _events.emit(WebSocketEvent.SceneCreated(sceneId, sceneName))
                    }
                    "SCENE_UPDATED" -> {
                        val sceneId = data?.optString("sceneId") ?: ""
                        val sceneName = data?.optString("sceneName") ?: ""
                        _events.emit(WebSocketEvent.SceneUpdated(sceneId, sceneName))
                    }
                    "SCENE_DELETED" -> {
                        val sceneId = data?.optString("sceneId") ?: ""
                        _events.emit(WebSocketEvent.SceneDeleted(sceneId))
                    }
                    "SCENE_APPLIED" -> {
                        val sceneId = data?.optString("sceneId") ?: ""
                        val sceneName = data?.optString("sceneName") ?: ""
                        val devicesAffected = data?.optInt("devicesAffected") ?: 0
                        _events.emit(WebSocketEvent.SceneApplied(sceneId, sceneName, devicesAffected))
                    }
                    "SCENE_CONFIRMED" -> {
                        val sceneId = data?.optString("sceneId")
                        val sceneName = data?.optString("sceneName") ?: ""
                        val correlationId = data?.optString("correlationId") ?: ""
                        _events.emit(WebSocketEvent.SceneConfirmed(sceneId, sceneName, correlationId))
                    }
                    "DEVICE_STATE_UPDATE", "DEVICE_STATE_CHANGE" -> {
                        val deviceId = data?.optString("deviceId") ?: ""
                        val stateJson = data?.optJSONObject("state")
                        val state = mutableMapOf<String, Any>()
                        stateJson?.keys()?.forEach { key ->
                            state[key] = stateJson.get(key)
                        }
                        _events.emit(WebSocketEvent.DeviceStateChange(deviceId, state))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON message: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected
}

