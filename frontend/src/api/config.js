import apiClient from './axios'

/**
 * System Configuration API
 * Manages ESP32 runtime settings
 */
export const configApi = {
  /**
   * Get all configuration settings
   */
  async getAll() {
    const response = await apiClient.get('/api/config')
    return response.data
  },

  /**
   * Get configuration for a specific category
   * @param {string} category - lighting, climate, audio, display, mqtt
   */
  async getCategory(category) {
    const response = await apiClient.get(`/api/config/${category}`)
    return response.data
  },

  /**
   * Update configuration for a specific category
   * @param {string} category - Category name
   * @param {object} settings - Settings to update
   */
  async updateCategory(category, settings) {
    const response = await apiClient.put(`/api/config/${category}`, settings)
    return response.data
  },

  /**
   * Update all configuration categories at once
   * @param {object} allSettings - { lighting: {...}, climate: {...}, ... }
   */
  async updateAll(allSettings) {
    const response = await apiClient.put('/api/config', allSettings)
    return response.data
  },

  /**
   * Reset a category to defaults
   * @param {string} category - Category name
   */
  async resetCategory(category) {
    const response = await apiClient.post(`/api/config/${category}/reset`)
    return response.data
  },

  /**
   * Reset all categories to defaults
   */
  async resetAll() {
    const response = await apiClient.post('/api/config/reset')
    return response.data
  },

  /**
   * Manually sync config to ESP32 devices
   */
  async syncToDevices() {
    const response = await apiClient.post('/api/config/sync')
    return response.data
  }
}
