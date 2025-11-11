<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="relative py-24 overflow-hidden">
      <!-- Background gradient -->
      <div class="absolute inset-0 bg-gradient-to-br from-primary-50 via-accent-50 to-orange-50 dark:from-neutral-900 dark:via-neutral-950 dark:to-neutral-900 opacity-50"></div>
      
      <!-- Animated background elements -->
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute top-1/4 left-1/4 w-96 h-96 bg-primary-400/20 rounded-full blur-3xl animate-pulse-glow"></div>
        <div class="absolute bottom-1/4 right-1/4 w-96 h-96 bg-accent-400/20 rounded-full blur-3xl animate-pulse-glow" style="animation-delay: 1s;"></div>
      </div>

      <div class="relative container mx-auto px-4 text-center">
        <div class="animate-fade-in">
          <h1 class="text-6xl md:text-7xl font-display font-bold mb-6 leading-tight">
            <span class="gradient-text">Illuminate</span><br>
            <span class="text-neutral-800 dark:text-neutral-100">Your Smart Home</span>
          </h1>
          
          <p class="text-xl md:text-2xl text-neutral-600 dark:text-neutral-300 mb-8 max-w-3xl mx-auto leading-relaxed">
            Experience the future of home lighting with AI-powered natural language control. 
            Simply speak or type what you want, and watch your space transform.
          </p>
          
          <!-- CTA Buttons -->
          <div class="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
            <router-link 
              v-if="!authStore.isAuthenticated"
              to="/dashboard" 
              class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
            >
              Get Started Free
              <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            </router-link>
            
            <router-link 
              v-else
              to="/dashboard" 
              class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
            >
              Go to Dashboard
              <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            </router-link>
            
            <button 
              @click="scrollToDemo"
              class="btn btn-secondary text-lg px-8 py-4 inline-flex items-center"
            >
              Watch Demo
              <svg class="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </button>
          </div>

          <!-- Stats -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-8 max-w-4xl mx-auto">
            <div class="stat-item">
              <div class="text-4xl font-bold gradient-text">99.9%</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400 mt-1">Uptime</div>
            </div>
            <div class="stat-item">
              <div class="text-4xl font-bold gradient-text">< 50ms</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400 mt-1">Response Time</div>
            </div>
            <div class="stat-item">
              <div class="text-4xl font-bold gradient-text">24/7</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400 mt-1">Support</div>
            </div>
            <div class="stat-item">
              <div class="text-4xl font-bold gradient-text">100+</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400 mt-1">Integrations</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-20 border-t border-neutral-200 dark:border-neutral-800">
      <div class="container mx-auto px-4">
        <div class="text-center mb-16">
          <h2 class="text-4xl md:text-5xl font-display font-bold mb-4">
            <span class="gradient-text">Powerful Features</span>
          </h2>
          <p class="text-lg text-neutral-600 dark:text-neutral-400 max-w-2xl mx-auto">
            Everything you need to create the perfect ambiance in your smart home
          </p>
        </div>

        <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- Feature 1 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-primary-400 to-primary-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-primary-500 transition-colors">Natural Language Control</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Control your lights naturally. Say "dim the lights for movie time" or "brighten the kitchen at sunset" and it just works.
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-accent-400 to-accent-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-accent-500 transition-colors">Smart Automation</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Create intelligent schedules that adapt to sunrise, sunset, and your daily routines automatically.
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-orange-400 to-orange-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-orange-500 transition-colors">Mobile Control</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Control your lights from anywhere with our beautiful and intuitive mobile app for iOS and Android.
            </p>
          </div>

          <!-- Feature 4 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-red-400 to-red-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-red-500 transition-colors">Custom Scenes</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Create and save custom lighting scenes for any mood or activity - work, relax, party, or sleep.
            </p>
          </div>

          <!-- Feature 5 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-emerald-400 to-emerald-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-emerald-500 transition-colors">Secure & Private</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Your data is encrypted and secure. We respect your privacy and never sell your information.
            </p>
          </div>

          <!-- Feature 6 -->
          <div class="feature-card group">
            <div class="feature-icon bg-gradient-to-br from-cyan-400 to-cyan-600">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold mt-6 mb-3 group-hover:text-cyan-500 transition-colors">Advanced Controls</h3>
            <p class="text-neutral-600 dark:text-neutral-400">
              Fine-tune brightness, color temperature, and RGB colors with precision controls for perfect lighting.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Demo Section -->
    <section id="demo" ref="demoSection" class="py-20 border-t border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/50">
      <div class="container mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-4xl md:text-5xl font-display font-bold mb-4">
            <span class="gradient-text">Try It Out</span>
          </h2>
          <p class="text-lg text-neutral-600 dark:text-neutral-400 max-w-2xl mx-auto">
            See how natural language commands are transformed into smart lighting actions
          </p>
        </div>
        
        <div class="card p-8 md:p-12 max-w-4xl mx-auto">
          <div class="mb-8">
            <label class="block text-sm font-medium mb-3 text-neutral-700 dark:text-neutral-300">
              Enter a natural language command:
            </label>
            <div class="flex flex-col sm:flex-row gap-3">
              <input 
                type="text" 
                class="input flex-1 text-lg" 
                placeholder="e.g., 'Turn on living room lights at 50% warm'"
                v-model="demoCommand"
                @keyup.enter="processDemo"
              >
              <button 
                @click="processDemo" 
                class="btn btn-accent px-8 py-3 text-lg whitespace-nowrap"
                :disabled="!demoCommand.trim()"
              >
                <span class="flex items-center">
                  Process
                  <svg class="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </span>
              </button>
            </div>
            
            <!-- Quick examples -->
            <div class="mt-4 flex flex-wrap gap-2">
              <span class="text-xs text-neutral-500 dark:text-neutral-400">Try these:</span>
              <button 
                v-for="example in examples" 
                :key="example"
                @click="demoCommand = example"
                class="text-xs px-3 py-1 rounded-full bg-neutral-200 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors"
              >
                {{ example }}
              </button>
            </div>
          </div>
          
          <!-- Results -->
          <transition
            enter-active-class="transition ease-out duration-300"
            enter-from-class="opacity-0 transform scale-95"
            enter-to-class="opacity-100 transform scale-100"
          >
            <div v-if="demoResult" class="space-y-4">
              <div class="p-6 bg-gradient-to-br from-primary-50 to-accent-50 dark:from-primary-900/20 dark:to-accent-900/20 rounded-xl border border-primary-200 dark:border-primary-800">
                <p class="text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-3 flex items-center">
                  <svg class="w-5 h-5 mr-2 text-primary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                  </svg>
                  AI Understanding
                </p>
                <pre class="text-sm overflow-x-auto bg-white/50 dark:bg-black/20 p-4 rounded-lg">{{ demoResult }}</pre>
              </div>
              
              <div class="p-4 bg-accent-50 dark:bg-accent-900/20 rounded-xl border border-accent-200 dark:border-accent-800 flex items-start">
                <svg class="w-5 h-5 mr-3 text-accent-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p class="text-sm text-neutral-700 dark:text-neutral-300">
                  Command understood! This would adjust your smart lights accordingly.
                </p>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-20 border-t border-neutral-200 dark:border-neutral-800">
      <div class="container mx-auto px-4 text-center">
        <h2 class="text-4xl md:text-5xl font-display font-bold mb-6">
          Ready to Transform<br>
          <span class="gradient-text">Your Home?</span>
        </h2>
        <p class="text-lg text-neutral-600 dark:text-neutral-400 mb-10 max-w-2xl mx-auto">
          Join thousands of users who have already upgraded to smart lighting with natural language control.
        </p>
        
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
          <AuthButton v-if="!authStore.isAuthenticated" />
          <router-link 
            v-else
            to="/dashboard" 
            class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
          >
            Go to Dashboard
            <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
          </router-link>
        </div>
      </div>
    </section>

    <!-- Auth Modal -->
    <AuthModal :show="showAuthModal" @close="showAuthModal = false" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthButton from '../components/AuthButton.vue'
