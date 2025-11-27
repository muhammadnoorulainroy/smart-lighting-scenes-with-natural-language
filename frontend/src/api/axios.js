import axios from 'axios'
import logger from '../utils/logger'

const MODULE = 'ApiClient'
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json'
  }
})

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
