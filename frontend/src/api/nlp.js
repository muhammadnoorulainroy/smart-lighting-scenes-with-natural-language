/**
 * @fileoverview API client for natural language command processing.
 *
 * Enables voice/text-based control of the lighting system using
 * OpenAI for command interpretation. Commands can:
 * - Control lights ("dim the bedroom to 50%")
 * - Apply scenes ("set movie mode in living room")
 * - Create schedules ("turn off all lights at 11pm daily")
 *
 * The NLP flow is:
 * 1. Parse: Send text, get preview of what will happen
 * 2. Confirm: User reviews and confirms
 * 3. Execute: Command is applied
 *
 * @module api/nlp
 */

import apiClient from './axios'

/**
 * NLP API for natural language command processing.
 *
 * Uses OpenAI's GPT models to interpret commands into structured actions.
 * Requires OPENAI_API_KEY to be configured on the backend.
 *
 * @namespace nlpApi
 */
export const nlpApi = {
  /**
   * Parses a natural language command without executing it.
   *
   * Returns a preview of what the command will do, including:
   * - Parsed intent (what action will be taken)
   * - Target (which lights/rooms are affected)
   * - Parameters (brightness, color, etc.)
   * - Confidence score
   *
   * For scheduled commands, also checks for conflicts with existing schedules.
   *
   * @async
   * @param {string} text - Natural language command
   * @returns {Promise<Object>} Parsed command preview
   * @property {string} text - Original command text
   * @property {Object} parsed - Structured command data
   * @property {string} parsed.intent - Action type (light.on, light.brightness, scene.apply, etc.)
   * @property {string} parsed.target - Target room or 'all'
   * @property {Object} [parsed.params] - Command parameters
   * @property {Object} [parsed.schedule] - Schedule configuration if time-based
   * @property {string} preview - Human-readable description of what will happen
   * @property {boolean} valid - Whether the command is valid and executable
   * @property {string} [error] - Error message if invalid
   * @property {boolean} isScheduled - Whether this creates a schedule
   * @property {Object} [conflictAnalysis] - Schedule conflict information
   */
  async parse(text) {
    const response = await apiClient.post('/api/nlp/parse', { text })
    return response.data
  },

  /**
   * Executes a natural language command immediately.
   *
   * Parses and executes in one step. Use this for quick commands
   * when preview/confirmation isn't needed.
   *
   * @async
   * @param {string} text - Natural language command
   * @returns {Promise<Object>} Execution result
   * @property {boolean} executed - Whether command was executed
   * @property {string} result - Execution result message
   */
  async execute(text) {
    const response = await apiClient.post('/api/nlp/execute', { text })
    return response.data
  },

  /**
   * Confirms and executes a previously parsed command.
   *
   * Use this after showing the user a preview from parse().
   * The parsed command object is sent back for execution.
   *
   * @async
   * @param {Object} parsedCommand - Command object from parse()
   * @returns {Promise<Object>} Execution result
   * @property {boolean} executed - Whether command was executed
   * @property {string} result - Execution result message
   */
  async confirm(parsedCommand) {
    const response = await apiClient.post('/api/nlp/confirm', parsedCommand)
    return response.data
  },

  /**
   * Applies a resolution to a schedule conflict.
   *
   * When creating schedules via NLP, conflicts with existing schedules
   * are detected. This method applies one of the suggested resolutions.
   *
   * Resolution types:
   * - adjust_time: Move the new schedule to avoid overlap
   * - disable: Disable one of the conflicting schedules
   * - delete: Delete one of the conflicting schedules
   * - merge: Combine into a single schedule
   *
   * @async
   * @param {string} scheduleId - Schedule UUID
   * @param {string} resolutionId - Resolution ID (from conflict analysis)
   * @param {Object} [params={}] - Additional resolution parameters
   * @returns {Promise<Object>} Resolution result
   * @property {string} message - Result message
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
