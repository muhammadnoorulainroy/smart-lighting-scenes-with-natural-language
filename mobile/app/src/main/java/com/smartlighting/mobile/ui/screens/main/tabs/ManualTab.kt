package com.smartlighting.mobile.ui.screens.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartlighting.mobile.data.model.Room
import com.smartlighting.mobile.ui.screens.main.MainViewModel
import com.smartlighting.mobile.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTab(
    viewModel: MainViewModel
) {
    val roomsState by viewModel.roomsState.collectAsState()
    val commandState by viewModel.commandState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val rooms = remember(roomsState) {
        when (val state = roomsState) {
            is UiState.Success -> state.data
            else -> emptyList()
        }
    }
    
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var brightness by remember { mutableStateOf(70f) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    
    val colorPalette = remember {
        listOf(
            // Reds & Oranges
            Color(0xFFFF0000), Color(0xFFFF4500), Color(0xFFFF6347), Color(0xFFFF7F50),
            // Yellows & Golds
            Color(0xFFFFFF00), Color(0xFFFFD700), Color(0xFFFFDAB9), Color(0xFFFFE4B5),
            // Greens
            Color(0xFF00FF00), Color(0xFF32CD32), Color(0xFF00FA9A), Color(0xFF90EE90),
            // Cyans & Blues
            Color(0xFF00FFFF), Color(0xFF00BFFF), Color(0xFF1E90FF), Color(0xFF0000FF),
            // Purples & Pinks
            Color(0xFF9370DB), Color(0xFF8B00FF), Color(0xFFFF1493), Color(0xFFFF69B4),
            // Whites & Neutrals
            Color(0xFFFFFFFF), Color(0xFFF5F5DC), Color(0xFFD3D3D3), Color(0xFF808080)
        )
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadRooms()
    }
    
    LaunchedEffect(commandState) {
        when (commandState) {
            is UiState.Success -> {
                val result = (commandState as UiState.Success).data
                snackbarHostState.showSnackbar(
                    message = result.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetCommandState()
                viewModel.loadScenes()
            }
            is UiState.Error -> {
                val error = (commandState as UiState.Error).message
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetCommandState()
            }
            else -> {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Room Selection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        
                        if (rooms.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = rooms.find { it.id == selectedRoomId }?.name ?: "Select Room",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    rooms.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text(room.name) },
                                            onClick = {
                                                selectedRoomId = room.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = "Color Selection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            selectedColor ?: MaterialTheme.colorScheme.surfaceVariant,
                                            (selectedColor ?: MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == null) {
                                Text(
                                    text = "Pick a color below",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            colorPalette.chunked(4).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowColors.forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .clickable { selectedColor = color }
                                                .border(
                                                    width = if (selectedColor == color) 4.dp else 1.dp,
                                                    color = if (selectedColor == color) 
                                                        MaterialTheme.colorScheme.primary 
                                                    else 
                                                        Color.Gray.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedColor == color) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (color == Color.White || color.red > 0.8f && color.green > 0.8f) 
                                                        Color.Black 
                                                    else 
                                                        Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Brightness: ${brightness.toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0f..100f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
            
            Button(
                onClick = {
                    val colorInt = selectedColor?.let {
                        val red = (it.red * 255).toInt()
                        val green = (it.green * 255).toInt()
                        val blue = (it.blue * 255).toInt()
                        (red shl 16) or (green shl 8) or blue
                    }
                    viewModel.sendManualCommand(
                        roomId = selectedRoomId,
                        color = colorInt,
                        brightness = brightness.toInt()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = commandState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                if (commandState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        "Create & Apply Scene",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
