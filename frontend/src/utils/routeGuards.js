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

export const requireResident = async (to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoading) await authStore.checkAuth()
  if (authStore.isAuthenticated && authStore.isResident) {
    next()
  } else if (authStore.isAuthenticated) {
    next({ path: '/dashboard', replace: true })
  } else {
    next({ path: '/', query: { redirect: to.fullPath, requiresAuth: 'true' } })
  }
}

export const requireOwner = async (to, from, next) => {
  const authStore = useAuthStore()
  if (authStore.isLoading) await authStore.checkAuth()
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






