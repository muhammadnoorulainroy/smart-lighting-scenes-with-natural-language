package com.smartlighting.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages JWT token storage using SharedPreferences
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "smart_lighting_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
    }

    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    /**
     * Get authentication token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * Save user information
     */
    fun saveUserInfo(userId: String?, email: String?, name: String?, role: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }
    
    /**
     * Get user role (OWNER, RESIDENT, GUEST)
     */
    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, null) ?: "GUEST"
    }
    
    /**
     * Check if user can edit (OWNER or RESIDENT)
     */
    fun canEdit(): Boolean {
        val role = getUserRole()
        return role == "OWNER" || role == "ROLE_OWNER" || role == "RESIDENT" || role == "ROLE_RESIDENT"
    }

    /**
     * Get user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Get user name
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Clear all authentication data
     */
    fun clearAuth() {
        prefs.edit().apply {
            remove(KEY_JWT_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            apply()
        }
    }
}

