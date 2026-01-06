/**
 * @fileoverview Vue Router navigation guards for authentication.
 *
 * Provides route protection based on authentication and user roles:
 * - requireAuth: Any authenticated user
 * - requireResident: RESIDENT or OWNER role
 * - requireOwner: OWNER role only
 * - requireGuest: Unauthenticated users only
 *
 * Guards wait for auth check to complete before making decisions,
 * ensuring proper behavior on page refresh.
 *
 * @module utils/routeGuards
 */

import { useAuthStore } from '../stores/auth'

/**
 * Route guard requiring any authenticated user.
 *
 * Redirects unauthenticated users to home page with redirect query
 * so they return to their intended destination after login.
 *
 * @async
 * @param {Object} to - Target route
 * @param {Object} from - Source route
 * @param {Function} next - Navigation resolver
 * @returns {Promise<void>}
 */
export const requireAuth = async (to, from, next) => {
  const authStore = useAuthStore()

  // Wait for initial auth check to complete
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }

  if (authStore.isAuthenticated) {
    next()
  } else {
    // Redirect to home with return path
    next({
      path: '/',
      query: { redirect: to.fullPath, requiresAuth: 'true' }
    })
  }
}

/**
 * Route guard requiring RESIDENT or OWNER role.
 *
 * Use for routes that require ability to manage resources
 * (create scenes, modify schedules, etc.)
 *
 * Redirects:
 * - Unauthenticated -> Home
 * - GUEST role -> Rooms (view only)
 *
 * @async
 * @param {Object} to - Target route
 * @param {Object} from - Source route
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
    // Authenticated but GUEST - redirect to rooms (view only)
    next({ path: '/rooms', replace: true })
  } else {
    // Not authenticated - go to home
    next({ path: '/', query: { redirect: to.fullPath, requiresAuth: 'true' } })
  }
}

/**
 * Route guard requiring OWNER role.
 *
 * Use for administrative routes like user management,
 * system settings, and dangerous operations.
 *
 * Redirects:
 * - Unauthenticated -> Home
 * - RESIDENT/GUEST -> Rooms
 *
 * @async
 * @param {Object} to - Target route
 * @param {Object} from - Source route
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
    // Authenticated but not owner - go to rooms
    next({ path: '/rooms', replace: true })
  } else {
    // Not authenticated - go to home
    next({ path: '/', query: { redirect: to.fullPath, requiresAuth: 'true' } })
  }
}

/**
 * Route guard for guest-only pages.
 *
 * Use for login/signup pages that should redirect
 * away if already authenticated.
 *
 * Redirects authenticated users to dashboard.
 *
 * @async
 * @param {Object} to - Target route
 * @param {Object} from - Source route
 * @param {Function} next - Navigation resolver
 * @returns {Promise<void>}
 */
export const requireGuest = async (to, from, next) => {
  const authStore = useAuthStore()

  if (authStore.isLoading) {
    await authStore.checkAuth()
  }

  if (!authStore.isAuthenticated) {
    next()
  } else {
    // Already logged in - go to dashboard
    next({ path: '/dashboard', replace: true })
  }
}
