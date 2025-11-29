/**
 * @fileoverview Vue Router navigation guards for authentication.
 * @module utils/routeGuards
 * @author Smart Lighting Team
 * @version 1.0.0
 */

import { useAuthStore } from '../stores/auth'

/**
 * Route guard requiring authentication.
 * Redirects unauthenticated users to home with redirect query.
 *
 * @param {Object} to - Target route
 * @param {Object} from - Current route
 * @param {Function} next - Navigation resolver
 * @returns {Promise<void>}
 */
export const requireAuth = async (to, from, next) => {
  const authStore = useAuthStore()

  // Wait for auth check if still loading
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }

  if (authStore.isAuthenticated) {
    next()
  } else {
    next({
      path: '/',
      query: { redirect: to.fullPath, requiresAuth: 'true' }
    })
  }
}

/**
 * Route guard requiring RESIDENT or OWNER role.
 * Redirects guests to home, unauthorized users to dashboard.
 *
 * @param {Object} to - Target route
 * @param {Object} from - Current route
 * @param {Function} next - Navigation resolver
 * @returns {Promise<void>}
 */
export const requireResident = async (to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }
  if (authStore.isAuthenticated && authStore.isResident) {
    next()
  } else if (authStore.isAuthenticated) {
    next({ path: '/dashboard', replace: true })
  } else {
    next({ path: '/', query: { redirect: to.fullPath, requiresAuth: 'true' } })
  }
}

/**
 * Route guard requiring OWNER role.
 * Redirects non-owners to dashboard, guests to home.
 *
 * @param {Object} to - Target route
 * @param {Object} from - Current route
 * @param {Function} next - Navigation resolver
 * @returns {Promise<void>}
 */
export const requireOwner = async (to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }
  if (authStore.isAuthenticated && authStore.isOwner) {
    next()
  } else if (authStore.isAuthenticated) {
    next({ path: '/dashboard', replace: true })
  } else {
    next({ path: '/', query: { redirect: to.fullPath, requiresAuth: 'true' } })
  }
}

/**
 * Route guard for guest-only pages (e.g., login page)
 * Redirects to dashboard if already authenticated
 */
export const requireGuest = async (to, from, next) => {
  const authStore = useAuthStore()

  // Wait for auth check if still loading
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }

  if (!authStore.isAuthenticated) {
    next()
  } else {
    next({ path: '/dashboard', replace: true })
  }
}
