import apiClient from './axios'

/**
 * Schedules API for managing lighting automations
 */
export const schedulesApi = {
  /**
   * Get all schedules
   */
  async getAll() {
    const response = await apiClient.get('/api/schedules')
    return response.data
  },

  /**
   * Get a schedule by ID
   */
  async getById(scheduleId) {
    const response = await apiClient.get(`/api/schedules/${scheduleId}`)
    return response.data
  },

  /**
   * Create a new schedule
   */
  async create(schedule) {
    const response = await apiClient.post('/api/schedules', schedule)
    return response.data
  },

  /**
   * Update an existing schedule
   */
  async update(scheduleId, schedule) {
    const response = await apiClient.put(`/api/schedules/${scheduleId}`, schedule)
    return response.data
  },

  /**
   * Toggle schedule enabled/disabled
   */
  async toggle(scheduleId) {
    const response = await apiClient.post(`/api/schedules/${scheduleId}/toggle`)
    return response.data
  },

  /**
   * Delete a schedule
   */
  async delete(scheduleId) {
    await apiClient.delete(`/api/schedules/${scheduleId}`)
  }
}
