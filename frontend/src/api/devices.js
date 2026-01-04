/**
 * @fileoverview API client for device management operations.
 *
 * Devices represent physical IoT devices (LEDs, sensors) connected to ESP32 controllers.
 * Each device belongs to a room and has a type (LIGHT, SENSOR, SWITCH).
 *
 * @module api/devices
 */

import apiClient from './axios'

/**
 * Device API methods for CRUD operations and sensor data.
 *
 * Device types:
 * - LIGHT: LED devices with on/off, brightness, color controls
 * - SENSOR: Environmental sensors (temperature, humidity, light level)
 * - SWITCH: Physical switches/buttons
 *
 * @namespace devicesApi
 */
export const devicesApi = {
  /**
   * Retrieves all devices, optionally filtered by room.
   *
   * Returns device state information including last seen timestamp
   * and current values (on/off, brightness, color).
   *
   * @async
   * @param {string|null} [roomId=null] - Room UUID to filter by (null for all devices)
   * @returns {Promise<Array<Object>>} Array of device objects
   * @property {string} id - Device UUID
   * @property {string} name - Device display name
   * @property {string} type - Device type (LIGHT, SENSOR, SWITCH)
   * @property {string} roomId - Parent room UUID
   * @property {Object} deviceState - Current state (isOn, brightness, color, lastSeen)
   * @property {Object} metaJson - Device metadata (ledIndex, controllerId)
   */
  async getAll(roomId = null) {
    const params = roomId ? { roomId } : {}
    const response = await apiClient.get('/api/devices', { params })
    return response.data
  },

  /**
   * Creates a new device.
   *
   * For LIGHT devices, the metaJson should include ledIndex (0-4)
   * to map to the physical LED on the ESP32 strip.
   *
   * @async
   * @param {Object} device - Device configuration
   * @param {string} device.name - Device display name (required)
   * @param {string} device.roomId - Room UUID to assign to (required)
   * @param {string} device.type - Device type: 'LIGHT', 'SENSOR', or 'SWITCH'
   * @param {Object} [device.metaJson] - Additional metadata
   * @param {number} [device.metaJson.ledIndex] - LED index for LIGHT devices (0-4)
   * @param {string} [device.metaJson.controllerId] - ESP32 controller ID
   * @returns {Promise<Object>} Created device object
   * @throws {Error} If room not found or validation fails
   */
  async create(device) {
    const response = await apiClient.post('/api/devices', device)
    return response.data
  },

  /**
   * Updates an existing device.
   *
   * Can update name, room assignment, or metadata.
   * Type cannot be changed after creation.
   *
   * @async
   * @param {string} deviceId - Device UUID
   * @param {Object} device - Updated device data
   * @param {string} [device.name] - New display name
   * @param {string} [device.roomId] - New room UUID
   * @param {Object} [device.metaJson] - Updated metadata
   * @returns {Promise<Object>} Updated device object
   * @throws {Error} If device not found
   */
  async update(deviceId, device) {
    const response = await apiClient.put(`/api/devices/${deviceId}`, device)
    return response.data
  },

  /**
   * Deletes a device.
   *
   * Requires OWNER role. Associated sensor readings are preserved.
   *
   * @async
   * @param {string} deviceId - Device UUID
   * @returns {Promise<Object>} Deletion confirmation
   * @throws {Error} If device not found or user lacks permission
   */
  async delete(deviceId) {
    const response = await apiClient.delete(`/api/devices/${deviceId}`)
    return response.data
  },

  /**
   * Gets the latest sensor readings for a device.
   *
   * Returns recent readings for sensors (temperature, humidity, light level).
   * Only applicable for SENSOR type devices.
   *
   * @async
   * @param {string} deviceId - Device UUID (must be SENSOR type)
   * @returns {Promise<Array<Object>>} Array of sensor readings
   * @property {string} metric - Reading type (temperature, humidity, luminosity)
   * @property {number} value - Reading value
   * @property {string} unit - Unit of measurement (°C, %, lux)
   * @property {string} timestamp - ISO timestamp of reading
   */
  async getSensorReadings(deviceId) {
    const response = await apiClient.get(`/api/devices/${deviceId}/readings`)
    return response.data
  }
}
