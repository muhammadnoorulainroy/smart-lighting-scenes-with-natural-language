/**
 * @fileoverview Vue Router configuration for the Smart Lighting application.
 *
 * Defines all application routes with:
 * - Lazy-loaded views for code splitting
 * - Authentication guards for protected routes
 * - Dynamic page titles
 * - Scroll behavior management
 *
 * Route structure:
 * - / (Home) - Public landing page
 * - /auth/callback - OAuth redirect handler
 * - /dashboard - Main app dashboard (requires auth)
 * - /rooms - Room management (requires auth)
 * - /scenes - Scene management (requires auth)
 * - /schedules - Schedule management (requires auth)
 * - /settings - System settings (requires owner)
 * - /* - 404 catch-all
 *
 * @module router
 */

import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { requireAuth, requireResident, requireOwner } from '../utils/routeGuards'

/**
 * Vue Router instance with HTML5 history mode.
 *
 * Features:
 * - HTML5 History API (no hash in URLs)
 * - Lazy loading for all views except Home
 * - Route-level navigation guards
 * - Automatic scroll restoration
 *
 * @type {Object}
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: false }
    },
    {
      path: '/auth/callback',
      name: 'auth-callback',
      component: () => import('../views/AuthCallbackView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true, requiresResident: true },
      beforeEnter: requireResident
    },
    {
      path: '/rooms',
      name: 'rooms',
      component: () => import('../views/RoomsView.vue'),
      meta: { requiresAuth: true },
      beforeEnter: requireAuth
    },
    {
      path: '/devices',
      name: 'devices',
      component: () => import('../views/DevicesView.vue'),
      meta: { requiresAuth: true },
      beforeEnter: requireAuth
    },
    {
      path: '/scenes',
      name: 'scenes',
      component: () => import('../views/ScenesView.vue'),
      meta: { requiresAuth: true },
      beforeEnter: requireAuth
    },
    {
      path: '/schedules',
      name: 'schedules',
      component: () => import('../views/SchedulesView.vue'),
      meta: { requiresAuth: true },
      beforeEnter: requireAuth
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/SettingsView.vue'),
      meta: { requiresAuth: true, requiresOwner: true, title: 'Settings' },
      beforeEnter: requireOwner
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { requiresAuth: false }
    }
  ],

  /**
   * Scroll behavior configuration.
   *
   * - Restores scroll position on back/forward navigation
   * - Scrolls to hash anchors with smooth animation
   * - Scrolls to top on new page navigation
   *
   * @param {Object} to - Target route
   * @param {Object} from - Source route
   * @param {Object} savedPosition - Saved position
   * @returns {Object} Target scroll position
   */
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  }
})

/**
 * Global navigation guard for page titles.
 *
 * Sets document title based on route meta.title,
 * falling back to the base app title.
 */
router.beforeEach((to, from, next) => {
  const baseTitle = 'Smart Lighting Scenes'
  document.title = to.meta.title ? `${to.meta.title} - ${baseTitle}` : baseTitle

  next()
})

export default router
