<template>
  <div class="flex items-center justify-center" :class="containerClass">
    <div class="relative">
      <!-- Outer ring -->
      <div 
        class="animate-spin rounded-full border-t-2 border-b-2"
        :class="[sizeClass, colorClass]"
      ></div>
      
      <!-- Center dot -->
      <div 
        class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 rounded-full"
        :class="dotClass"
      ></div>
    </div>
    
    <p v-if="message" class="ml-3 text-sm text-neutral-600 dark:text-neutral-400">
      {{ message }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg', 'xl'].includes(value)
  },
  message: {
    type: String,
    default: ''
  },
  color: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'accent', 'neutral'].includes(value)
  },
  containerClass: {
    type: String,
    default: 'p-8'
  }
})

const sizeClass = computed(() => {
  const sizes = {
    sm: 'h-6 w-6',
    md: 'h-10 w-10',
    lg: 'h-16 w-16',
    xl: 'h-24 w-24'
  }
  return sizes[props.size]
})

const dotClass = computed(() => {
  const sizes = {
    sm: 'h-1.5 w-1.5',
    md: 'h-2.5 w-2.5',
    lg: 'h-4 w-4',
    xl: 'h-6 w-6'
  }
  
  const colors = {
    primary: 'bg-primary-500',
    accent: 'bg-accent-500',
    neutral: 'bg-neutral-500'
  }
  
  return `${sizes[props.size]} ${colors[props.color]}`
})

const colorClass = computed(() => {
  const colors = {
    primary: 'border-primary-500',
    accent: 'border-accent-500',
    neutral: 'border-neutral-500'
  }
  return colors[props.color]
})
</script>






