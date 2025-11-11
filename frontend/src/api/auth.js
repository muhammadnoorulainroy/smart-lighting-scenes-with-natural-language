import apiClient from './axios'

export const authApi = {
  /**
   * Check if user is authenticated
   * @returns {Promise<boolean>}
   */
  async checkAuth() {
    try {
      const response = await apiClient.get('/api/auth/check')
      return response.data === true
    } catch (error) {
      console.error('Auth check failed:', error)
      return false
    }
  },

  /**
   * Get current user information
   * @returns {Promise<Object>}
   */
  async getCurrentUser() {
    try {
      const response = await apiClient.get('/api/me')
      return response.data
    } catch (error) {
      console.error('Failed to get current user:', error)
      throw error
    }
  },

  /**
   * Logout current user
   * @returns {Promise<void>}
   */
  async logout() {
    try {
      await apiClient.post('/api/auth/logout')
    } catch (error) {
      console.error('Logout failed:', error)
      throw error
    }
  }
}






