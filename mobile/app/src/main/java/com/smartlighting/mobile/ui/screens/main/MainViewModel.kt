package com.smartlighting.mobile.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.mobile.data.model.CommandResult
import com.smartlighting.mobile.data.model.Room
import com.smartlighting.mobile.data.model.Scene
import com.smartlighting.mobile.data.repository.LightingRepository
import com.smartlighting.mobile.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: LightingRepository
) : ViewModel() {
    
    private val _scenesState = MutableStateFlow<UiState<List<Scene>>>(UiState.Idle)
    val scenesState: StateFlow<UiState<List<Scene>>> = _scenesState.asStateFlow()
    
    private val _roomsState = MutableStateFlow<UiState<List<Room>>>(UiState.Idle)
    val roomsState: StateFlow<UiState<List<Room>>> = _roomsState.asStateFlow()
    
    private val _commandState = MutableStateFlow<UiState<CommandResult>>(UiState.Idle)
    val commandState: StateFlow<UiState<CommandResult>> = _commandState.asStateFlow()
    
    private val _activateSceneState = MutableStateFlow<UiState<Map<String, Any>>>(UiState.Idle)
    val activateSceneState: StateFlow<UiState<Map<String, Any>>> = _activateSceneState.asStateFlow()
    
    init {
        loadScenes()
        loadRooms()
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
}
