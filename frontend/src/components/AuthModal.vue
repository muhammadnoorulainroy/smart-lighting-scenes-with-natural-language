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
            Authentication Required
          </h2>

          <!-- Message -->
          <p class="text-center text-neutral-600 dark:text-neutral-400 mb-6">
            Please sign in with your Google account to access this feature.
          </p>

          <!-- Sign in button -->
          <div class="flex flex-col gap-3">
            <AuthButton />

            <button class="btn btn-secondary w-full" @click="$emit('close')">Maybe Later</button>
          </div>

          <!-- Privacy notice -->
          <p class="text-xs text-center text-neutral-500 dark:text-neutral-500 mt-6">
            We respect your privacy. Your data is encrypted and secure.
          </p>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup>
import AuthButton from './AuthButton.vue'

defineProps({
  show: {
    type: Boolean,
    required: true
  }
})

defineEmits(['close'])
</script>
