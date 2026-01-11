/**
 * @fileoverview Authentication API client for OAuth and local authentication.
 *
 * Provides methods for:
 * - Checking authentication status
 * - Getting current user profile
 * - Email/password signup and login
 * - Logging out
 *
 * Note: Google OAuth is handled via redirect, not through this API.
 * See {@link module:stores/auth} for the OAuth flow.
 *
 * @module api/auth
 */

import apiClient from './axios'

/**
 * Authentication API methods for user management.
 *
 * All methods use session cookies for authentication state.
 * The backend manages sessions, so no tokens are stored client-side.
 *
 * @namespace authApi
 */
export const authApi = {
  /**
   * Checks if the current session is authenticated.
   *
   * This is a lightweight check that doesn't fetch user details.
   * Use this for quick auth status checks without loading user data.
   *
   * @async
   * @returns {Promise<boolean>} True if authenticated, false otherwise
   */
  async checkAuth() {
    try {
      const response = await apiClient.get('/api/auth/check')
      return response.data === true
    } catch (error) {
      console.error('Auth check failed:', error)
      return false
    }
  },

  /**
   * Fetches the current authenticated user's profile.
   *
   * Returns full user details including role, email, and profile picture.
   * Throws an error if not authenticated.
   *
   * @async
   * @returns {Promise<Object>} User profile object
   * @property {string} id - User UUID
   * @property {string} name - Display name
   * @property {string} email - Email address
   * @property {string} role - User role (OWNER, RESIDENT, GUEST)
   * @property {string} [pictureUrl] - Profile picture URL (for OAuth users)
   * @throws {Error} If not authenticated or request fails
   */
  async getCurrentUser() {
    try {
      const response = await apiClient.get('/api/me')
      return response.data
    } catch (error) {
      console.error('Failed to get current user:', error)
      throw error
    }
  },

  /**
   * Registers a new user with email and password.
   *
   * On success, the user is automatically logged in and a JWT token is returned.
   * The user starts with GUEST role by default.
   *
   * @async
   * @param {string} email - User's email address (must be unique)
   * @param {string} password - Password (min 6 characters)
   * @param {string} name - Display name
   * @returns {Promise<Object>} Object with user profile and JWT token
   * @throws {Error} If email already exists or validation fails
   */
  async signup(email, password, name) {
    const response = await apiClient.post('/api/auth/signup', {
      email,
      password,
      name
    })
    // Response contains { user, token }
    return response.data
  },

  /**
   * Authenticates a user with email and password.
   *
   * On success, returns user profile and JWT token for cross-domain auth.
   *
   * @async
   * @param {string} email - User's email address
   * @param {string} password - User's password
   * @returns {Promise<Object>} Object with user profile and JWT token
   * @throws {Error} If credentials are invalid
   */
  async login(email, password) {
    const response = await apiClient.post('/api/auth/login', {
      email,
      password
    })
    // Response contains { user, token }
    return response.data
  },

  /**
   * Logs out the current user and invalidates the session.
   *
   * Clears the session cookie on the backend.
   * The frontend should redirect to home after calling this.
   *
   * @async
   * @returns {Promise<void>}
   * @throws {Error} If logout request fails (session is still cleared locally)
   */
  async logout() {
    try {
      await apiClient.post('/api/auth/logout')
    } catch (error) {
      console.error('Logout failed:', error)
      throw error
    }
  }
}
