/**
 * @fileoverview API client for room management operations.
 * @module api/rooms
 * @author Smart Lighting Team
 * @version 1.0.0
 */

import apiClient from './axios'

/**
 * Room API methods for CRUD operations.
 * @namespace
 */
export const roomsApi = {
  /**
   * Retrieves all rooms.
   * @returns {Promise<Array>} Array of room objects
   */
  async getAll() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  /**
   * Retrieves all rooms (alias for getAll).
   * @returns {Promise<Array>} Array of room objects
   */
  async getRooms() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  /**
   * Retrieves a room by its UUID.
   * @param {string} id - Room UUID
   * @returns {Promise<Object>} Room object with devices
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
