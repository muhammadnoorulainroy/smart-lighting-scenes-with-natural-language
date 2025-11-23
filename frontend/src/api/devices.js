import apiClient from './axios'

export const devicesApi = {
  async getAll(roomId = null) {
    const params = roomId ? { roomId } : {}
    const response = await apiClient.get('/api/devices', { params })
    return response.data
  },

  async create(device) {
    const response = await apiClient.post('/api/devices', device)
    return response.data
  },

  async update(deviceId, device) {
    const response = await apiClient.put(`/api/devices/${deviceId}`, device)
    return response.data
  },

  async delete(deviceId) {
    const response = await apiClient.delete(`/api/devices/${deviceId}`)
    return response.data
  }
}

