/**
 * @fileoverview API client for lighting scene management.
 *
 * Scenes are predefined lighting configurations that can be applied
 * to one or more lights simultaneously. They store settings like
 * brightness, color, and color temperature.
 *
 * Common scenes include: Morning, Evening, Movie Night, Reading, etc.
 *
 * @module api/scenes
 */

import apiClient from './axios'

/**
 * Scenes API for managing lighting presets.
 *
 * Scenes are stored configurations that can be:
 * - Created manually through the UI
 * - Generated via natural language commands
 * - Applied to specific rooms or all lights
 *
 * @namespace scenesApi
 */
export const scenesApi = {
  /**
   * Retrieves all active scenes.
   *
   * Returns scenes sorted by name. Only active (non-deleted) scenes are returned.
   *
   * @async
   * @returns {Promise<Array<Object>>} Array of scene objects
   * @property {string} id - Scene UUID
   * @property {string} name - Scene display name
   * @property {string} [description] - Optional description
   * @property {Object} settingsJson - Scene settings
   * @property {number} settingsJson.brightness - Brightness percentage (0-100)
   * @property {Array<number>} [settingsJson.rgb] - RGB color [R, G, B]
   * @property {number} [settingsJson.color_temp] - Color temperature in Kelvin
   * @property {boolean} isActive - Whether scene is active
   */
  async getAll() {
    const response = await apiClient.get('/api/scenes')
    return response.data
  },

  /**
   * Retrieves a single scene by ID.
   *
   * @async
   * @param {string} sceneId - Scene UUID
   * @returns {Promise<Object>} Scene object with full settings
   * @throws {Error} If scene not found (404)
   */
  async getById(sceneId) {
    const response = await apiClient.get(`/api/scenes/${sceneId}`)
    return response.data
  },

  /**
   * Creates a new lighting scene.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {Object} scene - Scene configuration
   * @param {string} scene.name - Scene name (required, must be unique)
   * @param {string} [scene.description] - Optional description
   * @param {Object} scene.settingsJson - Lighting settings
   * @param {number} scene.settingsJson.brightness - Brightness (0-100)
   * @param {Array<number>} [scene.settingsJson.rgb] - RGB color [R, G, B]
   * @param {number} [scene.settingsJson.color_temp] - Color temp (2700-6500K)
   * @param {string} [scene.settingsJson.target] - Target room or 'all'
   * @returns {Promise<Object>} Created scene object
   * @throws {Error} If name already exists or validation fails
   */
  async create(scene) {
    const response = await apiClient.post('/api/scenes', scene)
    return response.data
  },

  /**
   * Updates an existing scene.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {string} sceneId - Scene UUID
   * @param {Object} scene - Updated scene data
   * @param {string} [scene.name] - New name
   * @param {string} [scene.description] - New description
   * @param {Object} [scene.settingsJson] - Updated lighting settings
   * @returns {Promise<Object>} Updated scene object
   * @throws {Error} If scene not found or user lacks permission
   */
  async update(sceneId, scene) {
    const response = await apiClient.put(`/api/scenes/${sceneId}`, scene)
    return response.data
  },

  /**
   * Deletes (deactivates) a scene.
   *
   * Requires OWNER role. Scene is soft-deleted (marked inactive).
   *
   * @async
   * @param {string} sceneId - Scene UUID
   * @returns {Promise<void>}
   * @throws {Error} If scene not found or user lacks permission
   */
  async delete(sceneId) {
    await apiClient.delete(`/api/scenes/${sceneId}`)
  },

  /**
   * Applies a scene to the lights.
   *
   * Sends MQTT commands to all affected devices. The backend tracks
   * command delivery and returns acknowledgment status.
   *
   * @async
   * @param {string} sceneId - Scene UUID to apply
   * @returns {Promise<Object>} Application result
   * @property {string} message - Success message
   * @property {string} correlationId - ID for tracking acknowledgments
   * @property {number} devicesAffected - Number of devices commanded
   * @throws {Error} If scene not found or MQTT publish fails
   */
  async apply(sceneId) {
    const response = await apiClient.post(`/api/scenes/${sceneId}/apply`)
    return response.data
  }
}
