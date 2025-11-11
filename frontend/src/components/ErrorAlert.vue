<template>
  <transition
    enter-active-class="transition ease-out duration-300"
    enter-from-class="opacity-0 transform translate-y-2"
    enter-to-class="opacity-100 transform translate-y-0"
    leave-active-class="transition ease-in duration-200"
    leave-from-class="opacity-100 transform translate-y-0"
    leave-to-class="opacity-0 transform translate-y-2"
  >
    <div 
      v-if="show"
      class="rounded-lg p-4 mb-4"
      :class="alertClass"
      role="alert"
    >
      <div class="flex items-start">
        <div class="flex-shrink-0">
          <svg 
            v-if="type === 'error'"
            class="h-5 w-5 text-red-600 dark:text-red-400" 
            fill="currentColor" 
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          
          <svg 
            v-else-if="type === 'warning'"
            class="h-5 w-5 text-orange-600 dark:text-orange-400" 
            fill="currentColor" 
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          
          <svg 
            v-else-if="type === 'success'"
            class="h-5 w-5 text-accent-600 dark:text-accent-400" 
            fill="currentColor" 
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          
          <svg 
            v-else
            class="h-5 w-5 text-blue-600 dark:text-blue-400" 
            fill="currentColor" 
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
          </svg>
        </div>
        
        <div class="ml-3 flex-1">
          <h3 v-if="title" class="text-sm font-medium" :class="titleClass">
            {{ title }}
          </h3>
          <div class="text-sm" :class="[title ? 'mt-1' : '', messageClass]">
            {{ message }}
          </div>
        </div>
        
        <button 
          v-if="dismissible"
          @click="$emit('dismiss')"
          class="ml-3 flex-shrink-0 inline-flex rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2"
          :class="buttonClass"
        >
          <span class="sr-only">Dismiss</span>
          <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: true
  },
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['error', 'warning', 'success', 'info'].includes(value)
  },
  title: {
    type: String,
    default: ''
  },
  message: {
    type: String,
    required: true
  },
  dismissible: {
    type: Boolean,
    default: true
  }
})

defineEmits(['dismiss'])

const alertClass = computed(() => {
  const classes = {
    error: 'bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800',
    warning: 'bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800',
    success: 'bg-accent-50 dark:bg-accent-900/20 border border-accent-200 dark:border-accent-800',
    info: 'bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800'
  }
  return classes[props.type]
})

const titleClass = computed(() => {
  const classes = {
    error: 'text-red-800 dark:text-red-200',
    warning: 'text-orange-800 dark:text-orange-200',
    success: 'text-accent-800 dark:text-accent-200',
    info: 'text-blue-800 dark:text-blue-200'
  }
  return classes[props.type]
})

const messageClass = computed(() => {
  const classes = {
    error: 'text-red-700 dark:text-red-300',
    warning: 'text-orange-700 dark:text-orange-300',
    success: 'text-accent-700 dark:text-accent-300',
    info: 'text-blue-700 dark:text-blue-300'
  }
  return classes[props.type]
})

const buttonClass = computed(() => {
  const classes = {
    error: 'text-red-500 hover:text-red-600 focus:ring-red-500',
    warning: 'text-orange-500 hover:text-orange-600 focus:ring-orange-500',
    success: 'text-accent-500 hover:text-accent-600 focus:ring-accent-500',
    info: 'text-blue-500 hover:text-blue-600 focus:ring-blue-500'
  }
  return classes[props.type]
})
</script>






