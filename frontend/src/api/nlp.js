import apiClient from './axios'

/**
 * NLP API for natural language command processing
 */
export const nlpApi = {
  /**
   * Parse a natural language command without executing
   * Returns preview of what will happen
   */
  async parse(text) {
    const response = await apiClient.post('/api/nlp/parse', { text })
    return response.data
  },

  /**
   * Execute a natural language command
   */
  async execute(text) {
    const response = await apiClient.post('/api/nlp/execute', { text })
    return response.data
  },

  /**
   * Confirm and execute a previously parsed command
   */
  async confirm(parsedCommand) {
    const response = await apiClient.post('/api/nlp/confirm', parsedCommand)
    return response.data
  },

  /**
   * Apply a conflict resolution
   * @param {string} scheduleId - The ID of the schedule
   * @param {string} resolutionId - The ID of the resolution to apply
   * @param {object} params - Additional parameters for the resolution
   */
  async resolveConflict(scheduleId, resolutionId, params = {}) {
    const response = await apiClient.post('/api/nlp/resolve-conflict', {
      scheduleId,
      resolutionId,
      params
    })
    return response.data
  }
}
