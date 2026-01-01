/**
 * @fileoverview Authentication API client for OAuth and local auth.
 * @module api/auth
 * @author Smart Lighting Team
 * @version 1.0.0
 */

import apiClient from './axios'

/**
 * Authentication API methods.
 * @namespace
 */
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
   * Register a new user with email and password
   * @param {string} email
   * @param {string} password
   * @param {string} name
   * @returns {Promise<Object>} User data
   */
  async signup(email, password, name) {
    const response = await apiClient.post('/api/auth/signup', {
      email,
      password,
      name
    })
    return response.data
  },

  /**
   * Login with email and password
   * @param {string} email
   * @param {string} password
   * @returns {Promise<Object>} User data
   */
  async login(email, password) {
    const response = await apiClient.post('/api/auth/login', {
      email,
      password
    })
    return response.data
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
