/**
 * @fileoverview Axios HTTP client configuration with interceptors.
 *
 * This module provides a pre-configured Axios instance for all API communication
 * with the backend. It handles:
 * - Base URL configuration (supports both Docker and local development)
 * - JWT token authentication for cross-domain requests
 * - Session cookies for same-domain authentication
 * - Request/response logging for debugging
 * - Centralized error handling with status-specific messages
 *
 * @module api/axios
 */

import axios from 'axios'
import logger from '../utils/logger'

/** @constant {string} Module name for logging */
const MODULE = 'ApiClient'

/** @constant {string} LocalStorage key for JWT token */
const TOKEN_KEY = 'smart_lighting_token'

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
 * Get stored JWT token from localStorage.
 * @returns {string|null} The stored token or null
 */
export const getStoredToken = () => localStorage.getItem(TOKEN_KEY)

/**
 * Store JWT token in localStorage.
 * @param {string} token - The JWT token to store
 */
export const setStoredToken = token => localStorage.setItem(TOKEN_KEY, token)

/**
 * Remove JWT token from localStorage.
 */
export const clearStoredToken = () => localStorage.removeItem(TOKEN_KEY)

/**
 * Pre-configured Axios instance for API communication.
 *
 * Features:
 * - Automatic JSON content type headers
 * - JWT token in Authorization header (cross-domain)
 * - Credentials included for session cookie authentication (same-domain)
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
 * Request interceptor for adding JWT token and logging.
 * Adds Authorization header if token exists.
 * Logs the HTTP method and URL for debugging purposes.
 */
apiClient.interceptors.request.use(
  config => {
    // Add JWT token to Authorization header if available
    const token = getStoredToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
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
