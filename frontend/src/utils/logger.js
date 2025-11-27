const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3
}

const currentLevel = import.meta.env.PROD ? LOG_LEVELS.INFO : LOG_LEVELS.DEBUG

const formatMessage = (level, module, message, data) => {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [${level}] [${module}]`
  return { prefix, message, data }
}

const logger = {
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

