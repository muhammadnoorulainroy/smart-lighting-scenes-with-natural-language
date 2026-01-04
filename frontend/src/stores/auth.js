/**
 * @fileoverview Pinia store for authentication state management.
 *
 * Manages the complete authentication lifecycle including:
 * - Session state (authenticated, loading, error)
 * - User profile data
 * - Google OAuth integration
 * - Email/password authentication
 * - Role-based access control
 *
 * Uses composition API style for better TypeScript inference
 * and more flexible state management.
 *
 * @module stores/auth
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import logger from '../utils/logger'

/** @constant {string} Module name for logging */
const MODULE = 'AuthStore'

/**
 * Authentication store using Pinia composition API.
 *
 * State:
 * - user: Current user object or null
 * - isAuthenticated: Boolean flag
 * - isLoading: True during async operations
 * - error: Last error message or null
 *
 * Getters:
 * - userName, userEmail, userPicture: User profile fields
 * - userRole: Current role (OWNER, RESIDENT, GUEST)
 * - isOwner, isResident: Role-based permission checks
 *
 * Actions:
 * - checkAuth: Verify session on app load
 * - loginWithGoogle: Initiate OAuth flow
 * - loginWithEmail: Email/password login
 * - signup: Create new account
 * - logout: End session
 *
 * @function useAuthStore
 * @returns {Object} Store instance with state, getters, and actions
 */
export const useAuthStore = defineStore('auth', () => {
  // State

  /** Current authenticated user or null if not logged in */
  const user = ref(null)

  /** Whether the user is currently authenticated */
  const isAuthenticated = ref(false)

  /** Loading state for async operations */
  const isLoading = ref(true)

  /** Last error message or null */
  const error = ref(null)

  // Getters

  /** User's display name or 'Guest' if not logged in */
  const userName = computed(() => user.value?.name || 'Guest')

  /** User's email address or empty string */
  const userEmail = computed(() => user.value?.email || '')

  /** User's profile picture URL (from OAuth) or empty string */
  const userPicture = computed(() => user.value?.pictureUrl || '')

  /** User's role: 'OWNER', 'RESIDENT', or 'GUEST' */
  const userRole = computed(() => user.value?.role || 'GUEST')

  /** Whether user has OWNER role (full admin access) */
  const isOwner = computed(() => userRole.value === 'OWNER')

  /** Whether user has RESIDENT or higher role (can control lights) */
  const isResident = computed(() => userRole.value === 'RESIDENT' || userRole.value === 'OWNER')

  // Actions

  /**
   * Checks authentication status and loads user profile.
   *
   * Should be called on app initialization to restore session.
   * Sets isLoading while checking, then updates state.
   *
   * @async
   * @returns {Promise<void>}
   */
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

  /**
   * Fetches the current user's profile from the API.
   *
   * Called internally after successful authentication.
   * Updates user state and sets isAuthenticated to true.
   *
   * @async
   * @returns {Promise<void>}
   * @throws {Error} If not authenticated or API call fails
   */
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

  /**
   * Initiates Google OAuth login flow.
   *
   * Redirects to the backend OAuth endpoint which then redirects to Google.
   * On success, user returns to /auth/callback with session cookie set.
   *
   * @returns {void}
   */
  const loginWithGoogle = () => {
    logger.info(MODULE, 'Initiating OAuth login redirect')
    // Relative URL in production (Docker), or explicit URL in dev
    const baseUrl = import.meta.env.VITE_API_URL ?? ''
    window.location.href = `${baseUrl}/oauth2/authorization/google`
  }

  const login = loginWithGoogle

  /**
   * Authenticates with email and password.
   *
   * On success, sets user state and isAuthenticated.
   * On failure, sets error state and throws.
   *
   * @async
   * @param {string} email - User's email address
   * @param {string} password - User's password
   * @returns {Promise<Object>} Authenticated user data
   * @throws {Error} If credentials are invalid
   */
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

  /**
   * Creates a new user account with email and password.
   *
   * On success, automatically logs in the new user.
   *
   * @async
   * @param {string} email - Email address (must be unique)
   * @param {string} password - Password (min 6 characters)
   * @param {string} name - Display name
   * @returns {Promise<Object>} Created user data
   * @throws {Error} If email exists or validation fails
   */
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

  /**
   * Logs out the current user.
   *
   * Invalidates session on backend and clears local state.
   * Redirects to home page on completion.
   *
   * @async
   * @returns {Promise<void>}
   */
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

  /**
   * Clears all authentication state.
   *
   * Called internally on logout, auth failure, or session expiry.
   *
   * @returns {void}
   */
  const clearAuth = () => {
    logger.debug(MODULE, 'Clearing authentication state')
    user.value = null
    isAuthenticated.value = false
    error.value = null
  }

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    userName,
    userEmail,
    userPicture,
    userRole,
    isOwner,
    isResident,
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
