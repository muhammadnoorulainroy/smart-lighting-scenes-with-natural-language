/**
 * @fileoverview Structured logging utility for the Smart Lighting frontend.
 *
 * Provides consistent logging with:
 * - ISO timestamps
 * - Log level prefixes
 * - Module/component context
 * - Environment-aware filtering (DEBUG only in dev)
 *
 * @module utils/logger
 */

/**
 * Log level constants for filtering messages.
 * Lower numbers = more verbose.
 *
 * @constant {Object.<string, number>}
 */
const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3
}

/**
 * Current log level based on environment.
 *
 * - Production: INFO and above
 * - Development: DEBUG and above
 *
 * @constant {number}
 */
const currentLevel = import.meta.env.PROD ? LOG_LEVELS.INFO : LOG_LEVELS.DEBUG

/**
 * Formats a log message with timestamp, level, and module prefix.
 *
 * @param {string} level - Log level (DEBUG, INFO, WARN, ERROR)
 * @param {string} module - Module/component name for context
 * @param {string} message - The log message
 * @param {*} data - Optional additional data
 * @returns {{prefix: string, message: string, data: *}} Formatted parts
 * @private
 */
const formatMessage = (level, module, message, data) => {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [${level}] [${module}]`
  return { prefix, message, data }
}

/**
 * Logger instance with methods for each log level.
 *
 * Usage pattern:
 * 1. Define a MODULE constant at the top of your file
 * 2. Call logger methods with MODULE as first argument
 * 3. Include optional data object for structured logging
 *
 * @namespace logger
 */
const logger = {
  /**
   * Logs a debug message.
   *
   * Only shown in development mode. Use for detailed
   * troubleshooting information that would clutter production logs.
   *
   * @param {string} module - Module/component name
   * @param {string} message - Log message
   * @param {*} [data=null] - Optional data to log
   */
  debug(module, message, data = null) {
    if (currentLevel <= LOG_LEVELS.DEBUG) {
      const { prefix } = formatMessage('DEBUG', module, message, data)
      if (data) {
        console.debug(`${prefix} ${message}`, data)
      } else {
        console.debug(`${prefix} ${message}`)
      }
    }
  },

  /**
   * Logs an info message.
   *
   * Shown in both production and development.
   * Use for significant events like user actions or state changes.
   *
   * @param {string} module - Module/component name
   * @param {string} message - Log message
   * @param {*} [data=null] - Optional data to log
   */
  info(module, message, data = null) {
    if (currentLevel <= LOG_LEVELS.INFO) {
      const { prefix } = formatMessage('INFO', module, message, data)
      if (data) {
        console.info(`${prefix} ${message}`, data)
      } else {
        console.info(`${prefix} ${message}`)
      }
    }
  },

  /**
   * Logs a warning message.
   *
   * Use for potentially problematic situations that don't
   * prevent operation but should be noted.
   *
   * @param {string} module - Module/component name
   * @param {string} message - Log message
   * @param {*} [data=null] - Optional data to log
   */
  warn(module, message, data = null) {
    if (currentLevel <= LOG_LEVELS.WARN) {
      const { prefix } = formatMessage('WARN', module, message, data)
      if (data) {
        console.warn(`${prefix} ${message}`, data)
      } else {
        console.warn(`${prefix} ${message}`)
      }
    }
  },

  /**
   * Logs an error message.
   *
   * Use for errors that affect functionality.
   * Always logged regardless of environment.
   *
   * @param {string} module - Module/component name
   * @param {string} message - Log message
   * @param {Error|*} [error=null] - Error object or additional data
   */
  error(module, message, error = null) {
    if (currentLevel <= LOG_LEVELS.ERROR) {
      const { prefix } = formatMessage('ERROR', module, message, error)
      if (error) {
        console.error(`${prefix} ${message}`, error)
      } else {
        console.error(`${prefix} ${message}`)
      }
    }
  }
}

export default logger