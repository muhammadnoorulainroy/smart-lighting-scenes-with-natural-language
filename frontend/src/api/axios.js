/**
 * @fileoverview Axios HTTP client configuration with interceptors.
 *
 * This module provides a pre-configured Axios instance for all API communication
 * with the backend. It handles:
 * - Base URL configuration (supports both Docker and local development)
 * - Session cookies for authentication
 * - Request/response logging for debugging
 * - Centralized error handling with status-specific messages
 *
 * @module api/axios
 */

import axios from 'axios'
import logger from '../utils/logger'

/** @constant {string} Module name for logging */
const MODULE = 'ApiClient'

/**
 * Base URL for API requests.
 *
 * - In Docker/production: Uses empty string for relative URLs (Nginx proxy)
 * - In development: Uses VITE_API_URL env variable
 *
 * @constant {string}
 */
const API_BASE_URL = import.meta.env.VITE_API_URL ?? ''

/**
 * Pre-configured Axios instance for API communication.
 *
 * Features:
 * - Automatic JSON content type headers
 * - Credentials included for session cookie authentication
 * - Request logging in development mode
 * - Centralized error handling with user-friendly messages
 *
 * @type {Object}
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json'
  }
})

/**
 * Request interceptor for logging outgoing requests.
 * Logs the HTTP method and URL for debugging purposes.
 */
apiClient.interceptors.request.use(
  config => {
    logger.debug(MODULE, `${config.method?.toUpperCase()} ${config.url}`)
    return config
  },
  error => {
    logger.error(MODULE, 'Request configuration error', error)
    return Promise.reject(error)
  }
)

/**
 * Response interceptor for handling responses and errors.
 *
 * Success responses are logged and passed through.
 * Error responses are categorized by status code:
 * - 401: Unauthorized
 * - 403: Forbidden
 * - 404: Resource not found
 * - 500: Server error (logged with full details)
 */
apiClient.interceptors.response.use(
  response => {
    logger.debug(MODULE, `Response ${response.status} from ${response.config.url}`)
    return response
  },
  error => {
    if (error.response) {
      const { status, config } = error.response
      switch (status) {
        case 401:
          logger.warn(MODULE, `Unauthorized: ${config.url}`)
          break
        case 403:
          logger.warn(MODULE, `Forbidden: ${config.url}`)
          break
        case 404:
          logger.warn(MODULE, `Not found: ${config.url}`)
          break
        case 500:
          logger.error(MODULE, `Server error: ${config.url}`, error.response.data)
          break
        default:
          logger.error(MODULE, `API error ${status}: ${config.url}`)
      }
    } else if (error.request) {
      logger.error(MODULE, 'No response from server - network error or timeout')
    } else {
      logger.error(MODULE, 'Request error', error.message)
    }

    return Promise.reject(error)
  }
)

export default apiClient
