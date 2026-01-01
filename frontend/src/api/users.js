import apiClient from './axios'

export const usersApi = {
  async getAll() {
    const response = await apiClient.get('/api/users')
    return response.data
  },

  async create(userData) {
    const response = await apiClient.post('/api/users', userData)
    return response.data
  },

  async updateRole(userId, role) {
    const response = await apiClient.put(`/api/users/${userId}/role`, { role })
    return response.data
  },

  async disable(userId) {
    const response = await apiClient.put(`/api/users/${userId}/disable`)
    return response.data
  },

  async enable(userId) {
    const response = await apiClient.put(`/api/users/${userId}/enable`)
    return response.data
  }
}
