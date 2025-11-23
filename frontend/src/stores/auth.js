import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref(null)
  const isAuthenticated = ref(false)
  const isLoading = ref(true)
  const error = ref(null)

  // Computed
  const userName = computed(() => user.value?.name || 'Guest')
  const userEmail = computed(() => user.value?.email || '')
  const userPicture = computed(() => user.value?.pictureUrl || '')
  const userRole = computed(() => user.value?.role || 'GUEST')
  const isOwner = computed(() => userRole.value === 'OWNER')
  const isResident = computed(() => userRole.value === 'RESIDENT' || userRole.value === 'OWNER')

  // Actions
  const checkAuth = async () => {
    try {
      isLoading.value = true
      error.value = null
      
      const authenticated = await authApi.checkAuth()
      
      if (authenticated) {
        await fetchCurrentUser()
      } else {
        clearAuth()
      }
    } catch (err) {
      console.error('Failed to check authentication:', err)
      clearAuth()
    } finally {
      isLoading.value = false
    }
  }

  const fetchCurrentUser = async () => {
    try {
      const userData = await authApi.getCurrentUser()
      user.value = userData
      isAuthenticated.value = true
      error.value = null
    } catch (err) {
      console.error('Failed to fetch user:', err)
      clearAuth()
      throw err
    }
  }

  const login = () => {
    // Redirect to Google OAuth
    window.location.href = `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/oauth2/authorization/google`
  }

  const logout = async () => {
    try {
      await authApi.logout()
      clearAuth()
      window.location.href = '/'
    } catch (err) {
      console.error('Logout failed:', err)
      error.value = 'Failed to logout'
      // Clear auth anyway
      clearAuth()
      window.location.href = '/'
    }
  }

  const clearAuth = () => {
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