import AuthModal from '../components/AuthModal.vue'

const route = useRoute()
const authStore = useAuthStore()
const demoCommand = ref('')
const demoResult = ref('')
const demoSection = ref(null)
const showAuthModal = ref(false)

onMounted(() => {
  // Show auth modal if user was redirected from a protected page
  if (route.query.requiresAuth === 'true') {
    showAuthModal.value = true
  }
})

const examples = [
  'Turn on living room lights',
  'Dim bedroom to 30%',
  'Set kitchen to warm white',
  'Turn off all lights at 11 PM'
]

const scrollToDemo = () => {
  demoSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const processDemo = () => {
  if (!demoCommand.value.trim()) return
  
  // Demo processing (will be connected to backend later)
  const mockParsing = {
    command: demoCommand.value,
    parsed: {
      intent: 'control_lights',
      action: 'turn_on',
      location: 'living_room',
      brightness: 50,
      color_temperature: 'warm',
      timing: 'immediate'
    },
    confidence: 0.95,
    timestamp: new Date().toISOString()
  }
  
  demoResult.value = JSON.stringify(mockParsing, null, 2)
}
</script>

<style scoped>
.feature-card {
  @apply card p-8 hover:shadow-xl transition-all duration-300 hover:-translate-y-2 border-2 border-transparent hover:border-primary-200 dark:hover:border-primary-800;
}

.feature-icon {
  @apply w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg transform group-hover:scale-110 transition-transform duration-300;
}

.stat-item {
  @apply transform hover:scale-105 transition-transform duration-200;
}

/* Smooth scroll behavior */
html {
  scroll-behavior: smooth;
}
</style>
