/**
 * @fileoverview API client for system configuration management.
 *
 * Manages runtime configuration for ESP32 controllers including:
 * - Lighting: Default brightness, color temperature ranges
 * - Climate: Temperature-based color adjustments
 * - Audio: Music-reactive settings
 * - Display: OLED display configuration
 * - MQTT: Connection and topic settings
 *
 * Configuration changes are synced to ESP32 devices via MQTT.
 *
 * @module api/config
 */

import apiClient from './axios'

/**
 * System Configuration API for ESP32 runtime settings.
 *
 * Categories:
 * - lighting: LED control defaults and ranges
 * - climate: Temperature/humidity sensor thresholds
 * - audio: Microphone and music-reactive settings
 * - display: OLED display preferences
 * - mqtt: MQTT broker configuration
 *
 * Requires OWNER role for write operations.
 *
 * @namespace configApi
 */
export const configApi = {
  /**
   * Retrieves all configuration settings.
   *
   * Returns settings organized by category.
   *
   * @async
   * @returns {Promise<Object>} Configuration object by category
   * @property {Object} lighting - Lighting settings
   * @property {number} lighting.default_brightness - Default brightness (0-100)
   * @property {number} lighting.min_brightness - Minimum brightness
   * @property {number} lighting.max_brightness - Maximum brightness
   * @property {Object} climate - Climate sensor settings
   * @property {Object} audio - Audio/microphone settings
   * @property {Object} display - OLED display settings
   * @property {Object} mqtt - MQTT connection settings
   */
  async getAll() {
    const response = await apiClient.get('/api/config')
    return response.data
  },

  /**
   * Retrieves configuration for a specific category.
   *
   * @async
   * @param {string} category - Category name: 'lighting', 'climate', 'audio', 'display', 'mqtt'
   * @returns {Promise<Object>} Category settings
   * @throws {Error} If category not found
   */
  async getCategory(category) {
    const response = await apiClient.get(`/api/config/${category}`)
    return response.data
  },

  /**
   * Updates configuration for a specific category.
   *
   * Only provided fields are updated; others remain unchanged.
   * Changes are automatically synced to ESP32 devices.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {string} category - Category name
   * @param {Object} settings - Settings to update (partial update supported)
   * @returns {Promise<Object>} Updated category settings
   * @throws {Error} If validation fails or user lacks permission
   */
  async updateCategory(category, settings) {
    const response = await apiClient.put(`/api/config/${category}`, settings)
    return response.data
  },

  /**
   * Updates all configuration categories at once.
   *
   * Useful for bulk configuration changes or restoring a backup.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {Object} allSettings - Settings organized by category
   * @param {Object} [allSettings.lighting] - Lighting settings
   * @param {Object} [allSettings.climate] - Climate settings
   * @param {Object} [allSettings.audio] - Audio settings
   * @param {Object} [allSettings.display] - Display settings
   * @param {Object} [allSettings.mqtt] - MQTT settings
   * @returns {Promise<Object>} Updated configuration
   */
  async updateAll(allSettings) {
    const response = await apiClient.put('/api/config', allSettings)
    return response.data
  },

  /**
   * Resets a category to factory defaults.
   *
   * Requires OWNER role.
   *
   * @async
   * @param {string} category - Category name to reset
   * @returns {Promise<Object>} Default settings for the category
   */
  async resetCategory(category) {
    const response = await apiClient.post(`/api/config/${category}/reset`)
    return response.data
  },

  /**
   * Resets all categories to factory defaults.
   *
   * Requires OWNER role.
   *
   * @async
   * @returns {Promise<Object>} All default settings
   */
  async resetAll() {
    const response = await apiClient.post('/api/config/reset')
    return response.data
  },

  /**
   * Manually triggers configuration sync to ESP32 devices.
   *
   * Normally configuration is synced automatically on changes.
   * Use this if devices are out of sync or after network issues.
   *
   * Requires OWNER role.
   *
   * @async
   * @returns {Promise<Object>} Sync result
   * @property {string} message - Status message
   * @property {number} devicesUpdated - Number of devices that received update
   */
  async syncToDevices() {
    const response = await apiClient.post('/api/config/sync')
    return response.data
  }
}
