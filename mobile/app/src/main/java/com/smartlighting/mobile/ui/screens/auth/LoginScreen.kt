package com.smartlighting.mobile.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.mobile.ui.components.LoadingIndicator
import com.smartlighting.mobile.util.UiState

/**
 * Login screen with Google Sign-In and Email/Password authentication
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val googleClientId by viewModel.googleClientId.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (val state = authState) {
            is UiState.Success -> {
                errorMessage = null
                onLoginSuccess()
                viewModel.resetAuthState()
            }
            is UiState.Error -> {
                errorMessage = state.message
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (authState is UiState.Loading) {
                LoadingIndicator(message = if (isSignUpMode) "Creating account..." else "Signing in...")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo/Icon
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    Text(
                        text = "Smart Lighting",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Subtitle
                    Text(
                        text = "Control your lights with natural language",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Mode toggle
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Error message display
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Name field (only for sign up)
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                errorMessage = null
                            },
                            label = { Text("Name") },
                            placeholder = { Text("Enter your name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email") },
                        placeholder = { Text("your@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Enter password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (isSignUpMode) {
                                when {
                                    name.isBlank() -> errorMessage = "Please enter your name"
                                    email.isBlank() -> errorMessage = "Please enter your email"
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                                        errorMessage = "Please enter a valid email address"
                                    password.isBlank() -> errorMessage = "Please enter a password"
                                    password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                    else -> viewModel.signUp(email.trim().lowercase(), password, name.trim())
                                }
                            } else {
                                when {
                                    email.isBlank() -> errorMessage = "Please enter your email"
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                                        errorMessage = "Please enter a valid email address"
                                    password.isBlank() -> errorMessage = "Please enter your password"
                                    else -> viewModel.login(email.trim().lowercase(), password)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.Default.Login,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUpMode) "Sign Up" else "Login",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Forgot Password (only show in login mode)
                    if (!isSignUpMode) {
                        TextButton(
                            onClick = {
                                showForgotPasswordDialog = true
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Toggle between Sign Up and Login
                    TextButton(
                        onClick = { 
                            isSignUpMode = !isSignUpMode
                            errorMessage = null
                            // Don't clear email/password to allow easy switching
                            name = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSignUpMode) 
                                "Already have an account? Login" 
                            else 
                                "Don't have an account? Sign Up",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Google Sign-In Button
                    val clientId = googleClientId
                    val isGoogleAvailable = !clientId.isNullOrEmpty()
                    
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            clientId?.let { id ->
                                if (id.isNotEmpty()) {
                                    viewModel.signInWithGoogle(context, id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isGoogleAvailable
                    ) {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGoogleAvailable) 
                                "Sign in with Google" 
                            else 
                                "Google Sign-In Unavailable"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Forgot Password?") },
            text = {
                Text(
                    "Password reset functionality is not yet implemented. Please:\n\n" +
                    "• Contact your administrator for help\n" +
                    "• Or create a new account with a different email",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
