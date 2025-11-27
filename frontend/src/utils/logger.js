/**
 * @fileoverview Structured logging utility for the Smart Lighting frontend.
 * @module utils/logger
 * @author Smart Lighting Team
 * @version 1.0.0
 */

/**
 * Log level constants for filtering messages.
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
 * Production uses INFO, development uses DEBUG.
 * @type {number}
 */
const currentLevel = import.meta.env.PROD ? LOG_LEVELS.INFO : LOG_LEVELS.DEBUG

/**
 * Formats a log message with timestamp, level, and module prefix.
 * @param {string} level - The log level (DEBUG, INFO, WARN, ERROR)
 * @param {string} module - The module/component name
 * @param {string} message - The log message
 * @param {*} data - Optional additional data
 * @returns {{prefix: string, message: string, data: *}} Formatted message parts
 * @private
 */
const formatMessage = (level, module, message, data) => {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [${level}] [${module}]`
  return { prefix, message, data }
}

/**
 * Logger instance with methods for each log level.
 * @namespace
 */
const logger = {
  /**
   * Logs a debug message. Only shown in development.
   * @param {string} module - The module/component name
   * @param {string} message - The log message
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
   * Logs an info message. Shown in production and development.
   * @param {string} module - The module/component name
   * @param {string} message - The log message
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
   * @param {string} module - The module/component name
   * @param {string} message - The log message
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
   * @param {string} module - The module/component name
   * @param {string} message - The log message
   * @param {Error|*} [error=null] - Optional error object or data
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

