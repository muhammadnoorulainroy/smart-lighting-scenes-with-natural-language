package com.smartlighting.mobile.ui.screens.main.tabs

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartlighting.mobile.data.model.NlpCommand
import com.smartlighting.mobile.data.model.Schedule
import com.smartlighting.mobile.ui.screens.main.MainViewModel
import com.smartlighting.mobile.util.UiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SchedulesTab(
    viewModel: MainViewModel
) {
    val schedulesState by viewModel.schedulesState.collectAsState()
    val nlpCommandState by viewModel.nlpCommandState.collectAsState()
    val scheduleOperationState by viewModel.scheduleOperationState.collectAsState()
    val scenesState by viewModel.scenesState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var nlpCommand by remember { mutableStateOf("") }
    var parsedCommand by remember { mutableStateOf<NlpCommand?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
    }
    
    LaunchedEffect(nlpCommandState) {
        when (val state = nlpCommandState) {
            is UiState.Success -> {
                val command = state.data
                if (command.valid && command.executed == true) {
                    snackbarHostState.showSnackbar(
                        message = "Schedule created successfully",
                        duration = SnackbarDuration.Short
                    )
                    nlpCommand = ""
                    parsedCommand = null
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
                            "NLP feature unavailable: AI service not configured. Please use manual schedule creation."
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
    
    LaunchedEffect(scheduleOperationState) {
        when (scheduleOperationState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Operation completed successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetScheduleOperationState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (scheduleOperationState as UiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetScheduleOperationState()
            }
            else -> {}
        }
    }
    
    // Schedule Create/Edit Dialog
    if (showCreateDialog || editingSchedule != null) {
        ScheduleCreateEditDialog(
            schedule = editingSchedule,
            availableScenes = (scenesState as? UiState.Success)?.data ?: emptyList(),
            onDismiss = {
                showCreateDialog = false
                editingSchedule = null
            },
            onSave = { schedule ->
                if (editingSchedule != null) {
                    viewModel.updateSchedule(editingSchedule!!.id ?: "", schedule)
                } else {
                    viewModel.createSchedule(schedule)
                }
                showCreateDialog = false
                editingSchedule = null
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
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
                    placeholder = { Text("Try: 'Turn off lights at 11pm'", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
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
            
            // Schedules List
            when (val state = schedulesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    val schedules = state.data
                    if (schedules.isEmpty()) {
                        EmptyScheduleState()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(schedules) { schedule ->
                                ScheduleCard(
                                    schedule = schedule,
                                    onToggle = { viewModel.toggleSchedule(schedule.id ?: "") },
                                    onEdit = { editingSchedule = schedule },
                                    onDelete = { viewModel.deleteSchedule(schedule.id ?: "") }
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadSchedules() }
                    )
                }
                else -> {}
            }
        }
        
        // Beautiful FAB for creating new schedule
        FloatingActionButton(
            onClick = {
                editingSchedule = null
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
                hoveredElevation = 8.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "Create Schedule",
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
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onToggle: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Schedule?") },
            text = { Text("Are you sure you want to delete \"${schedule.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = if (schedule.enabled) 0.7f else 0.4f),
                            Color.Black
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (schedule.triggerType) {
                            "time" -> "⏰"
                            "sun" -> "☀️"
                            "sensor" -> "📡"
                            else -> "⏰"
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column {
                            Text(
                                text = schedule.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (schedule.enabled) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.6f)
                                }
                            )
                            schedule.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = schedule.enabled,
                            onCheckedChange = { onToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                        if (onEdit != null) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Trigger info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTrigger(schedule),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Actions info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatActions(schedule),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Stats
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Triggered ${schedule.triggerCount ?: 0} times",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleState() {
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
                text = "⏰",
                style = MaterialTheme.typography.displayLarge
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Schedules Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first lighting schedule using\nnatural language commands above",
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

@Composable
private fun CreateScheduleDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Text(
                    "Create Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Use the text field above to create schedules with natural language",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Examples:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Turn off all lights at 11pm every day\n" +
                            "• Set bedroom lights to 50% at 7am on weekdays\n" +
                            "• Turn on living room lights at sunset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatTrigger(schedule: Schedule): String {
    val config = schedule.triggerConfig ?: return "Unknown"
    
    return when (schedule.triggerType) {
        "time" -> {
            val time = config["at"]?.toString()?.substring(0, 5) ?: config["time"]?.toString() ?: "Unknown"
            val weekdays = config["weekdays"] as? List<*>
            
            when {
                weekdays == null || weekdays.size == 7 -> "Every day at $time"
                weekdays.size == 5 && !weekdays.contains("sat") && !weekdays.contains("sun") -> 
                    "Weekdays at $time"
                weekdays.size == 2 && weekdays.contains("sat") && weekdays.contains("sun") -> 
                    "Weekends at $time"
                else -> "${weekdays.joinToString(", ")} at $time"
            }
        }
        "sun" -> {
            val event = config["event"]?.toString() ?: "sunrise"
            val offset = (config["offset_minutes"] as? Number)?.toInt() ?: 0
            
            if (offset == 0) {
                "At $event"
            } else {
                val absOffset = kotlin.math.abs(offset)
                val direction = if (offset > 0) "after" else "before"
                "$absOffset min $direction $event"
            }
        }
        else -> "Unknown"
    }
}

private fun formatActions(schedule: Schedule): String {
    val actions = schedule.actions ?: return "No actions"
    if (actions.isEmpty()) return "No actions"
    
    val action = actions[0]
    
    return when {
        action.type == "scene" -> "Apply scene: ${action.scene ?: action.sceneId}"
        action.intent == "light.off" -> "Turn off ${action.target ?: "all"} lights"
        action.intent == "light.on" -> "Turn on ${action.target ?: "all"} lights"
        action.intent == "light.brightness" -> {
            val brightness = action.params?.get("brightness")
            "Set ${action.target ?: "all"} to $brightness%"
        }
        else -> action.intent ?: "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCreateEditDialog(
    schedule: Schedule?,
    availableScenes: List<com.smartlighting.mobile.data.model.Scene>,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var name by remember { mutableStateOf(schedule?.name ?: "") }
    var description by remember { mutableStateOf(schedule?.description ?: "") }
    var triggerType by remember { mutableStateOf(schedule?.triggerType ?: "time") }
    
    // Time trigger config
    var time by remember { mutableStateOf("07:00") }
    var selectedDays by remember { mutableStateOf(listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) }
    
    // Sun trigger config
    var sunEvent by remember { mutableStateOf("sunset") }
    var offsetMinutes by remember { mutableStateOf(0) }
    
    // Action config
    var actionType by remember { mutableStateOf("light.on") }
    var target by remember { mutableStateOf("all") }
    var brightness by remember { mutableStateOf(50f) }
    var selectedSceneId by remember { mutableStateOf<String?>(null) }
    
    // Initialize from schedule if editing
    LaunchedEffect(schedule) {
        schedule?.let { sch ->
            name = sch.name
            description = sch.description ?: ""
            triggerType = sch.triggerType
            
            val triggerConfig = sch.triggerConfig ?: emptyMap()
            if (triggerType == "time") {
                time = triggerConfig["time"]?.toString()?.substring(0, 5) ?: "07:00"
                selectedDays = (triggerConfig["days"] as? List<*>)?.mapNotNull { it.toString().uppercase() } 
                    ?: listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            } else if (triggerType == "sun") {
                sunEvent = triggerConfig["event"]?.toString() ?: "sunset"
                offsetMinutes = (triggerConfig["offsetMinutes"] as? Number)?.toInt() ?: 0
            }
            
            val action = sch.actions?.firstOrNull()
            if (action != null) {
                actionType = action.intent ?: if (action.type == "scene") "scene.apply" else "light.on"
                target = action.target ?: "all"
                brightness = (action.params?.get("brightness") as? Number)?.toFloat() ?: 50f
                selectedSceneId = action.sceneId ?: action.scene
            }
        }
    }
    
    val days = listOf(
        "MONDAY" to "Mon",
        "TUESDAY" to "Tue",
        "WEDNESDAY" to "Wed",
        "THURSDAY" to "Thu",
        "FRIDAY" to "Fri",
        "SATURDAY" to "Sat",
        "SUNDAY" to "Sun"
    )
    
    val targets = listOf(
        "all" to "All Lights",
        "bedroom" to "Bedroom",
        "living_room" to "Living Room",
        "kitchen" to "Kitchen",
        "bathroom" to "Bathroom",
        "hallway" to "Hallway"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                item {
                    Text(
                        text = if (schedule != null) "Edit Schedule" else "Create Schedule",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Name
                item {
                    Column {
                        Text(
                            "Schedule Name",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Morning Lights") },
                            singleLine = true
                        )
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
                
                // Trigger Type
                item {
                    Column {
                        Text(
                            "Trigger Type",
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
                                value = if (triggerType == "time") "Time-based" else "Sun event",
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
                                DropdownMenuItem(
                                    text = { Text("Time-based") },
                                    onClick = {
                                        triggerType = "time"
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sun event") },
                                    onClick = {
                                        triggerType = "sun"
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Time Trigger Config
                if (triggerType == "time") {
                    item {
                        Column {
                            Text(
                                "Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("HH:MM") },
                                singleLine = true
                            )
                        }
                    }
                    
                    item {
                        Column {
                            Text(
                                "Days",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                days.chunked(4).forEach { rowDays ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowDays.forEach { (dayValue, dayLabel) ->
                                            FilterChip(
                                                selected = selectedDays.contains(dayValue),
                                                onClick = {
                                                    selectedDays = if (selectedDays.contains(dayValue)) {
                                                        selectedDays - dayValue
                                                    } else {
                                                        selectedDays + dayValue
                                                    }
                                                },
                                                label = { Text(dayLabel) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // Fill remaining space if less than 4 chips in row
                                        repeat(4 - rowDays.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Sun Trigger Config
                if (triggerType == "sun") {
                    item {
                        Column {
                            Text(
                                "Sun Event",
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
                                    value = sunEvent.capitalize(),
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
                                    listOf("sunrise", "sunset").forEach { event ->
                                        DropdownMenuItem(
                                            text = { Text(event.capitalize()) },
                                            onClick = {
                                                sunEvent = event
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Column {
                            Text(
                                "Offset (minutes)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = offsetMinutes.toString(),
                                onValueChange = { offsetMinutes = it.toIntOrNull() ?: 0 },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g., -30 for 30 min before") },
                                singleLine = true
                            )
                        }
                    }
                }
                
                // Divider
                item {
                    HorizontalDivider()
                }
                
                // Action Section Title
                item {
                    Text(
                        "Action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Action Type
                item {
                    Column {
                        Text(
                            "Action Type",
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
                                value = when (actionType) {
                                    "light.on" -> "Turn On"
                                    "light.off" -> "Turn Off"
                                    "light.brightness" -> "Set Brightness"
                                    "scene.apply" -> "Apply Scene"
                                    else -> actionType
                                },
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
                                listOf(
                                    "light.on" to "Turn On",
                                    "light.off" to "Turn Off",
                                    "light.brightness" to "Set Brightness",
                                    "scene.apply" to "Apply Scene"
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            actionType = value
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Target (for non-scene actions)
                if (actionType != "scene.apply") {
                    item {
                        Column {
                            Text(
                                "Target",
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
                }
                
                // Brightness (for brightness action)
                if (actionType == "light.brightness") {
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
                }
                
                // Scene Selection (for scene action)
                if (actionType == "scene.apply") {
                    item {
                        Column {
                            Text(
                                "Scene",
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
                                    value = availableScenes.find { it.id == selectedSceneId }?.name ?: "Select Scene",
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
                                    availableScenes.forEach { scene ->
                                        DropdownMenuItem(
                                            text = { Text(scene.name) },
                                            onClick = {
                                                selectedSceneId = scene.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                // Build trigger config
                                val triggerConfig = if (triggerType == "time") {
                                    mapOf(
                                        "time" to "$time:00",
                                        "days" to selectedDays
                                    )
                                } else {
                                    mapOf(
                                        "event" to sunEvent,
                                        "offsetMinutes" to offsetMinutes
                                    )
                                }
                                
                                // Build action
                                val action = if (actionType == "scene.apply") {
                                    com.smartlighting.mobile.data.model.ScheduleAction(
                                        type = "scene",
                                        intent = null,
                                        target = null,
                                        sceneId = selectedSceneId,
                                        scene = selectedSceneId,
                                        params = null
                                    )
                                } else {
                                    com.smartlighting.mobile.data.model.ScheduleAction(
                                        type = "light",
                                        intent = actionType,
                                        target = target,
                                        sceneId = null,
                                        scene = null,
                                        params = if (actionType == "light.brightness") {
                                            mapOf("brightness" to brightness.toInt())
                                        } else null
                                    )
                                }
                                
                                val newSchedule = Schedule(
                                    id = schedule?.id,
                                    name = name,
                                    description = description.ifBlank { null },
                                    triggerType = triggerType,
                                    triggerConfig = triggerConfig,
                                    actionType = if (actionType == "scene.apply") "scene" else "device",
                                    actionConfig = null,
                                    actions = listOf(action),
                                    enabled = schedule?.enabled ?: true
                                )
                                
                                onSave(newSchedule)
                            },
                            enabled = name.isNotBlank() && 
                                     (actionType != "scene.apply" || selectedSceneId != null),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (schedule != null) "Update" else "Create")
                        }
                    }
                }
            }
        }
    }
}

