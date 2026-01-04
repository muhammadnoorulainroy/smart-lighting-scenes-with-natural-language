/**
 * @fileoverview API client for system event logging.
 *
 * Events track significant actions in the system like:
 * - Scene applications
 * - Schedule executions
 * - Device state changes
 * - User actions
 *
 * Used for activity history and debugging.
 *
 * @module api/events
 */

import apiClient from './axios'

/**
 * Events API for retrieving system activity logs.
 *
 * Events are read-only records created by the backend.
 *
 * @namespace eventsApi
 */
export const eventsApi = {
  /**
   * Retrieves system events with optional filtering.
   *
   * Returns events sorted by timestamp (newest first).
   *
   * @async
   * @param {Object} [params={}] - Query parameters
   * @param {number} [params.limit] - Maximum events to return
   * @param {number} [params.offset] - Number of events to skip (pagination)
   * @param {string} [params.deviceId] - Filter by device UUID
   * @param {string} [params.type] - Filter by event type
   * @param {string} [params.from] - Start timestamp (ISO format)
   * @param {string} [params.to] - End timestamp (ISO format)
   * @returns {Promise<Array<Object>>} Array of event objects
   * @property {string} id - Event UUID
   * @property {string} type - Event type (SCENE_APPLIED, SCHEDULE_EXECUTED, etc.)
   * @property {string} description - Human-readable description
   * @property {Object} [data] - Additional event data
   * @property {string} timestamp - ISO timestamp
   * @property {string} [deviceId] - Related device UUID
   * @property {string} [userId] - User who triggered the event
   */
  async getAll(params = {}) {
    const response = await apiClient.get('/api/events', { params })
    return response.data
  }
}
