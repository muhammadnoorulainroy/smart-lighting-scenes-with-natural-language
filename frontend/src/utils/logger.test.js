import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import logger from './logger'

describe('Logger Utility', () => {
  let consoleSpy

  beforeEach(() => {
    consoleSpy = {
      debug: vi.spyOn(console, 'debug').mockImplementation(() => {}),
      info: vi.spyOn(console, 'info').mockImplementation(() => {}),
      warn: vi.spyOn(console, 'warn').mockImplementation(() => {}),
      error: vi.spyOn(console, 'error').mockImplementation(() => {})
    }
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('debug()', () => {
    it('should call console.debug with formatted message', () => {
      logger.debug('TestModule', 'Debug message')

      expect(consoleSpy.debug).toHaveBeenCalled()
      const call = consoleSpy.debug.mock.calls[0][0]
      expect(call).toContain('[DEBUG]')
      expect(call).toContain('[TestModule]')
      expect(call).toContain('Debug message')
    })

    it('should include data when provided', () => {
      const data = { key: 'value' }
      logger.debug('TestModule', 'With data', data)

      expect(consoleSpy.debug).toHaveBeenCalledWith(
        expect.stringContaining('With data'),
        data
      )
    })
  })

  describe('info()', () => {
    it('should call console.info with formatted message', () => {
      logger.info('AuthModule', 'User logged in')

      expect(consoleSpy.info).toHaveBeenCalled()
      const call = consoleSpy.info.mock.calls[0][0]
      expect(call).toContain('[INFO]')
      expect(call).toContain('[AuthModule]')
      expect(call).toContain('User logged in')
    })

    it('should include timestamp in message', () => {
      logger.info('TestModule', 'Test message')

      const call = consoleSpy.info.mock.calls[0][0]
      expect(call).toMatch(/\[\d{4}-\d{2}-\d{2}T/)
    })
  })

  describe('warn()', () => {
    it('should call console.warn with formatted message', () => {
      logger.warn('ApiModule', 'Rate limit approaching')

      expect(consoleSpy.warn).toHaveBeenCalled()
      const call = consoleSpy.warn.mock.calls[0][0]
      expect(call).toContain('[WARN]')
      expect(call).toContain('[ApiModule]')
      expect(call).toContain('Rate limit approaching')
    })
  })

  describe('error()', () => {
    it('should call console.error with formatted message', () => {
      logger.error('ApiModule', 'Request failed')

      expect(consoleSpy.error).toHaveBeenCalled()
      const call = consoleSpy.error.mock.calls[0][0]
      expect(call).toContain('[ERROR]')
      expect(call).toContain('[ApiModule]')
      expect(call).toContain('Request failed')
    })

    it('should include error object when provided', () => {
      const error = new Error('Network error')
      logger.error('ApiModule', 'Request failed', error)

      expect(consoleSpy.error).toHaveBeenCalledWith(
        expect.stringContaining('Request failed'),
        error
      )
    })
  })

  describe('message formatting', () => {
    it('should format all log levels consistently', () => {
      const module = 'TestModule'
      const message = 'Test message'

      logger.debug(module, message)
      logger.info(module, message)
      logger.warn(module, message)
      logger.error(module, message)

      const levels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
      const spies = [consoleSpy.debug, consoleSpy.info, consoleSpy.warn, consoleSpy.error]

      spies.forEach((spy, index) => {
        const call = spy.mock.calls[0][0]
        expect(call).toContain(`[${levels[index]}]`)
        expect(call).toContain(`[${module}]`)
        expect(call).toContain(message)
      })
    })
  })
})

