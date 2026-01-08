<template>
  <transition
    enter-active-class="transition ease-out duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition ease-in duration-200"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="show"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <transition
        enter-active-class="transition ease-out duration-300"
        enter-from-class="opacity-0 transform scale-95"
        enter-to-class="opacity-100 transform scale-100"
        leave-active-class="transition ease-in duration-200"
        leave-from-class="opacity-100 transform scale-100"
        leave-to-class="opacity-0 transform scale-95"
      >
        <div v-if="show" class="card p-8 max-w-md w-full relative animate-slide-up">
          <!-- Close button -->
          <button
            class="absolute top-4 right-4 p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
            aria-label="Close"
            @click="$emit('close')"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>

          <!-- Icon -->
          <div class="flex justify-center mb-4">
            <div
              class="w-16 h-16 bg-gradient-to-br from-primary-400 to-accent-500 rounded-full flex items-center justify-center"
            >
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                />
              </svg>
            </div>
          </div>

          <!-- Title -->
          <h2
            class="text-2xl font-display font-bold text-center mb-2 text-neutral-900 dark:text-neutral-100"
          >
            {{ isSignup ? 'Create Account' : 'Welcome to Smart Lighting' }}
          </h2>

          <!-- Message -->
          <p class="text-center text-neutral-600 dark:text-neutral-400 mb-6">
            {{ isSignup ? 'Sign up to get started' : 'Sign in to continue' }}
          </p>

          <!-- Error message -->
          <div
            v-if="errorMessage"
            class="mb-4 p-3 bg-red-100 dark:bg-red-900/30 border border-red-300 dark:border-red-700 rounded-lg"
          >
            <p class="text-sm text-red-700 dark:text-red-300">{{ errorMessage }}</p>
          </div>

          <!-- Email/Password Form -->
          <form @submit.prevent="handleSubmit" class="space-y-4 mb-4">
            <!-- Name field (signup only) -->
            <div v-if="isSignup">
              <label
                for="name"
                class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1"
              >
                Name
              </label>
              <input
                id="name"
                v-model="form.name"
                type="text"
                required
                minlength="2"
                class="w-full px-4 py-2 rounded-lg border border-neutral-300 dark:border-neutral-600 bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-100 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
                placeholder="Your name"
              />
            </div>

            <!-- Email field -->
            <div>
              <label
                for="email"
                class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1"
              >
                Email
              </label>
              <input
                id="email"
                v-model="form.email"
                type="email"
                required
                class="w-full px-4 py-2 rounded-lg border border-neutral-300 dark:border-neutral-600 bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-100 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
                placeholder="you@example.com"
              />
            </div>

            <!-- Password field -->
            <div>
              <label
                for="password"
                class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1"
              >
                Password
              </label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                required
                :minlength="isSignup ? 6 : 1"
                class="w-full px-4 py-2 rounded-lg border border-neutral-300 dark:border-neutral-600 bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-100 focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
                :placeholder="isSignup ? 'At least 6 characters' : 'Your password'"
              />
            </div>

            <!-- Submit button -->
            <button
              type="submit"
              :disabled="isLoading"
              class="btn btn-primary w-full flex items-center justify-center gap-2"
            >
              <svg v-if="isLoading" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                />
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              {{ isLoading ? 'Please wait...' : isSignup ? 'Create Account' : 'Sign In' }}
            </button>
          </form>

          <!-- Toggle signup/login -->
          <p class="text-center text-sm text-neutral-600 dark:text-neutral-400 mb-4">
            {{ isSignup ? 'Already have an account?' : "Don't have an account?" }}
            <button
              type="button"
              class="text-primary-600 dark:text-primary-400 hover:underline font-medium"
              @click="toggleMode"
            >
              {{ isSignup ? 'Sign in' : 'Sign up' }}
            </button>
          </p>

          <!-- Divider -->
          <div class="relative my-6">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-neutral-300 dark:border-neutral-600"></div>
            </div>
            <div class="relative flex justify-center text-sm">
              <span class="px-2 bg-white dark:bg-neutral-900 text-neutral-500">or</span>
            </div>
          </div>

          <!-- Google button -->
          <button
            type="button"
            class="w-full flex items-center justify-center gap-3 px-4 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
            @click="handleGoogleLogin"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
            <span class="text-neutral-700 dark:text-neutral-300 font-medium"
              >Continue with Google</span
            >
          </button>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '../stores/auth'

defineProps({
  show: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['close', 'success'])

const authStore = useAuthStore()

const isSignup = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

const form = reactive({
  name: '',
  email: '',
  password: ''
})

const toggleMode = () => {
  isSignup.value = !isSignup.value
  errorMessage.value = ''
}

const handleSubmit = async () => {
  errorMessage.value = ''
  isLoading.value = true

  try {
    if (isSignup.value) {
      await authStore.signup(form.email, form.password, form.name)
    } else {
      await authStore.loginWithEmail(form.email, form.password)
    }
    emit('success')
    emit('close')
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    isLoading.value = false
  }
}

const handleGoogleLogin = () => {
  authStore.loginWithGoogle()
}
</script>
