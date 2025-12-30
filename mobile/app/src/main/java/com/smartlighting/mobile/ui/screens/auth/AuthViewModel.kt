package com.smartlighting.mobile.ui.screens.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.smartlighting.mobile.data.repository.LightingRepository
import com.smartlighting.mobile.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling Google Sign-In authentication
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: LightingRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val authState: StateFlow<UiState<String>> = _authState.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _googleClientId = MutableStateFlow<String?>(null)
    val googleClientId: StateFlow<String?> = _googleClientId.asStateFlow()
    
    init {
        loadAuthConfig()
    }
    
    private fun loadAuthConfig() {
        viewModelScope.launch {
            val result = repository.getAuthConfig()
            if (result.isSuccess) {
                _googleClientId.value = result.getOrNull()?.get("googleClientId")
            } else {
                _authState.value = UiState.Error("Failed to load auth config: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Sign in with Google using Credential Manager
     */
    fun signInWithGoogle(context: Context, clientId: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                // Send token to backend for authentication
                authenticateWithBackend(idToken)
                
            } catch (e: GetCredentialException) {
                _authState.value = UiState.Error("Authentication failed: ${e.message}")
            } catch (e: Exception) {
                _authState.value = UiState.Error("An error occurred: ${e.message}")
            }
        }
    }
    
    /**
     * Authenticate with backend using Google ID token
     */
    private suspend fun authenticateWithBackend(idToken: String) {
        val result = repository.authenticateWithGoogle(idToken)
        if (result.isSuccess) {
            _isAuthenticated.value = true
            _authState.value = UiState.Success("Authenticated successfully")
        } else {
            _authState.value = UiState.Error("Backend authentication failed: ${result.exceptionOrNull()?.message}")
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        _isAuthenticated.value = false
        _authState.value = UiState.Idle
    }
    
    /**
     * Reset auth state
     */
    fun resetAuthState() {
        _authState.value = UiState.Idle
    }
}
