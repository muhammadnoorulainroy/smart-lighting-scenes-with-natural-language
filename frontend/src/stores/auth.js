import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import logger from '../utils/logger'

const MODULE = 'AuthStore'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const isAuthenticated = ref(false)
  const isLoading = ref(true)
  const error = ref(null)

  const userName = computed(() => user.value?.name || 'Guest')
  const userEmail = computed(() => user.value?.email || '')
  const userPicture = computed(() => user.value?.pictureUrl || '')
  const userRole = computed(() => user.value?.role || 'GUEST')
  const isOwner = computed(() => userRole.value === 'OWNER')
  const isResident = computed(() => userRole.value === 'RESIDENT' || userRole.value === 'OWNER')

  const checkAuth = async () => {
    try {
      isLoading.value = true
      error.value = null
      logger.debug(MODULE, 'Checking authentication status')

      const authenticated = await authApi.checkAuth()

      if (authenticated) {
        logger.debug(MODULE, 'User is authenticated, fetching profile')
        await fetchCurrentUser()
      } else {
        logger.info(MODULE, 'User is not authenticated')
        clearAuth()
      }
    } catch (err) {
      logger.error(MODULE, 'Failed to check authentication', err)
      clearAuth()
    } finally {
      isLoading.value = false
    }
  }

  const fetchCurrentUser = async () => {
    try {
      logger.debug(MODULE, 'Fetching current user profile')
      const userData = await authApi.getCurrentUser()
      user.value = userData
      isAuthenticated.value = true
      error.value = null
      logger.info(MODULE, `User authenticated: ${userData.email} (${userData.role})`)
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch user profile', err)
      clearAuth()
      throw err
    }
  }

  const login = () => {
    logger.info(MODULE, 'Initiating OAuth login redirect')
    window.location.href = `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/oauth2/authorization/google`
  }

  const logout = async () => {
    try {
      logger.info(MODULE, 'User logging out')
      await authApi.logout()
      clearAuth()
      logger.info(MODULE, 'Logout successful')
      window.location.href = '/'
    } catch (err) {
      logger.error(MODULE, 'Logout failed', err)
      error.value = 'Failed to logout'
      logger.warn(MODULE, 'Clearing auth state despite logout failure')
      clearAuth()
      window.location.href = '/'
    }
  }

  const clearAuth = () => {
    logger.debug(MODULE, 'Clearing authentication state')
    user.value = null
    isAuthenticated.value = false
    error.value = null
  }

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    error,
    // Computed
    userName,
    userEmail,
    userPicture,
    userRole,
    isOwner,
    isResident,
    // Actions
    checkAuth,
    fetchCurrentUser,
    login,
    logout,
    clearAuth
  }
})
