/**
 * @fileoverview API client for schedule management.
 *
 * Schedules are automated lighting rules that trigger at specific times
 * or based on sun position (sunrise/sunset). They can:
 * - Turn lights on/off at specific times
 * - Apply scenes automatically
 * - Run on specific days (weekdays, weekends, etc.)
 *
 * @module api/schedules
 */

import apiClient from './axios'

/**
 * Schedules API for managing lighting automations.
 *
 * Schedule types:
 * - Time-based: Triggers at a specific time (e.g., "07:00:00")
 * - Sun-based: Triggers at sunrise/sunset with optional offset
 *
 * @namespace schedulesApi
 */
export const schedulesApi = {
  /**
   * Retrieves all schedules for the current user.
   *
   * Returns both enabled and disabled schedules.
   *
   * @async
   * @returns {Promise<Array<Object>>} Array of schedule objects
   * @property {string} id - Schedule UUID
   * @property {string} name - Schedule name
   * @property {string} [description] - Optional description
   * @property {boolean} enabled - Whether schedule is active
   * @property {string} triggerType - 'time' or 'sun'
   * @property {Object} triggerConfig - Trigger configuration
   * @property {string} [triggerConfig.at] - Time in HH:mm:ss format
   * @property {string} [triggerConfig.event] - 'sunrise' or 'sunset'
   * @property {number} [triggerConfig.offset_minutes] - Minutes before/after
   * @property {Array<string>} [triggerConfig.weekdays] - Days to run
   * @property {Array<Object>} actions - Actions to execute
   */
  async getAll() {
    const response = await apiClient.get('/api/schedules')
    return response.data
  },

  /**
   * Retrieves a single schedule by ID.
   *
   * @async
   * @param {string} scheduleId - Schedule UUID
   * @returns {Promise<Object>} Schedule object with full configuration
   * @throws {Error} If schedule not found (404)
   */
  async getById(scheduleId) {
    const response = await apiClient.get(`/api/schedules/${scheduleId}`)
    return response.data
  },

  /**
   * Creates a new schedule.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {Object} schedule - Schedule configuration
   * @param {string} schedule.name - Schedule name (required)
   * @param {string} [schedule.description] - Optional description
   * @param {boolean} [schedule.enabled=true] - Whether to enable immediately
   * @param {string} schedule.triggerType - 'time' or 'sun'
   * @param {Object} schedule.triggerConfig - Trigger settings
   * @param {Array<Object>} schedule.actions - Actions to execute
   * @returns {Promise<Object>} Created schedule object
   * @throws {Error} If validation fails
   */
  async create(schedule) {
    const response = await apiClient.post('/api/schedules', schedule)
    return response.data
  },

  /**
   * Updates an existing schedule.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {string} scheduleId - Schedule UUID
   * @param {Object} schedule - Updated schedule data
   * @returns {Promise<Object>} Updated schedule object
   * @throws {Error} If schedule not found or validation fails
   */
  async update(scheduleId, schedule) {
    const response = await apiClient.put(`/api/schedules/${scheduleId}`, schedule)
    return response.data
  },

  /**
   * Toggles a schedule's enabled state.
   *
   * Convenience method to enable/disable without fetching full schedule.
   *
   * @async
   * @param {string} scheduleId - Schedule UUID
   * @returns {Promise<Object>} Updated schedule with new enabled state
   */
  async toggle(scheduleId) {
    const response = await apiClient.post(`/api/schedules/${scheduleId}/toggle`)
    return response.data
  },

  /**
   * Deletes a schedule.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {string} scheduleId - Schedule UUID
   * @returns {Promise<void>}
   * @throws {Error} If schedule not found or user lacks permission
   */
  async delete(scheduleId) {
    await apiClient.delete(`/api/schedules/${scheduleId}`)
  }
}
