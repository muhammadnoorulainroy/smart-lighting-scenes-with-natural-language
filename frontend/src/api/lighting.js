import apiClient from './axios'

/**
 * Lighting control API
 */
export const lightingApi = {
  /**
   * Send a command to control an LED device
   * @param {string} deviceId - The device UUID
   * @param {object} command - The LED command
   * @param {boolean} command.on - Power state
   * @param {number[]} command.rgb - RGB color [r, g, b]
   * @param {number} command.brightness - Brightness percentage (0-100)
   * @param {number} command.color_temp - Color temperature in Kelvin
   * @param {string} command.mode - 'auto' or 'manual'
   */
  async sendLedCommand(deviceId, command) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/command`, command)
    return response.data
  },

  /**
   * Set LED to a specific scene/preset
   * @param {string} deviceId - The device UUID
   * @param {string} sceneName - Name of the scene
   */
  async setScene(deviceId, sceneName) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/scene`, { scene: sceneName })
    return response.data
  },

  /**
   * Turn LED on/off
   * @param {string} deviceId - The device UUID
   * @param {boolean} on - Power state
   */
  async setPower(deviceId, on) {
    const response = await apiClient.post(`/api/lighting/devices/${deviceId}/power`, { on })
    return response.data
  },

  /**
   * Set global mode (auto/manual) for a controller
   * @param {string} controllerId - The controller ID
   * @param {string} mode - 'auto' or 'manual'
   */
  async setMode(controllerId, mode) {
    const response = await apiClient.post(`/api/lighting/controllers/${controllerId}/mode`, { mode })
    return response.data
  },

  /**
   * Set global mode (auto/manual) for all lights
   * @param {string} mode - 'auto' or 'manual'
   */
  async setGlobalMode(mode) {
    const response = await apiClient.post('/api/lighting/mode', { mode })
    return response.data
  }
}
