<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <div class="text-center">
      <!-- 404 Illustration -->
      <div class="mb-8">
        <div class="relative inline-block">
          <div class="text-9xl font-bold gradient-text opacity-20">404</div>
          <div class="absolute inset-0 flex items-center justify-center">
            <svg class="w-32 h-32 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Message -->
      <h1 class="text-4xl font-display font-bold mb-4 text-neutral-800 dark:text-neutral-100">
        Page Not Found
      </h1>
      <p class="text-lg text-neutral-600 dark:text-neutral-400 mb-8 max-w-md mx-auto">
        Oops! The page you're looking for doesn't exist or has been moved.
      </p>

      <!-- Actions -->
      <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
        <router-link to="/" class="btn btn-primary">
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          Go Home
        </router-link>
        
        <button @click="$router.back()" class="btn btn-secondary">
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Go Back
        </button>
      </div>

      <!-- Suggestions -->
      <div class="mt-12 p-6 card inline-block">
        <p class="text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-3">
          Looking for something? Try these:
        </p>
        <div class="flex flex-wrap gap-2 justify-center">
          <router-link to="/dashboard" class="text-sm px-4 py-2 rounded-lg bg-neutral-100 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors">
            Dashboard
          </router-link>
          <router-link to="/scenes" class="text-sm px-4 py-2 rounded-lg bg-neutral-100 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors">
            Scenes
          </router-link>
          <router-link to="/routines" class="text-sm px-4 py-2 rounded-lg bg-neutral-100 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors">
            Routines
          </router-link>
          <router-link to="/schedules" class="text-sm px-4 py-2 rounded-lg bg-neutral-100 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors">
            Schedules
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 404 page
</script>






