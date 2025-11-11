<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <div class="text-center">
      <LoadingSpinner size="lg" message="Completing authentication..." />
      
      <div v-if="error" class="mt-8 max-w-md mx-auto">
        <ErrorAlert 
          type="error"
          title="Authentication Failed"
          :message="error"
          @dismiss="goHome"
        />
        
        <button @click="goHome" class="btn btn-primary mt-4">
          Return Home
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
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
    
    // Wait a bit for the backend to set the session cookie
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // Fetch user data
    await authStore.checkAuth()
    
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






