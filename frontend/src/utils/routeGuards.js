import { useAuthStore } from '../stores/auth'

/**
 * Route guard to check if user is authenticated
 * Redirects to home if not authenticated
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
 * Route guard to check if user is a resident or admin
 */
export const requireResident = async (to, from, next) => {
  const authStore = useAuthStore()
  
  // Wait for auth check if still loading
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }
  
  if (authStore.isAuthenticated && authStore.isResident) {
    next()
  } else if (authStore.isAuthenticated) {
    next({ path: '/dashboard', replace: true })
  } else {
    next({
      path: '/',
      query: { redirect: to.fullPath, requiresAuth: 'true' }
    })
  }
}

/**
 * Route guard to check if user is an admin
 */
export const requireAdmin = async (to, from, next) => {
  const authStore = useAuthStore()
  
  // Wait for auth check if still loading
  if (authStore.isLoading) {
    await authStore.checkAuth()
  }
  
  if (authStore.isAuthenticated && authStore.isAdmin) {
    next()
  } else if (authStore.isAuthenticated) {
    next({ path: '/dashboard', replace: true })
  } else {
    next({
      path: '/',
      query: { redirect: to.fullPath, requiresAuth: 'true' }
    })
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






