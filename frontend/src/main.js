/**
 * @fileoverview Application entry point.
 *
 * Initializes the Vue 3 application with:
 * - Pinia for state management
 * - Vue Router for navigation
 * - Tailwind CSS for styling
 *
 * This file:
 * 1. Creates the Vue application instance
 * 2. Registers global plugins (Pinia, Router)
 * 3. Imports global styles
 * 4. Mounts the app to the DOM
 *
 * @module main
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import './assets/main.css'

/** The root Vue application instance */
const app = createApp(App)

// Register Pinia for reactive state management
app.use(createPinia())

// Register Vue Router for client-side navigation
app.use(router)

// Mount the application to the #app element
app.mount('#app')
