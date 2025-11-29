<template>
  <div ref="menuContainer" class="relative">
    <!-- User Avatar Button -->
    <button
      class="flex items-center space-x-3 p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
      :aria-expanded="isOpen"
      aria-haspopup="true"
      @click="toggleMenu"
    >
      <img
        v-if="userPicture && !imageError"
        :src="userPicture"
        :alt="userName"
        referrerpolicy="no-referrer"
        class="w-9 h-9 rounded-full border-2 border-primary-500 shadow-sm object-cover"
        @error="handleImageError"
      />
      <div
        v-if="!userPicture || imageError"
        class="w-9 h-9 rounded-full bg-gradient-to-br from-primary-400 to-accent-500 flex items-center justify-center text-white font-semibold shadow-sm"
      >
        {{ userInitials }}
      </div>

      <div class="hidden md:block text-left">
        <p class="text-sm font-medium text-neutral-900 dark:text-neutral-100">
          {{ userName }}
        </p>
        <p class="text-xs text-neutral-500 dark:text-neutral-400 capitalize">
          {{ userRole }}
        </p>
      </div>

      <svg
        class="w-4 h-4 text-neutral-500 transition-transform"
        :class="{ 'rotate-180': isOpen }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <!-- Dropdown Menu -->
    <transition
      enter-active-class="transition ease-out duration-100"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition ease-in duration-75"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div
        v-show="isOpen"
        class="absolute right-0 mt-2 w-64 glass border border-neutral-200 dark:border-neutral-700 rounded-lg shadow-xl py-2 z-50"
      >
        <!-- User Info -->
        <div class="px-4 py-3 border-b border-neutral-200 dark:border-neutral-700">
          <p class="text-sm font-medium text-neutral-900 dark:text-neutral-100">
            {{ userName }}
          </p>
          <p class="text-xs text-neutral-500 dark:text-neutral-400 truncate">
            {{ userEmail }}
          </p>
          <span
            class="inline-block mt-2 px-2 py-1 text-xs font-medium rounded-full"
            :class="roleClass"
          >
            {{ userRole }}
          </span>
        </div>

        <!-- Menu Items -->
        <div class="py-1">
          <router-link to="/dashboard" class="menu-item" @click="closeMenu">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
              />
            </svg>
            <span>Dashboard</span>
          </router-link>

          <router-link to="/scenes" class="menu-item" @click="closeMenu">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
              />
            </svg>
            <span>Scenes</span>
          </router-link>

          <router-link to="/routines" class="menu-item" @click="closeMenu">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <span>Routines</span>
          </router-link>

          <router-link to="/schedules" class="menu-item" @click="closeMenu">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <span>Schedules</span>
          </router-link>
        </div>

        <!-- Logout -->
        <div class="border-t border-neutral-200 dark:border-neutral-700 pt-1">
          <button
            class="menu-item w-full text-left text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
            @click="handleLogout"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
              />
            </svg>
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const isOpen = ref(false)
const menuContainer = ref(null)
const imageError = ref(false)

const userName = computed(() => authStore.userName)
const userEmail = computed(() => authStore.userEmail)
const userPicture = computed(() => authStore.userPicture)
const userRole = computed(() => authStore.userRole.toLowerCase())

const userInitials = computed(() => {
  const name = userName.value
  if (!name || name === 'Guest') {
    return 'G'
  }
  const parts = name.split(' ')
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
})

const roleClass = computed(() => {
  const role = userRole.value
  if (role === 'admin') {
    return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
  } else if (role === 'resident') {
    return 'bg-primary-100 text-primary-800 dark:bg-primary-900 dark:text-primary-200'
  }
  return 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200'
})

const toggleMenu = () => {
  isOpen.value = !isOpen.value
}

const closeMenu = () => {
  isOpen.value = false
}

const handleImageError = () => {
  console.warn('Failed to load user profile image:', userPicture.value)
  imageError.value = true
}

const handleLogout = async () => {
  closeMenu()
  await authStore.logout()
}

// Close menu when clicking outside
const handleClickOutside = event => {
  if (menuContainer.value && !menuContainer.value.contains(event.target)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.menu-item {
  @apply flex items-center space-x-3 px-4 py-2 text-sm text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors;
}
</style>
