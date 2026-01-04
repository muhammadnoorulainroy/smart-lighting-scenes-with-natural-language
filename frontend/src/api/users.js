/**
 * @fileoverview API client for user management operations.
 *
 * Provides administrative functions for managing users in the system.
 * Most operations require OWNER role.
 *
 * User roles:
 * - OWNER: Full access, can manage users and settings
 * - RESIDENT: Can control lights, create scenes/schedules
 * - GUEST: Read-only access, can view status
 *
 * @module api/users
 */

import apiClient from './axios'

/**
 * Users API for administrative user management.
 *
 * All methods except getAll require OWNER role.
 *
 * @namespace usersApi
 */
export const usersApi = {
  /**
   * Retrieves all users in the system.
   *
   * Requires OWNER role.
   *
   * @async
   * @returns {Promise<Array<Object>>} Array of user objects
   * @property {string} id - User UUID
   * @property {string} name - Display name
   * @property {string} email - Email address
   * @property {string} role - User role (OWNER, RESIDENT, GUEST)
   * @property {boolean} isActive - Whether account is enabled
   * @property {string} [pictureUrl] - Profile picture URL
   * @property {string} provider - Auth provider (GOOGLE, LOCAL)
   */
  async getAll() {
    const response = await apiClient.get('/api/users')
    return response.data
  },

  /**
   * Creates a new user account.
   *
   * Creates a LOCAL auth user. For Google OAuth users,
   * accounts are created automatically on first login.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {Object} userData - User configuration
   * @param {string} userData.name - Display name
   * @param {string} userData.email - Email address (must be unique)
   * @param {string} userData.password - Password (min 6 characters)
   * @param {string} [userData.role='GUEST'] - Initial role
   * @returns {Promise<Object>} Created user object
   * @throws {Error} If email already exists or validation fails
   */
  async create(userData) {
    const response = await apiClient.post('/api/users', userData)
    return response.data
  },

  /**
   * Updates a user's role.
   *
   * Cannot change your own role. Requires OWNER role.
   *
   * @async
   * @param {string} userId - User UUID
   * @param {string} role - New role: 'OWNER', 'RESIDENT', or 'GUEST'
   * @returns {Promise<Object>} Updated user object
   * @throws {Error} If user not found or trying to change own role
   */
  async updateRole(userId, role) {
    const response = await apiClient.put(`/api/users/${userId}/role`, { role })
    return response.data
  },

  /**
   * Disables a user account.
   *
   * Disabled users cannot log in but their data is preserved.
   * Cannot disable your own account. Requires OWNER role.
   *
   * @async
   * @param {string} userId - User UUID
   * @returns {Promise<Object>} Updated user object
   * @throws {Error} If user not found or trying to disable self
   */
  async disable(userId) {
    const response = await apiClient.put(`/api/users/${userId}/disable`)
    return response.data
  },

  /**
   * Enables a previously disabled user account.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {string} userId - User UUID
   * @returns {Promise<Object>} Updated user object
   * @throws {Error} If user not found
   */
  async enable(userId) {
    const response = await apiClient.put(`/api/users/${userId}/enable`)
    return response.data
  }
}
