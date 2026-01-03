package com.smartlighting.mobile.ui.screens.main.tabs

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartlighting.mobile.data.model.NlpCommand
import com.smartlighting.mobile.data.model.Scene
import com.smartlighting.mobile.data.model.SceneSettings
import com.smartlighting.mobile.ui.screens.main.MainViewModel
import com.smartlighting.mobile.util.UiState

@Composable
fun ScenesTab(
    viewModel: MainViewModel
) {
    val scenesState by viewModel.scenesState.collectAsState()
    val nlpCommandState by viewModel.nlpCommandState.collectAsState()
    val activateState by viewModel.activateSceneState.collectAsState()
    val sceneOperationState by viewModel.sceneOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var nlpCommand by remember { mutableStateOf("") }
    var parsedCommand by remember { mutableStateOf<NlpCommand?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingScene by remember { mutableStateOf<Scene?>(null) }
    
    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            nlpCommand = matches[0]
            viewModel.parseNlpCommand(nlpCommand)
        }
        isListening = false
    }
    
    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition(voiceLauncher) { isListening = true }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadScenes()
    }
    
    LaunchedEffect(nlpCommandState) {
        when (val state = nlpCommandState) {
            is UiState.Success -> {
                val command = state.data
                if (command.valid && command.executed == true) {
                    snackbarHostState.showSnackbar(
                        message = "Scene command executed successfully",
                        duration = SnackbarDuration.Short
                    )
                    nlpCommand = ""
                    parsedCommand = null
                    viewModel.loadScenes()
                    viewModel.resetNlpCommandState()
                } else if (command.valid && command.executed == false && !command.result.isNullOrEmpty() && command.result.contains("Error", ignoreCase = true)) {
                    // Handle execution errors
                    snackbarHostState.showSnackbar(
                        message = command.result,
                        duration = SnackbarDuration.Long
                    )
                    nlpCommand = ""
                    parsedCommand = null
                    viewModel.resetNlpCommandState()
                } else if (command.valid) {
                    parsedCommand = command
                    viewModel.resetNlpCommandState()
                } else if (!command.valid && command.error != null) {
                    // Handle invalid commands - show error message
                    val errorMessage = when {
                        command.error.contains("OpenAI API key") -> 
                            "NLP feature unavailable: AI service not configured. Please use preset scenes or manual controls."
                        command.error.contains("timed out") -> 
                            "Request timeout. Please try again."
                        command.error.contains("rate limit") -> 
                            "Too many requests. Please wait a moment."
                        else -> "Failed to parse command: ${command.error}"
                    }
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Long
                    )
                    viewModel.resetNlpCommandState()
                }
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetNlpCommandState()
            }
            else -> {}
        }
    }
    
    LaunchedEffect(activateState) {
        when (activateState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Scene activated successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetActivateSceneState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (activateState as UiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetActivateSceneState()
            }
            else -> {}
        }
    }
    
    LaunchedEffect(sceneOperationState) {
        when (sceneOperationState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Scene operation completed successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSceneOperationState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (sceneOperationState as UiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSceneOperationState()
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color.Black
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // NLP Command Input - Minimal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nlpCommand,
                    onValueChange = { nlpCommand = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Try: 'Turn on movie mode'", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isListening) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    },
                    trailingIcon = {
                        if (nlpCommand.isNotEmpty()) {
                            IconButton(onClick = { nlpCommand = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                )
                
                FloatingActionButton(
                    onClick = { 
                        if (nlpCommand.isNotEmpty()) {
                            viewModel.parseNlpCommand(nlpCommand)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    if (nlpCommandState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Process Command")
                    }
                }
            }
            
            // Show parsed command preview - compact
            parsedCommand?.let { command ->
                if (command.valid) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    command.preview ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { 
                                        viewModel.confirmNlpCommand(command)
                                        parsedCommand = null
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Check, 
                                        contentDescription = "Confirm",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { parsedCommand = null }) {
                                    Icon(
                                        Icons.Default.Close, 
                                        contentDescription = "Cancel",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Scenes List
            when (val state = scenesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    val scenes = state.data
                    if (scenes.isEmpty()) {
                        EmptySceneState()
                    } else {
                        val presetScenes = scenes.filter { it.isGlobal == true || it.name in listOf("Reading", "Movie", "Nightlight", "Relax", "Bright", "Party", "Cozy") }
                        val customScenes = scenes.filter { it.isGlobal != true && it.name !in listOf("Reading", "Movie", "Nightlight", "Relax", "Bright", "Party", "Cozy") }
                        
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (presetScenes.isNotEmpty()) {
                                items(presetScenes.chunked(2)) { rowScenes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowScenes.forEach { scene ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                PresetSceneCard(
                                                    scene = scene,
                                                    onApply = { viewModel.activateScene(scene.id ?: "") },
                                                    isLoading = activateState is UiState.Loading
                                                )
                                            }
                                        }
                                        if (rowScenes.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            
                            if (customScenes.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(customScenes.chunked(2)) { rowScenes ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowScenes.forEach { scene ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                // Only show edit/delete for non-preset scenes
                                                val isPreset = scene.isPreset == true || scene.isGlobal == true
                                                CustomSceneCard(
                                                    scene = scene,
                                                    onApply = { viewModel.activateScene(scene.id ?: "") },
                                                    onEdit = if (!isPreset) { { editingScene = scene } } else null,
                                                    onDelete = if (!isPreset) { { viewModel.deleteScene(scene.id ?: "") } } else null,
                                                    isLoading = activateState is UiState.Loading
                                                )
                                            }
                                        }
                                        if (rowScenes.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadScenes() }
                    )
                }
                else -> {}
            }
        }
        
            // Beautiful FAB for creating new scene
            FloatingActionButton(
                onClick = {
                    editingScene = null
                    showCreateDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    hoveredElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Create Scene",
                    modifier = Modifier.size(28.dp)
                )
            }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
    
    // Scene Create/Edit Dialog
    if (showCreateDialog || editingScene != null) {
        SceneCreateEditDialog(
            scene = editingScene,
            onDismiss = {
                showCreateDialog = false
                editingScene = null
            },
            onSave = { scene ->
                if (editingScene != null) {
                    viewModel.updateScene(editingScene!!.id ?: "", scene)
                } else {
                    viewModel.createScene(scene)
                }
                showCreateDialog = false
                editingScene = null
            }
        )
    }
}

@Composable
private fun PresetSceneCard(
    scene: Scene,
    onApply: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(enabled = !isLoading, onClick = onApply),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val icon = scene.icon ?: getSceneIcon(scene.name)
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = Color.White
                )
                
                scene.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSceneCard(
    scene: Scene,
    onApply: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(enabled = !isLoading, onClick = onApply),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
        ) {
            // Edit and Delete buttons at top right (only for custom scenes)
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            enabled = !isLoading,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Scene",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            enabled = !isLoading,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Scene",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Content centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val icon = scene.icon ?: getSceneIcon(scene.name)
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = Color.White
                )
                
                scene.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySceneState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displayLarge
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Scenes Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first lighting scene using\nnatural language commands above",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Retry")
        }
    }
}

private fun getSceneIcon(name: String): String {
    return when (name.lowercase()) {
        "reading", "read" -> "📖"
        "movie", "cinema" -> "🎬"
        "night", "nightlight", "sleep" -> "🌙"
        "party" -> "🎉"
        "work", "focus" -> "💼"
        "relax", "chill" -> "🧘"
        "romantic" -> "💕"
        "bright" -> "☀️"
        "cozy" -> "🕯️"
        else -> "💡"
    }
}

private fun startVoiceRecognition(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStart: () -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your lighting command...")
    }
    onStart()
    launcher.launch(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneCreateEditDialog(
    scene: Scene?,
    onDismiss: () -> Unit,
    onSave: (Scene) -> Unit
) {
    var name by remember { mutableStateOf(scene?.name ?: "") }
    var description by remember { mutableStateOf(scene?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(scene?.icon ?: getSceneIcon(scene?.name ?: "")) }
    var target by remember { mutableStateOf(scene?.settings?.target ?: "all") }
    var brightness by remember { mutableStateOf(scene?.settings?.brightness?.toFloat() ?: 80f) }
    var selectedColor by remember {
        val rgb = scene?.settings?.rgb
        if (rgb != null && rgb.size == 3) {
            mutableStateOf(Color(rgb[0], rgb[1], rgb[2]))
        } else {
            mutableStateOf(Color(0xFFFFFFFF))
        }
    }
    var colorTemp by remember { mutableStateOf(scene?.settings?.colorTemp ?: 4000) }
    
    val icons = listOf("💡", "🎬", "📖", "🌙", "🎉", "💼", "🧘", "💕", "☀️", "🕯️", "🌅", "✨")
    val targets = listOf(
        "all" to "All Lights",
        "bedroom" to "Bedroom",
        "living_room" to "Living Room",
        "kitchen" to "Kitchen",
        "bathroom" to "Bathroom",
        "hallway" to "Hallway"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (scene != null) "Edit Scene" else "Create Scene",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                item {
                    Column {
                        Text(
                            "Scene Name",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Cozy Evening") },
                            singleLine = true
                        )
                    }
                }
                
                // Icon Picker
                item {
                    Column {
                        Text(
                            "Icon",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            icons.chunked(6).forEach { rowIcons ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowIcons.forEach { icon ->
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (selectedIcon == icon)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { selectedIcon = icon },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = icon,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Description
                item {
                    Column {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Brief description") },
                            maxLines = 2
                        )
                    }
                }
                
                // Target/Room
                item {
                    Column {
                        Text(
                            "Apply To",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = targets.find { it.first == target }?.second ?: "All Lights",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                targets.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            target = value
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Brightness
                item {
                    Column {
                        Text(
                            "Brightness: ${brightness.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Color Picker
                item {
                    ColorPickerCanvas(
                        initialColor = selectedColor,
                        onColorChange = { selectedColor = it }
                    )
                }
                
                // Color Temperature
                item {
                    Column {
                        Text(
                            "Color Temperature: ${colorTemp}K",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = colorTemp.toFloat(),
                            onValueChange = { colorTemp = it.toInt() },
                            valueRange = 2700f..6500f,
                            steps = 37,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Warm", style = MaterialTheme.typography.bodySmall)
                            Text("Neutral", style = MaterialTheme.typography.bodySmall)
                            Text("Cool", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                // Preview
                item {
                    Column {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    selectedColor.copy(alpha = brightness / 100f)
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Convert color to RGB
                    val rgb = listOf(
                        (selectedColor.red * 255).toInt(),
                        (selectedColor.green * 255).toInt(),
                        (selectedColor.blue * 255).toInt()
                    )
                    
                    val newScene = Scene(
                        id = scene?.id,
                        name = name,
                        description = description.ifBlank { null },
                        icon = selectedIcon,
                        settings = SceneSettings(
                            brightness = brightness.toInt(),
                            rgb = rgb,
                            colorTemp = colorTemp,
                            target = target
                        ),
                        isGlobal = false,
                        isPreset = false
                    )
                    onSave(newScene)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (scene != null) "Update" else "Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorPickerCanvas(
    initialColor: Color,
    onColorChange: (Color) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    
    // Convert initial color to HSB
    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        val argb = initialColor.toArgb()
        android.graphics.Color.colorToHSV(argb, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }
    
    // Update color when HSB changes
    LaunchedEffect(hue, saturation, brightness) {
        val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
        onColorChange(color)
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pick a Color",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            // Color preview
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Hue Bar
        Text(
            "Hue: ${hue.toInt()}°",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width) * 360f
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width) * 360f
                    }
                }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Saturation/Brightness Canvas
        Text(
            "Tap to adjust saturation & brightness",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))
                .drawBehind {
                    // White to transparent (saturation gradient)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White, Color.Transparent)
                        )
                    )
                    // Transparent to black (brightness gradient)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        saturation = 1f - (x / size.width)
                        brightness = 1f - (y / size.height)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        val y = offset.y.coerceIn(0f, size.height.toFloat())
                        saturation = 1f - (x / size.width)
                        brightness = 1f - (y / size.height)
                    }
                }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // RGB Values display
        val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "R: ${(currentColor.red * 255).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "G: ${(currentColor.green * 255).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "B: ${(currentColor.blue * 255).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
