import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'

vi.mock('../api/auth', () => ({
  authApi: {
    checkAuth: vi.fn(),
    getCurrentUser: vi.fn(),
    logout: vi.fn()
  }
}))

vi.mock('../utils/logger', () => ({
  default: {
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  }
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('initial state', () => {
    it('should have null user initially', () => {
      const store = useAuthStore()
      expect(store.user).toBeNull()
    })

    it('should not be authenticated initially', () => {
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(false)
    })

    it('should be loading initially', () => {
      const store = useAuthStore()
      expect(store.isLoading).toBe(true)
    })

    it('should have no error initially', () => {
      const store = useAuthStore()
      expect(store.error).toBeNull()
    })
  })

  describe('computed properties', () => {
    it('should return Guest when no user', () => {
      const store = useAuthStore()
      expect(store.userName).toBe('Guest')
    })

    it('should return empty string for email when no user', () => {
      const store = useAuthStore()
      expect(store.userEmail).toBe('')
    })

    it('should return user name when authenticated', () => {
      const store = useAuthStore()
      store.user = { name: 'John Doe', email: 'john@example.com', role: 'RESIDENT' }
      expect(store.userName).toBe('John Doe')
    })

    it('should correctly identify OWNER role', () => {
      const store = useAuthStore()
      store.user = { name: 'Admin', email: 'admin@example.com', role: 'OWNER' }
      expect(store.isOwner).toBe(true)
      expect(store.isResident).toBe(true)
    })

    it('should correctly identify RESIDENT role', () => {
      const store = useAuthStore()
      store.user = { name: 'User', email: 'user@example.com', role: 'RESIDENT' }
      expect(store.isOwner).toBe(false)
      expect(store.isResident).toBe(true)
    })

    it('should correctly identify GUEST role', () => {
      const store = useAuthStore()
      store.user = { name: 'Guest', email: 'guest@example.com', role: 'GUEST' }
      expect(store.isOwner).toBe(false)
      expect(store.isResident).toBe(false)
    })
  })

  describe('checkAuth()', () => {
    it('should fetch user when authenticated', async () => {
      const mockUser = {
        id: '123',
        name: 'Test User',
        email: 'test@example.com',
        role: 'RESIDENT'
      }

      authApi.checkAuth.mockResolvedValue(true)
      authApi.getCurrentUser.mockResolvedValue(mockUser)

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.isAuthenticated).toBe(true)
      expect(store.user).toEqual(mockUser)
      expect(store.isLoading).toBe(false)
    })

    it('should clear auth when not authenticated', async () => {
      authApi.checkAuth.mockResolvedValue(false)

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.isAuthenticated).toBe(false)
      expect(store.user).toBeNull()
      expect(store.isLoading).toBe(false)
    })

    it('should handle errors gracefully', async () => {
      authApi.checkAuth.mockRejectedValue(new Error('Network error'))

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.isAuthenticated).toBe(false)
      expect(store.user).toBeNull()
      expect(store.isLoading).toBe(false)
    })
  })

  describe('fetchCurrentUser()', () => {
    it('should set user data on success', async () => {
      const mockUser = {
        id: '456',
        name: 'Jane Doe',
        email: 'jane@example.com',
        role: 'OWNER',
        pictureUrl: 'https://example.com/photo.jpg'
      }

      authApi.getCurrentUser.mockResolvedValue(mockUser)

      const store = useAuthStore()
      await store.fetchCurrentUser()

      expect(store.user).toEqual(mockUser)
      expect(store.isAuthenticated).toBe(true)
      expect(store.error).toBeNull()
    })

    it('should clear auth and throw on failure', async () => {
      authApi.getCurrentUser.mockRejectedValue(new Error('Unauthorized'))

      const store = useAuthStore()

      await expect(store.fetchCurrentUser()).rejects.toThrow('Unauthorized')
      expect(store.user).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('clearAuth()', () => {
    it('should reset all auth state', () => {
      const store = useAuthStore()
      store.user = { name: 'Test', email: 'test@example.com', role: 'RESIDENT' }
      store.isAuthenticated = true
      store.error = 'Some error'

      store.clearAuth()

      expect(store.user).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(store.error).toBeNull()
    })
  })
})
