/**
 * @fileoverview Pinia store for authentication state management.
 * @module stores/auth
 * @author Smart Lighting Team
 * @version 1.0.0
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import logger from '../utils/logger'

const MODULE = 'AuthStore'

/**
 * Authentication store using Pinia composition API.
 *
 * Manages user authentication state, profile data, and role-based access.
 * Integrates with Google OAuth via the backend API.
 *
 * @returns {Object} Store instance with state, getters, and actions
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const isAuthenticated = ref(false)
  const isLoading = ref(true)
  const error = ref(null)

  const userName = computed(() => user.value?.name || 'Guest')
  const userEmail = computed(() => user.value?.email || '')
  const userPicture = computed(() => user.value?.pictureUrl || '')
  const userRole = computed(() => user.value?.role || 'GUEST')
  const isOwner = computed(() => userRole.value === 'OWNER')
  const isResident = computed(() => userRole.value === 'RESIDENT' || userRole.value === 'OWNER')

  const checkAuth = async () => {
    try {
      isLoading.value = true
      error.value = null
      logger.debug(MODULE, 'Checking authentication status')

      const authenticated = await authApi.checkAuth()

      if (authenticated) {
        logger.debug(MODULE, 'User is authenticated, fetching profile')
        await fetchCurrentUser()
      } else {
        logger.info(MODULE, 'User is not authenticated')
        clearAuth()
      }
    } catch (err) {
      logger.error(MODULE, 'Failed to check authentication', err)
      clearAuth()
    } finally {
      isLoading.value = false
    }
  }

  const fetchCurrentUser = async () => {
    try {
      logger.debug(MODULE, 'Fetching current user profile')
      const userData = await authApi.getCurrentUser()
      user.value = userData
      isAuthenticated.value = true
      error.value = null
      logger.info(MODULE, `User authenticated: ${userData.email} (${userData.role})`)
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch user profile', err)
      clearAuth()
      throw err
    }
  }

  const loginWithGoogle = () => {
    logger.info(MODULE, 'Initiating OAuth login redirect')
    // relative URL in production (Docker), or explicit URL in dev
    const baseUrl = import.meta.env.VITE_API_URL ?? ''
    window.location.href = `${baseUrl}/oauth2/authorization/google`
  }

  // Keep old name for backward compatibility
  const login = loginWithGoogle

  const loginWithEmail = async (email, password) => {
    try {
      isLoading.value = true
      error.value = null
      logger.info(MODULE, `Attempting email login for: ${email}`)
      
      const userData = await authApi.login(email, password)
      user.value = userData
      isAuthenticated.value = true
      logger.info(MODULE, `Email login successful: ${userData.email}`)
      return userData
    } catch (err) {
      const message = err.response?.data?.error || 'Login failed'
      logger.error(MODULE, `Email login failed: ${message}`, err)
      error.value = message
      throw new Error(message)
    } finally {
      isLoading.value = false
    }
  }

  const signup = async (email, password, name) => {
    try {
      isLoading.value = true
      error.value = null
      logger.info(MODULE, `Attempting signup for: ${email}`)
      
      const userData = await authApi.signup(email, password, name)
      user.value = userData
      isAuthenticated.value = true
      logger.info(MODULE, `Signup successful: ${userData.email}`)
      return userData
    } catch (err) {
      const message = err.response?.data?.error || 'Signup failed'
      logger.error(MODULE, `Signup failed: ${message}`, err)
      error.value = message
      throw new Error(message)
    } finally {
      isLoading.value = false
    }
  }

  const logout = async () => {
    try {
      logger.info(MODULE, 'User logging out')
      await authApi.logout()
      clearAuth()
      logger.info(MODULE, 'Logout successful')
      window.location.href = '/'
    } catch (err) {
      logger.error(MODULE, 'Logout failed', err)
      error.value = 'Failed to logout'
      logger.warn(MODULE, 'Clearing auth state despite logout failure')
      clearAuth()
      window.location.href = '/'
    }
  }

  const clearAuth = () => {
    logger.debug(MODULE, 'Clearing authentication state')
    user.value = null
    isAuthenticated.value = false
    error.value = null
  }

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    error,
    // Computed
    userName,
    userEmail,
    userPicture,
    userRole,
    isOwner,
    isResident,
    // Actions
    checkAuth,
    fetchCurrentUser,
    login,
    loginWithGoogle,
    loginWithEmail,
    signup,
    logout,
    clearAuth
  }
})
