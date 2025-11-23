import apiClient from './axios'

export const roomsApi = {
  async getAll() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  async getRooms() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  /**
   * Get room by ID
   * @param {number} id - Room ID
   * @returns {Promise<Object>}
   */
  async getRoomById(id) {
    const response = await apiClient.get(`/api/rooms/${id}`)
    return response.data
  },

  /**
   * Create a new room
   * @param {Object} roomData - Room data
   * @returns {Promise<Object>}
   */
  async createRoom(roomData) {
    const response = await apiClient.post('/api/rooms', roomData)
    return response.data
  },

  /**
   * Update a room
   * @param {number} id - Room ID
   * @param {Object} roomData - Updated room data
   * @returns {Promise<Object>}
   */
  async updateRoom(id, roomData) {
    const response = await apiClient.put(`/api/rooms/${id}`, roomData)
    return response.data
  },

  /**
   * Delete a room
   * @param {number} id - Room ID
   * @returns {Promise<void>}
   */
  async deleteRoom(id) {
    await apiClient.delete(`/api/rooms/${id}`)
  }
}






