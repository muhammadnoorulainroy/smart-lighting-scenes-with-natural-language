/**
 * @fileoverview API client for device management operations.
 * @module api/devices
 * @author Smart Lighting Team
 * @version 1.0.0
 */

import apiClient from './axios'

/**
 * Device API methods for CRUD operations.
 * @namespace
 */
export const devicesApi = {
  /**
   * Retrieves all devices, optionally filtered by room.
   * @param {string|null} [roomId=null] - Optional room UUID to filter by
   * @returns {Promise<Array>} Array of device objects
   */
  async getAll(roomId = null) {
    const params = roomId ? { roomId } : {}
    const response = await apiClient.get('/api/devices', { params })
    return response.data
  },

  /**
   * Creates a new device.
   * @param {Object} device - Device configuration
   * @param {string} device.name - Device name
   * @param {string} device.roomId - Room UUID
   * @param {string} device.type - Device type (LIGHT, SENSOR, SWITCH)
   * @returns {Promise<Object>} Created device
   */
  async create(device) {
    const response = await apiClient.post('/api/devices', device)
    return response.data
  },

  /**
   * Updates an existing device.
   * @param {string} deviceId - Device UUID
   * @param {Object} device - Updated device data
   * @returns {Promise<Object>} Updated device
   */
  async update(deviceId, device) {
    const response = await apiClient.put(`/api/devices/${deviceId}`, device)
    return response.data
  },

  /**
   * Deletes a device.
   * @param {string} deviceId - Device UUID
   * @returns {Promise<Object>} Deletion confirmation
   */
  async delete(deviceId) {
    const response = await apiClient.delete(`/api/devices/${deviceId}`)
    return response.data
  },

  /**
   * Gets the latest sensor readings for a device.
   * @param {string} deviceId - Device UUID
   * @returns {Promise<Array>} Array of sensor readings
   */
  async getSensorReadings(deviceId) {
    const response = await apiClient.get(`/api/devices/${deviceId}/readings`)
    return response.data
  }
}
