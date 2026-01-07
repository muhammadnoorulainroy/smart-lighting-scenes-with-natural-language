package com.smartlighting.mobile.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.mobile.data.model.CommandResult
import com.smartlighting.mobile.data.model.NlpCommand
import com.smartlighting.mobile.data.model.Room
import com.smartlighting.mobile.data.model.Scene
import com.smartlighting.mobile.data.model.Schedule
import com.smartlighting.mobile.data.repository.LightingRepository
import com.smartlighting.mobile.data.websocket.WebSocketEvent
import com.smartlighting.mobile.data.websocket.WebSocketService
import com.smartlighting.mobile.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: LightingRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {
    
    private val TAG = "MainViewModel"
    
    /**
     * Whether the current user can edit (create/update/delete)
     * GUEST users cannot edit, only OWNER and RESIDENT can
     */
    val canEdit: Boolean
        get() = repository.canEdit()
    
    /**
     * Logout user - clears auth state and session
     */
    fun logout() {
        viewModelScope.launch {
            webSocketService.disconnect()
            repository.logout()
        }
    }
    
    private val _scenesState = MutableStateFlow<UiState<List<Scene>>>(UiState.Idle)
    val scenesState: StateFlow<UiState<List<Scene>>> = _scenesState.asStateFlow()
    
    private val _roomsState = MutableStateFlow<UiState<List<Room>>>(UiState.Idle)
    val roomsState: StateFlow<UiState<List<Room>>> = _roomsState.asStateFlow()
    
    private val _commandState = MutableStateFlow<UiState<CommandResult>>(UiState.Idle)
    val commandState: StateFlow<UiState<CommandResult>> = _commandState.asStateFlow()
    
    private val _activateSceneState = MutableStateFlow<UiState<Map<String, Any>>>(UiState.Idle)
    val activateSceneState: StateFlow<UiState<Map<String, Any>>> = _activateSceneState.asStateFlow()
    
    private val _sceneOperationState = MutableStateFlow<UiState<Scene>>(UiState.Idle)
    val sceneOperationState: StateFlow<UiState<Scene>> = _sceneOperationState.asStateFlow()
    
    private val _schedulesState = MutableStateFlow<UiState<List<Schedule>>>(UiState.Idle)
    val schedulesState: StateFlow<UiState<List<Schedule>>> = _schedulesState.asStateFlow()
    
    private val _scheduleOperationState = MutableStateFlow<UiState<Schedule>>(UiState.Idle)
    val scheduleOperationState: StateFlow<UiState<Schedule>> = _scheduleOperationState.asStateFlow()
    
    private val _nlpCommandState = MutableStateFlow<UiState<NlpCommand>>(UiState.Idle)
    val nlpCommandState: StateFlow<UiState<NlpCommand>> = _nlpCommandState.asStateFlow()
    
    init {
        loadScenes()
        loadRooms()
        loadSchedules()
        connectWebSocket()
    }
    
    /**
     * Connect to WebSocket and listen for real-time updates
     */
    private fun connectWebSocket() {
        viewModelScope.launch {
            webSocketService.connect()
            
            // Listen for WebSocket events
            webSocketService.events.collect { event ->
                Log.d(TAG, "WebSocket event received: $event")
                when (event) {
                    is WebSocketEvent.ScheduleCreated,
                    is WebSocketEvent.ScheduleUpdated,
                    is WebSocketEvent.ScheduleDeleted,
                    is WebSocketEvent.ScheduleToggled,
                    is WebSocketEvent.ScheduleTriggered -> {
                        Log.d(TAG, "Schedule changed, reloading schedules")
                        loadSchedules()
                    }
                    is WebSocketEvent.SceneCreated -> {
                        Log.d(TAG, "Scene created: ${event.sceneName}, reloading scenes")
                        loadScenes()
                    }
                    is WebSocketEvent.SceneUpdated -> {
                        Log.d(TAG, "Scene updated: ${event.sceneName}, reloading scenes")
                        loadScenes()
                    }
                    is WebSocketEvent.SceneDeleted -> {
                        Log.d(TAG, "Scene deleted: ${event.sceneId}, reloading scenes")
                        loadScenes()
                    }
                    is WebSocketEvent.SceneApplied,
                    is WebSocketEvent.SceneConfirmed -> {
                        Log.d(TAG, "Scene event, reloading scenes")
                        loadScenes()
                    }
                    is WebSocketEvent.DeviceStateChange -> {
                        // Could update device state locally if needed
                        Log.d(TAG, "Device state changed: ${event.deviceId}")
                    }
                    is WebSocketEvent.Connected -> {
                        Log.i(TAG, "WebSocket connected - refreshing data")
                        loadSchedules()
                        loadScenes()
                    }
                    is WebSocketEvent.Disconnected -> {
                        Log.w(TAG, "WebSocket disconnected")
                    }
                    is WebSocketEvent.Error -> {
                        Log.e(TAG, "WebSocket error: ${event.message}")
                    }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketService.disconnect()
    }
    
    fun loadScenes() {
        viewModelScope.launch {
            _scenesState.value = UiState.Loading
            val result = repository.getScenes()
            _scenesState.value = if (result.isSuccess) {
                UiState.Success(result.getOrNull() ?: emptyList())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to load scenes")
            }
        }
    }
    
    fun loadRooms() {
        viewModelScope.launch {
            _roomsState.value = UiState.Loading
            val result = repository.getRooms()
            _roomsState.value = if (result.isSuccess) {
                UiState.Success(result.getOrNull() ?: emptyList())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to load rooms")
            }
        }
    }
    
    fun activateScene(sceneId: String) {
        viewModelScope.launch {
            _activateSceneState.value = UiState.Loading
            val result = repository.activateScene(sceneId)
            _activateSceneState.value = when {
                result.isSuccess -> {
                    UiState.Success(result.getOrNull() ?: emptyMap())
                }
                else -> {
                    UiState.Error(result.exceptionOrNull()?.message ?: "Failed to activate scene")
                }
            }
        }
    }
    
    fun sendVoiceCommand(command: String) {
        if (command.isBlank()) {
            _commandState.value = UiState.Error("Please enter a command")
            return
        }
        
        viewModelScope.launch {
            _commandState.value = UiState.Loading
            
            val result = repository.sendNaturalLanguageCommand(command, null)
            
            _commandState.value = when {
                result.isSuccess -> {
                    val response = result.getOrNull()
                    if (response != null) {
                        val successValue = response["success"]
                        val isSuccess = when (successValue) {
                            is Boolean -> successValue
                            is String -> successValue.equals("true", ignoreCase = true)
                            is Number -> successValue.toInt() != 0
                            else -> false
                        }
                        
                        val message = if (isSuccess) {
                            "Command executed successfully"
                        } else {
                            response["error"]?.toString() ?: "Command executed"
                        }
                        
                        loadScenes()
                        
                        UiState.Success(CommandResult(success = isSuccess, message = message))
                    } else {
                        UiState.Error("No response received from server")
                    }
                }
                else -> {
                    UiState.Error(result.exceptionOrNull()?.message ?: "Failed to execute command")
                }
            }
        }
    }
    
    fun sendManualCommand(roomId: String?, color: Int?, brightness: Int?) {
        viewModelScope.launch {
            _commandState.value = UiState.Loading
            
            val command = buildString {
                append("Create a scene: ")
                if (roomId != null) {
                    val roomName = (_roomsState.value as? UiState.Success)
                        ?.data?.find { it.id == roomId }?.name ?: "lights"
                    append("Set $roomName ")
                } else {
                    append("Set all lights ")
                }
                
                if (color != null) {
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    append("to color RGB($r,$g,$b) ")
                }
                
                if (brightness != null) {
                    append("at $brightness% brightness")
                }
            }
            
            val result = repository.sendNaturalLanguageCommand(command, null)
            
            _commandState.value = when {
                result.isSuccess -> {
                    loadScenes()
                    UiState.Success(CommandResult(success = true, message = "Scene created and applied successfully"))
                }
                else -> {
                    UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create scene")
                }
            }
        }
    }
    
    fun resetCommandState() {
        _commandState.value = UiState.Idle
    }
    
    fun resetActivateSceneState() {
        _activateSceneState.value = UiState.Idle
    }
    
    fun resetSceneOperationState() {
        _sceneOperationState.value = UiState.Idle
    }
    
    fun createScene(scene: Scene) {
        viewModelScope.launch {
            _sceneOperationState.value = UiState.Loading
            val result = repository.createScene(scene)
            _sceneOperationState.value = if (result.isSuccess) {
                loadScenes()
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create scene")
            }
        }
    }

    fun updateScene(sceneId: String, scene: Scene) {
        viewModelScope.launch {
            _sceneOperationState.value = UiState.Loading
            val result = repository.updateScene(sceneId, scene)
            _sceneOperationState.value = if (result.isSuccess) {
                loadScenes()
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update scene")
            }
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            _sceneOperationState.value = UiState.Loading
            val result = repository.deleteScene(sceneId)
            _sceneOperationState.value = if (result.isSuccess) {
                loadScenes()
                UiState.Success(Scene(id = sceneId, name = ""))
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete scene")
            }
        }
    }
    
    // Schedules methods
    fun loadSchedules() {
        viewModelScope.launch {
            _schedulesState.value = UiState.Loading
            val result = repository.getSchedules()
            _schedulesState.value = if (result.isSuccess) {
                UiState.Success(result.getOrNull() ?: emptyList())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to load schedules")
            }
        }
    }
    
    fun createSchedule(schedule: Schedule) {
        viewModelScope.launch {
            _scheduleOperationState.value = UiState.Loading
            val result = repository.createSchedule(schedule)
            _scheduleOperationState.value = if (result.isSuccess) {
                loadSchedules()
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create schedule")
            }
        }
    }

    fun updateSchedule(scheduleId: String, schedule: Schedule) {
        viewModelScope.launch {
            _scheduleOperationState.value = UiState.Loading
            val result = repository.updateSchedule(scheduleId, schedule)
            _scheduleOperationState.value = if (result.isSuccess) {
                loadSchedules()
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update schedule")
            }
        }
    }

    fun toggleSchedule(scheduleId: String) {
        viewModelScope.launch {
            _scheduleOperationState.value = UiState.Loading
            val result = repository.toggleSchedule(scheduleId)
            _scheduleOperationState.value = if (result.isSuccess) {
                loadSchedules()  // Reload schedules after toggle
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to toggle schedule")
            }
        }
    }
    
    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            _scheduleOperationState.value = UiState.Loading
            val result = repository.deleteSchedule(scheduleId)
            _scheduleOperationState.value = if (result.isSuccess) {
                loadSchedules()  // Reload schedules after deletion
                UiState.Success(Schedule(id = scheduleId, name = "", triggerType = ""))
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete schedule")
            }
        }
    }
    
    fun parseNlpCommand(text: String) {
        viewModelScope.launch {
            _nlpCommandState.value = UiState.Loading
            val result = repository.parseNlpCommand(text)
            _nlpCommandState.value = if (result.isSuccess) {
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to parse command")
            }
        }
    }
    
    fun confirmNlpCommand(command: NlpCommand) {
        viewModelScope.launch {
            _nlpCommandState.value = UiState.Loading
            val result = repository.confirmNlpCommand(command)
            _nlpCommandState.value = if (result.isSuccess) {
                loadSchedules()  // Reload schedules after creating
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to execute command")
            }
        }
    }
    
    fun resetScheduleOperationState() {
        _scheduleOperationState.value = UiState.Idle
    }
    
    fun resetNlpCommandState() {
        _nlpCommandState.value = UiState.Idle
    }
}
