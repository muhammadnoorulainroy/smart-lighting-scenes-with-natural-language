<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <div class="text-center">
      <LoadingSpinner size="lg" message="Completing authentication..." />

      <div v-if="error" class="mt-8 max-w-md mx-auto">
        <ErrorAlert type="error" title="Authentication Failed" :message="error" @dismiss="goHome" />

        <button class="btn btn-primary mt-4" @click="goHome">Return Home</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import apiClient from '../api/axios'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const error = ref('')

onMounted(async () => {
  try {
    // Check if there's an error in the URL
    if (route.query.error) {
      error.value = route.query.error_description || 'Authentication failed. Please try again.'
      return
    }

    // Check for JWT token in URL (cross-domain OAuth)
    const { token } = route.query
    if (token) {
      console.log('Exchanging OAuth token for session...')
      try {
        // Exchange token for session
        const { data } = await apiClient.post('/api/auth/token', { token: decodeURIComponent(token) })
        if (data?.id) {
          // Token exchange successful, refresh auth state
          await authStore.checkAuth()
        }
      } catch (tokenError) {
        console.error('Token exchange failed:', tokenError)
        error.value = 'Failed to complete authentication. Please try again.'
        return
      }
    } else if (route.query.success) {
      // Legacy: wait for session cookie (same-domain)
      await new Promise(resolve => setTimeout(resolve, 1000))
      await authStore.checkAuth()
    }

    if (authStore.isAuthenticated) {
      // Redirect to the intended page or dashboard
      const redirect = route.query.redirect || '/dashboard'
      router.push(redirect)
    } else {
      error.value = 'Failed to authenticate. Please try again.'
    }
  } catch (err) {
    console.error('Auth callback error:', err)
    error.value = 'An error occurred during authentication. Please try again.'
  }
})

const goHome = () => {
  router.push('/')
}
</script>
