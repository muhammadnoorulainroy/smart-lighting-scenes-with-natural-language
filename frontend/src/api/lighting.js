/**
 * @fileoverview API client for direct lighting control.
 *
 * Provides low-level control over LED devices via MQTT commands.
 * Use this for immediate, manual lighting adjustments rather than
 * scenes or schedules.
 *
 * Commands are sent via the backend which publishes to MQTT topics.
 * Acknowledgments are received via WebSocket for confirmation.
 *
 * @module api/lighting
 */

import apiClient from './axios'

/**
 * Lighting control API for direct device commands.
 *
 * LED devices support:
 * - Power: on/off
 * - Brightness: 0-100%
 * - Color: RGB values [0-255, 0-255, 0-255]
 * - Color temperature: 2700K (warm) to 6500K (cool)
 * - Mode: 'auto' (sensor-responsive) or 'manual' (fixed)
 *
 * @namespace lightingApi
 */
export const lightingApi = {
  /**
   * Sends a command to control a specific LED device.
   *
   * The command is published via MQTT to the device's control topic.
   * Multiple properties can be set in a single command.
   *
   * @async
   * @param {string} deviceId - Device UUID
   * @param {Object} command - LED command parameters
   * @param {boolean} [command.on] - Power state (true = on, false = off)
   * @param {Array<number>} [command.rgb] - RGB color as [R, G, B] (0-255 each)
   * @param {number} [command.brightness] - Brightness percentage (0-100)
   * @param {number} [command.color_temp] - Color temperature in Kelvin (2700-6500)
   * @param {string} [command.mode] - Control mode: 'auto' or 'manual'
   * @returns {Promise<Object>} Command result with correlation ID
   */
  async sendLedCommand(deviceId, command) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/command`, command)
    return response.data
  },

  /**
   * Applies a scene preset to a specific device.
   *
   * Device scenes are simpler than global scenes - they apply
   * predefined settings directly to one device.
   *
   * @async
   * @param {string} deviceId - Device UUID
   * @param {string} sceneName - Name of the scene preset
   * @returns {Promise<Object>} Command result
   */
  async setScene(deviceId, sceneName) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/scene`, {
      scene: sceneName
    })
    return response.data
  },

  /**
   * Turns a device on or off.
   *
   * Simple power toggle without changing other settings.
   * Current brightness and color are preserved.
   *
   * @async
   * @param {string} deviceId - Device UUID
   * @param {boolean} on - True to turn on, false to turn off
   * @returns {Promise<Object>} Command result
   */
  async setPower(deviceId, on) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/power`, { on })
    return response.data
  },

  /**
   * Sets the control mode for a specific controller.
   *
   * Modes:
   * - 'auto': Light responds to sensor data (temperature, humidity, ambient light)
   * - 'manual': Light maintains fixed settings until changed
   *
   * @async
   * @param {string} controllerId - ESP32 controller ID
   * @param {string} mode - Control mode: 'auto' or 'manual'
   * @returns {Promise<Object>} Command result
   */
  async setMode(controllerId, mode) {
    const response = await apiClient.post(`/api/lighting/controllers/${controllerId}/mode`, {
      mode
    })
    return response.data
  },

  /**
   * Sets the global control mode for all lights.
   *
   * Affects all connected LED devices across all controllers.
   *
   * @async
   * @param {string} mode - Control mode: 'auto' or 'manual'
   * @returns {Promise<Object>} Command result
   */
  async setGlobalMode(mode) {
    const response = await apiClient.post('/api/lighting/mode', { mode })
    return response.data
  }
}
