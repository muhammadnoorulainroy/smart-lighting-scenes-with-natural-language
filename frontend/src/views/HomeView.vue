<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="py-20 text-center">
      <h1 class="text-5xl md:text-6xl font-display font-bold mb-6 animate-fade-in">
        <span class="gradient-text">Smart Lighting</span><br>
        <span class="text-neutral-800 dark:text-neutral-100">Redefined</span>
      </h1>
      <p class="text-xl text-neutral-600 dark:text-neutral-400 mb-12 max-w-2xl mx-auto animate-slide-up">
        Control your home lighting with natural language. Just speak or type what you want, and watch the magic happen.
      </p>
      
      <!-- Feature Cards -->
      <div class="grid md:grid-cols-3 gap-8 mt-16">
        <div class="feature-card">
          <div class="feature-icon bg-gradient-to-br from-primary-400 to-primary-600">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
          </div>
          <h3 class="text-xl font-semibold mt-4 mb-2">Natural Language Control</h3>
          <p class="text-neutral-600 dark:text-neutral-400">
            Simply say "dim the lights for movie time" or "brighten the kitchen at sunset"
          </p>
        </div>

        <div class="feature-card">
          <div class="feature-icon bg-gradient-to-br from-accent-400 to-accent-600">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h3 class="text-xl font-semibold mt-4 mb-2">Smart Automation</h3>
          <p class="text-neutral-600 dark:text-neutral-400">
            Create intelligent schedules that adapt to sunrise, sunset, and your daily routines
          </p>
        </div>

        <div class="feature-card">
          <div class="feature-icon bg-gradient-to-br from-orange-400 to-orange-600">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
          <h3 class="text-xl font-semibold mt-4 mb-2">Mobile Control</h3>
          <p class="text-neutral-600 dark:text-neutral-400">
            Control your lights from anywhere with our intuitive mobile app
          </p>
        </div>
      </div>

      <!-- CTA Section -->
      <div class="mt-20">
        <router-link to="/dashboard" class="btn btn-primary text-lg px-8 py-3 inline-flex items-center group">
          Get Started
          <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
          </svg>
        </router-link>
      </div>
    </section>

    <!-- Demo Section -->
    <section class="py-16 border-t border-neutral-200 dark:border-neutral-800">
      <h2 class="text-3xl font-display font-semibold text-center mb-12">See It In Action</h2>
      
      <div class="card p-8 max-w-3xl mx-auto">
        <div class="mb-6">
          <label class="block text-sm font-medium mb-2">Try a natural language command:</label>
          <div class="flex space-x-2">
            <input 
              type="text" 
              class="input flex-1" 
              placeholder="e.g., 'Turn on living room lights at 50% warm'"
              v-model="demoCommand"
            >
            <button @click="processDemo" class="btn btn-accent">
              Process
            </button>
          </div>
        </div>
        
        <div v-if="demoResult" class="mt-6 p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
          <p class="text-sm text-neutral-600 dark:text-neutral-400 mb-2">AI Understanding:</p>
          <pre class="text-xs overflow-x-auto">{{ demoResult }}</pre>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const demoCommand = ref('')
const demoResult = ref('')

const processDemo = () => {
  // Demo processing (will be connected to backend later)
  demoResult.value = JSON.stringify({
    intent: 'set_brightness',
    location: 'living_room',
    brightness: 50,
    color_temp: 'warm',
    timing: 'immediate'
  }, null, 2)
}
</script>

<style scoped>
.feature-card {
  @apply card p-6 hover:shadow-xl transition-all duration-300 hover:-translate-y-1;
}

.feature-icon {
  @apply w-16 h-16 rounded-full mx-auto flex items-center justify-center shadow-lg;
}
</style>
