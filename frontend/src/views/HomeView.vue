<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="relative py-24 overflow-hidden">
      <div
        class="absolute inset-0 bg-gradient-to-br from-primary-50 via-accent-50 to-orange-50 dark:from-neutral-900 dark:via-neutral-950 dark:to-neutral-900 opacity-50"
      />
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div
          class="absolute top-1/4 left-1/4 w-96 h-96 bg-primary-400/20 rounded-full blur-3xl animate-pulse-glow"
        />
        <div
          class="absolute bottom-1/4 right-1/4 w-96 h-96 bg-accent-400/20 rounded-full blur-3xl animate-pulse-glow"
          style="animation-delay: 1s"
        />
      </div>
      <div class="relative container mx-auto px-4 text-center">
        <div class="animate-fade-in">
          <h1 class="text-6xl md:text-7xl font-display font-bold mb-6 leading-tight">
            <span class="gradient-text">Illuminate</span><br />
            <span class="text-neutral-800 dark:text-neutral-100">Your Smart Home</span>
          </h1>
          <p
            class="text-xl md:text-2xl text-neutral-600 dark:text-neutral-300 mb-8 max-w-3xl mx-auto leading-relaxed"
          >
            Experience the future of home lighting with AI-powered natural language control. Simply
            speak or type what you want, and watch your space transform.
          </p>
          <!-- CTA Buttons -->
          <div class="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
            <router-link
              v-if="!authStore.isAuthenticated"
              to="/dashboard"
              class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
            >
              Get Started Free
              <ArrowIcon class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" />
            </router-link>
            <router-link
              v-else
              to="/dashboard"
              class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
            >
              Go to Dashboard
              <ArrowIcon class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" />
            </router-link>
            <button
              class="btn btn-secondary text-lg px-8 py-4 inline-flex items-center"
              @click="scrollToDemo"
            >
              Watch Demo
              <PlayIcon class="w-5 h-5 ml-2" />
            </button>
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
          <FeatureCard v-for="feature in features" :key="feature.title" v-bind="feature" />
        </div>
      </div>
    </section>

    <!-- Demo Section -->
    <section
      id="demo"
      ref="demoSection"
      class="py-20 border-t border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/50"
    >
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
                v-model="demoCommand"
                type="text"
                class="input flex-1 text-lg"
                placeholder="e.g., 'Turn on living room lights at 50% warm'"
                @keyup.enter="processDemo"
              />
              <button
                class="btn btn-accent px-8 py-3 text-lg whitespace-nowrap"
                :disabled="!demoCommand.trim()"
                @click="processDemo"
              >
                <span class="flex items-center">
                  Process
                  <BoltIcon class="w-5 h-5 ml-2" />
                </span>
              </button>
            </div>
            <div class="mt-4 flex flex-wrap gap-2">
              <span class="text-xs text-neutral-500 dark:text-neutral-400">Try these:</span>
              <button
                v-for="example in examples"
                :key="example"
                class="text-xs px-3 py-1 rounded-full bg-neutral-200 dark:bg-neutral-800 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-neutral-700 dark:text-neutral-300 transition-colors"
                @click="demoCommand = example"
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
              <div
                class="p-6 bg-gradient-to-br from-primary-50 to-accent-50 dark:from-primary-900/20 dark:to-accent-900/20 rounded-xl border border-primary-200 dark:border-primary-800"
              >
                <p
                  class="text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-3 flex items-center"
                >
                  <LightbulbIcon class="w-5 h-5 mr-2 text-primary-500" />
                  AI Understanding
                </p>
                <pre class="text-sm overflow-x-auto bg-white/50 dark:bg-black/20 p-4 rounded-lg">{{
                  demoResult
                }}</pre>
              </div>
              <div
                class="p-4 bg-accent-50 dark:bg-accent-900/20 rounded-xl border border-accent-200 dark:border-accent-800 flex items-start"
              >
                <CheckCircleIcon class="w-5 h-5 mr-3 text-accent-500 flex-shrink-0 mt-0.5" />
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
          Ready to Transform<br />
          <span class="gradient-text">Your Home?</span>
        </h2>
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
          <AuthButton v-if="!authStore.isAuthenticated" @click="showAuthModal = true" />
          <router-link
            v-else
            to="/dashboard"
            class="btn btn-primary text-lg px-8 py-4 inline-flex items-center group shadow-glow"
          >
            Go to Dashboard
            <ArrowIcon class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" />
          </router-link>
        </div>
      </div>
    </section>

    <AuthModal :show="showAuthModal" @close="showAuthModal = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthButton from '../components/AuthButton.vue'
import AuthModal from '../components/AuthModal.vue'
import FeatureCard from '../components/home/FeatureCard.vue'
import {
  features,
  ArrowIcon,
  PlayIcon,
  BoltIcon,
  LightbulbIcon,
  CheckCircleIcon
} from '../components/home/icons.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const demoCommand = ref('')
const demoResult = ref('')
const demoSection = ref(null)
const showAuthModal = ref(false)

const examples = [
  'Turn on living room lights',
  'Dim bedroom to 30%',
  'Set kitchen to warm white',
  'Turn off all lights at 11 PM'
]

// Show auth modal when redirected from protected route
const checkAuthRequired = () => {
  if (route.query.requiresAuth === 'true') {
    showAuthModal.value = true
    // Clean up the URL query param
    router.replace({ query: {} })
  }
}

onMounted(checkAuthRequired)

// Watch for query changes (when already on home page)
watch(
  () => route.query.requiresAuth,
  newVal => {
    if (newVal === 'true') {
      showAuthModal.value = true
      router.replace({ query: {} })
    }
  }
)

const scrollToDemo = () => demoSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })

const processDemo = () => {
  if (!demoCommand.value.trim()) {
    return
  }
  demoResult.value = JSON.stringify(
    {
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
    },
    null,
    2
  )
}
</script>

<style scoped>
.stat-item {
  transition-property: transform;
  transition-duration: 200ms;
}
.stat-item:hover {
  transform: scale(1.05);
}
</style>
