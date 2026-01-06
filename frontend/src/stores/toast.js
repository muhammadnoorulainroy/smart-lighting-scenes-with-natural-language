/**
 * @fileoverview Centralized Toast Notification Store
 * 
 * Provides a global toast notification system that can be used
 * from any component in the application.
 */

import { ref, computed } from 'vue'

// Reactive state
const toasts = ref([])
let toastId = 0

// Track pending toast timeouts for cleanup
const pendingTimeouts = new Map()

/**
 * Add a new toast notification
 * @param {string} message - Toast message
 * @param {('success'|'error'|'pending'|'info'|'warning')} type - Toast type
 * @param {number} duration - Duration in ms (0 = persistent)
 * @returns {number} Toast ID for manual dismissal
 */
function show(message, type = 'success', duration = null) {
  const id = ++toastId
  
  // Default durations based on type
  const defaultDurations = {
    success: 3000,
    info: 3000,
    warning: 4000,
    error: 5000,
    pending: 0 // Persistent until manually dismissed
  }
  
  const toast = {
    id,
    message,
    type,
    timestamp: Date.now()
  }
  
  toasts.value.push(toast)
  
  // Auto-dismiss after duration (unless 0)
  const dismissAfter = duration ?? defaultDurations[type] ?? 3000
  if (dismissAfter > 0) {
    setTimeout(() => {
      dismiss(id)
    }, dismissAfter)
  }
  
  return id
}

/**
 * Dismiss a toast by ID
 * @param {number} id - Toast ID
 */
function dismiss(id) {
  // Clear any pending timeout for this toast
  if (pendingTimeouts.has(id)) {
    clearTimeout(pendingTimeouts.get(id))
    pendingTimeouts.delete(id)
  }
  
  const index = toasts.value.findIndex(t => t.id === id)
  if (index !== -1) {
    toasts.value.splice(index, 1)
  }
}

/**
 * Dismiss all toasts
 */
function dismissAll() {
  // Clear all pending timeouts
  pendingTimeouts.forEach(timeoutId => clearTimeout(timeoutId))
  pendingTimeouts.clear()
  toasts.value = []
}

/**
 * Dismiss all pending toasts (useful when ACK arrives)
 */
function dismissPending() {
  const pendingIds = toasts.value
    .filter(t => t.type === 'pending')
    .map(t => t.id)
  
  pendingIds.forEach(id => dismiss(id))
}

/**
 * Update an existing toast (useful for pending -> success/error)
 * @param {number} id - Toast ID
 * @param {string} message - New message
 * @param {string} type - New type
 * @param {number} duration - Auto-dismiss duration
 */
function update(id, message, type, duration = null) {
  const toast = toasts.value.find(t => t.id === id)
  if (toast) {
    toast.message = message
    toast.type = type
    
    // Set auto-dismiss for non-pending toasts
    const defaultDurations = {
      success: 3000,
      info: 3000,
      warning: 4000,
      error: 5000,
      pending: 0
    }
    
    const dismissAfter = duration ?? defaultDurations[type] ?? 3000
    if (dismissAfter > 0) {
      setTimeout(() => {
        dismiss(id)
      }, dismissAfter)
    }
  }
}

// Convenience methods
const success = (message, duration) => show(message, 'success', duration)
const error = (message, duration) => show(message, 'error', duration)
const warning = (message, duration) => show(message, 'warning', duration)
const info = (message, duration) => show(message, 'info', duration)
const pending = (message) => show(message, 'pending', 0)

/**
 * Show a pending toast that auto-converts to error after timeout
 * @param {string} message - Pending message
 * @param {number} timeoutMs - Timeout in ms (default 30s)
 * @param {string} timeoutMessage - Message to show on timeout
 * @returns {number} Toast ID
 */
function pendingWithTimeout(message, timeoutMs = 30000, timeoutMessage = 'Request timed out. Device may be offline.') {
  const id = show(message, 'pending', 0)
  
  // Set timeout to convert to error
  const timeoutId = setTimeout(() => {
    const toast = toasts.value.find(t => t.id === id)
    if (toast && toast.type === 'pending') {
      update(id, timeoutMessage, 'error')
    }
    pendingTimeouts.delete(id)
  }, timeoutMs)
  
  pendingTimeouts.set(id, timeoutId)
  return id
}

/**
 * Composable for using toast notifications
 * @returns {Object} Toast methods and state
 */
export function useToast() {
  return {
    // State
    toasts: computed(() => toasts.value),
    
    // Methods
    show,
    dismiss,
    dismissAll,
    dismissPending,
    update,
    
    // Convenience methods
    success,
    error,
    warning,
    info,
    pending,
    pendingWithTimeout
  }
}

export default useToast