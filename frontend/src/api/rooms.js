/**
 * @fileoverview API client for room management operations.
 *
 * Rooms are the top-level organizational unit for devices in the smart lighting system.
 * Each room can contain multiple devices (lights, sensors).
 *
 * @module api/rooms
 */

import apiClient from './axios'

/**
 * Room API methods for CRUD operations.
 *
 * All methods require authentication. Most operations require
 * RESIDENT or OWNER role.
 *
 * @namespace roomsApi
 */
export const roomsApi = {
  /**
   * Retrieves all rooms with their associated devices.
   *
   * Returns rooms sorted by creation date. Each room includes
   * a list of devices assigned to it.
   *
   * @async
   * @returns {Promise<Array<Object>>} Array of room objects
   * @property {string} id - Room UUID
   * @property {string} name - Room display name
   * @property {string} [icon] - Emoji icon for the room
   * @property {Array<Object>} devices - Devices in this room
   */
  async getAll() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  /**
   * Retrieves all rooms (alias for getAll).
   *
   * @async
   * @returns {Promise<Array<Object>>} Array of room objects
   * @see roomsApi.getAll
   */
  async getRooms() {
    const response = await apiClient.get('/api/rooms')
    return response.data
  },

  /**
   * Retrieves a single room by its UUID.
   *
   * Includes full device details and device states.
   *
   * @async
   * @param {string} id - Room UUID
   * @returns {Promise<Object>} Room object with devices
   * @throws {Error} If room not found (404)
   */
  async getRoomById(id) {
    const response = await apiClient.get(`/api/rooms/${id}`)
    return response.data
  },

  /**
   * Creates a new room.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {Object} roomData - Room configuration
   * @param {string} roomData.name - Room name (required, 1-100 characters)
   * @param {string} [roomData.icon] - Emoji icon (optional)
   * @returns {Promise<Object>} Created room object
   * @throws {Error} If validation fails or user lacks permission
   */
  async createRoom(roomData) {
    const response = await apiClient.post('/api/rooms', roomData)
    return response.data
  },

  /**
   * Updates an existing room.
   *
   * Requires RESIDENT or OWNER role.
   *
   * @async
   * @param {string} id - Room UUID
   * @param {Object} roomData - Updated room data
   * @param {string} [roomData.name] - New room name
   * @param {string} [roomData.icon] - New emoji icon
   * @returns {Promise<Object>} Updated room object
   * @throws {Error} If room not found or user lacks permission
   */
  async updateRoom(id, roomData) {
    const response = await apiClient.put(`/api/rooms/${id}`, roomData)
    return response.data
  },

  /**
   * Deletes a room and unassigns its devices.
   *
   * Requires OWNER role. Devices are not deleted, only unassigned.
   *
   * @async
   * @param {string} id - Room UUID
   * @returns {Promise<void>}
   * @throws {Error} If room not found or user lacks permission
   */
  async deleteRoom(id) {
    await apiClient.delete(`/api/rooms/${id}`)
  }
}
