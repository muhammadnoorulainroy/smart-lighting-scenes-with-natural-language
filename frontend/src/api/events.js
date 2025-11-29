import apiClient from './axios'

export const eventsApi = {
  async getAll(params = {}) {
    const response = await apiClient.get('/api/events', { params })
    return response.data
  }
}
