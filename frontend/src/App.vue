<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { ref, onMounted } from 'vue'

const isDarkMode = ref(false)

onMounted(() => {
  // Check system preference
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    isDarkMode.value = true
    document.documentElement.classList.add('dark')
  }
})

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  document.documentElement.classList.toggle('dark')
}
</script>

<template>
  <div id="app" class="min-h-screen bg-neutral-50 dark:bg-neutral-950 transition-colors">
    <!-- Navigation Header -->
    <header class="glass sticky top-0 z-50 border-b border-neutral-200 dark:border-neutral-800">
      <nav class="container mx-auto px-4 py-4 flex items-center justify-between">
        <div class="flex items-center space-x-8">
          <!-- Logo -->
          <RouterLink to="/" class="flex items-center space-x-2 group">
            <div class="w-10 h-10 bg-gradient-to-br from-primary-400 to-accent-500 rounded-lg flex items-center justify-center group-hover:shadow-glow transition-all">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <span class="font-display text-xl font-semibold gradient-text">Smart Lighting</span>
          </RouterLink>

          <!-- Main Navigation -->
          <div class="hidden md:flex items-center space-x-6">
            <RouterLink to="/dashboard" class="nav-link">Dashboard</RouterLink>
            <RouterLink to="/scenes" class="nav-link">Scenes</RouterLink>
            <RouterLink to="/routines" class="nav-link">Routines</RouterLink>
            <RouterLink to="/schedules" class="nav-link">Schedules</RouterLink>
          </div>
        </div>

        <!-- Right side actions -->
        <div class="flex items-center space-x-4">
          <!-- Dark mode toggle -->
          <button 
            @click="toggleDarkMode" 
            class="p-2 rounded-lg hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-colors"
            aria-label="Toggle dark mode"
          >
            <svg v-if="!isDarkMode" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          </button>

          <!-- User menu placeholder -->
          <button class="btn btn-primary">
            Login
          </button>
        </div>
      </nav>
    </header>

    <!-- Main Content -->
    <main class="container mx-auto px-4 py-8">
      <RouterView v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- Footer -->
    <footer class="mt-auto border-t border-neutral-200 dark:border-neutral-800 py-8">
      <div class="container mx-auto px-4 text-center text-sm text-neutral-600 dark:text-neutral-400">
        <p>&copy; 2025 Smart Lighting Scenes. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.nav-link {
  @apply text-neutral-700 dark:text-neutral-300 hover:text-primary-500 dark:hover:text-primary-400 transition-colors font-medium;
}

.nav-link.router-link-active {
  @apply text-primary-500 dark:text-primary-400;
}

/* Page transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
