import apiClient from './axios'

/**
 * Scenes API for managing lighting presets
 */
export const scenesApi = {
  /**
   * Get all scenes
   */
  async getAll() {
    const response = await apiClient.get('/api/scenes')
    return response.data
  },

  /**
   * Get a scene by ID
   */
  async getById(sceneId) {
    const response = await apiClient.get(`/api/scenes/${sceneId}`)
    return response.data
  },

  /**
   * Create a new scene
   */
  async create(scene) {
    const response = await apiClient.post('/api/scenes', scene)
    return response.data
  },

  /**
   * Update an existing scene
   */
  async update(sceneId, scene) {
    const response = await apiClient.put(`/api/scenes/${sceneId}`, scene)
    return response.data
  },

  /**
   * Delete a scene
   */
  async delete(sceneId) {
    await apiClient.delete(`/api/scenes/${sceneId}`)
  },

  /**
   * Apply a scene to the lights
   */
  async apply(sceneId) {
    const response = await apiClient.post(`/api/scenes/${sceneId}/apply`)
    return response.data
  }
}
