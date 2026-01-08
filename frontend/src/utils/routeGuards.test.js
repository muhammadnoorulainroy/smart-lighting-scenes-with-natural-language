import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { requireAuth, requireOwner, requireResident, requireGuest } from './routeGuards'
import { useAuthStore } from '../stores/auth'

vi.mock('../stores/auth', async () => {
  const actual = await vi.importActual('../stores/auth')
  return {
    ...actual,
    useAuthStore: vi.fn()
  }
})

describe('Route Guards', () => {
  let mockStore
  let mockNext
  let mockTo
  let mockFrom

  beforeEach(() => {
    setActivePinia(createPinia())

    mockStore = {
      isLoading: false,
      isAuthenticated: false,
      isOwner: false,
      isResident: false,
      checkAuth: vi.fn()
    }

    useAuthStore.mockReturnValue(mockStore)

    mockNext = vi.fn()
    mockTo = { path: '/dashboard', fullPath: '/dashboard' }
    mockFrom = { path: '/' }
  })

  describe('requireAuth', () => {
    it('should allow authenticated users', async () => {
      mockStore.isAuthenticated = true

      await requireAuth(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith()
    })

    it('should redirect unauthenticated users to home', async () => {
      mockStore.isAuthenticated = false

      await requireAuth(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith({
        path: '/',
        query: { redirect: '/dashboard', requiresAuth: 'true' }
      })
    })

    it('should check auth if still loading', async () => {
      mockStore.isLoading = true
      mockStore.checkAuth.mockResolvedValue()

      await requireAuth(mockTo, mockFrom, mockNext)

      expect(mockStore.checkAuth).toHaveBeenCalled()
    })
  })

  describe('requireOwner', () => {
    it('should allow owners', async () => {
      mockStore.isAuthenticated = true
      mockStore.isOwner = true

      await requireOwner(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith()
    })

    it('should redirect non-owners to rooms', async () => {
      mockStore.isAuthenticated = true
      mockStore.isOwner = false

      await requireOwner(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith({
        path: '/rooms',
        replace: true
      })
    })

    it('should redirect unauthenticated to home', async () => {
      mockStore.isAuthenticated = false
      mockStore.isOwner = false

      await requireOwner(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith({
        path: '/',
        query: { redirect: '/dashboard', requiresAuth: 'true' }
      })
    })
  })

  describe('requireResident', () => {
    it('should allow residents', async () => {
      mockStore.isAuthenticated = true
      mockStore.isResident = true

      await requireResident(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith()
    })

    it('should allow owners (they are also residents)', async () => {
      mockStore.isAuthenticated = true
      mockStore.isResident = true
      mockStore.isOwner = true

      await requireResident(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith()
    })

    it('should redirect non-residents to rooms', async () => {
      mockStore.isAuthenticated = true
      mockStore.isResident = false

      await requireResident(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith({
        path: '/rooms',
        replace: true
      })
    })
  })

  describe('requireGuest', () => {
    it('should allow unauthenticated users', async () => {
      mockStore.isAuthenticated = false

      await requireGuest(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith()
    })

    it('should redirect authenticated users to dashboard', async () => {
      mockStore.isAuthenticated = true

      await requireGuest(mockTo, mockFrom, mockNext)

      expect(mockNext).toHaveBeenCalledWith({
        path: '/dashboard',
        replace: true
      })
    })
  })
})
