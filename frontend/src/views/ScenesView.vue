<template>
  <div class="scenes-page container mx-auto px-4 py-8">
    <!-- Header -->
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-3xl font-display font-semibold">Lighting Scenes</h1>
        <p class="text-neutral-600 dark:text-neutral-400 mt-1">
          Apply preset moods or create your own custom scenes
        </p>
      </div>
      <button class="btn btn-primary" @click="showCreateModal = true">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Create Scene
      </button>
    </div>

    <!-- NLP Command Input -->
    <div class="card p-6 mb-8">
      <div class="flex items-center gap-4">
        <div class="flex-1 relative">
          <input
            v-model="nlpCommand"
            type="text"
            class="input w-full pr-12"
            placeholder="Try: 'Turn on movie mode' or 'Set bedroom to 50% brightness'"
            @keyup.enter="processNlpCommand"
          />
          <button
            class="absolute right-2 top-1/2 -translate-y-1/2 p-2 text-neutral-500 hover:text-primary-500"
            :class="{ 'text-red-500 animate-pulse': isListening }"
            @click="toggleVoiceInput"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
          </button>
        </div>
        <button
          class="btn btn-accent"
          :disabled="!nlpCommand.trim() || nlpProcessing"
          @click="processNlpCommand"
        >
          <svg v-if="nlpProcessing" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span v-else>Process</span>
        </button>
      </div>

      <!-- NLP Result -->
      <transition
        enter-active-class="transition ease-out duration-200"
        enter-from-class="opacity-0 -translate-y-2"
        enter-to-class="opacity-100 translate-y-0"
      >
        <div v-if="nlpResult" class="mt-4 p-4 rounded-lg" :class="nlpResult.valid ? 'bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800' : 'bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800'">
          <div v-if="nlpResult.valid" class="space-y-3">
            <div class="flex items-start gap-3">
              <svg class="w-5 h-5 text-green-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <p class="font-medium text-neutral-800 dark:text-neutral-200">{{ nlpResult.preview }}</p>
                <p v-if="nlpResult.isScheduled" class="text-sm text-neutral-600 dark:text-neutral-400 mt-1">
                  This will create a schedule
                </p>
              </div>
            </div>
            <div class="flex gap-3">
              <button class="btn btn-primary" @click="confirmNlpCommand">
                Confirm & Execute
              </button>
              <button class="btn btn-secondary" @click="nlpResult = null">
                Cancel
              </button>
            </div>
          </div>
          <div v-else class="flex items-start gap-3">
            <svg class="w-5 h-5 text-red-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p class="font-medium text-red-800 dark:text-red-200">{{ nlpResult.error }}</p>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Preset Scenes -->
    <div class="mb-8">
      <h2 class="text-xl font-semibold mb-4">Preset Scenes</h2>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div
          v-for="scene in presetScenes"
          :key="scene.id"
          class="card p-4 text-center cursor-pointer hover:shadow-lg hover:-translate-y-1 transition-all duration-200"
          :class="{ 'ring-2 ring-primary-500': applyingScene === scene.id }"
          @click="applyScene(scene)"
        >
          <div class="text-4xl mb-2">{{ scene.icon }}</div>
          <h3 class="font-medium text-sm">{{ scene.name }}</h3>
          <p class="text-xs text-neutral-500 mt-1 line-clamp-2">{{ scene.description }}</p>
          <div class="mt-2 text-xs font-medium px-2 py-1 rounded-full inline-block" :class="getTargetBadgeClass(scene)">
            {{ formatTarget(scene) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Custom Scenes -->
    <div>
      <h2 class="text-xl font-semibold mb-4">Custom Scenes</h2>
      <div v-if="customScenes.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="scene in customScenes"
          :key="scene.id"
          class="card p-4"
        >
          <div class="flex items-start justify-between">
            <div class="flex items-center gap-3">
              <span class="text-2xl">{{ scene.icon }}</span>
              <div>
                <h3 class="font-medium">{{ scene.name }}</h3>
                <p class="text-sm text-neutral-500">{{ scene.description }}</p>
                <div class="mt-1">
                  <span class="text-xs font-medium px-2 py-0.5 rounded-full" :class="getTargetBadgeClass(scene)">
                    {{ formatTarget(scene) }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex gap-2">
              <button class="p-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded" @click="editScene(scene)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button class="p-2 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-red-500" @click="deleteScene(scene)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
          <button
            class="btn btn-primary w-full mt-4"
            :disabled="applyingScene === scene.id"
            @click="applyScene(scene)"
          >
            {{ applyingScene === scene.id ? 'Applying...' : 'Apply Scene' }}
          </button>
        </div>
      </div>
      <div v-else class="card p-8 text-center text-neutral-500">
        <svg class="w-12 h-12 mx-auto mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
        </svg>
        <p>No custom scenes yet</p>
        <button class="btn btn-secondary mt-4" @click="showCreateModal = true">
          Create Your First Scene
        </button>
      </div>
    </div>

    <!-- Create/Edit Scene Modal -->
    <SceneModal
      :show="showCreateModal || showEditModal"
      :scene="editingScene"
      @close="closeModal"
      @save="saveScene"
    />

    <!-- Toast Notification -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-4"
    >
      <div
        v-if="showToastNotification"
        class="fixed bottom-8 right-8 px-6 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50"
        :class="{
          'bg-green-500 text-white': toastType === 'success',
          'bg-yellow-500 text-white': toastType === 'pending',
          'bg-red-500 text-white': toastType === 'error'
        }"
      >
        <!-- Success icon -->
        <svg v-if="toastType === 'success'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <!-- Pending spinner -->
        <svg v-else-if="toastType === 'pending'" class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        <!-- Error icon -->
        <svg v-else-if="toastType === 'error'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { scenesApi } from '../api/scenes'
import { nlpApi } from '../api/nlp'
import { useWebSocket } from '../stores/websocket'
import SceneModal from '../components/scenes/SceneModal.vue'

const { connected: wsConnected, lastSceneApplied, clearLastSceneApplied, connect: connectWs } = useWebSocket()

// State
const scenes = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingScene = ref(null)
const applyingScene = ref(null)
const showToastNotification = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimeout = null

// NLP State
const nlpCommand = ref('')
const nlpProcessing = ref(false)
const nlpResult = ref(null)
const isListening = ref(false)
let recognition = null

// Watch for WebSocket scene events
watch(lastSceneApplied, (newEvent) => {
  if (newEvent) {
    if (newEvent.confirmed) {
      showToast(`Scene "${newEvent.sceneName}" confirmed by ${newEvent.devicesConfirmed} device(s) (${newEvent.latencyMs}ms)`, 'success')
    } else if (newEvent.timedOut) {
      showToast(`Scene "${newEvent.sceneName}" timed out (${newEvent.acksReceived}/${newEvent.lightsExpected} devices responded)`, 'error')
    } else {
      // Legacy event without ack
      showToast(`Scene "${newEvent.sceneName}" applied to ${newEvent.devicesAffected} light(s)`, 'success')
    }
    clearLastSceneApplied()
  }
})

const showToast = (message, type = 'success') => {
  toastMessage.value = message
  toastType.value = type
  showToastNotification.value = true
  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }
  toastTimeout = setTimeout(() => {
    showToastNotification.value = false
  }, type === 'error' ? 5000 : 3000)
}

// Computed
const presetScenes = computed(() => scenes.value.filter(s => s.isPreset))
const customScenes = computed(() => scenes.value.filter(s => !s.isPreset))

// Methods
const loadScenes = async () => {
  loading.value = true
  try {
    scenes.value = await scenesApi.getAll()
  } catch (err) {
    console.error('Failed to load scenes:', err)
  } finally {
    loading.value = false
  }
}

const applyScene = async scene => {
  applyingScene.value = scene.id
  try {
    const result = await scenesApi.apply(scene.id)
    
    if (result.correlationId && result.status === 'pending') {
      showToast(`Sending "${scene.name}" to ${result.lightsAffected} device(s)...`, 'pending')
    } else if (!wsConnected.value) {
      showToast(result.message || `Scene "${scene.name}" applied`, 'success')
    }
  } catch (err) {
    console.error('Failed to apply scene:', err)
    showToast('Failed to apply scene', 'error')
  } finally {
    applyingScene.value = null
  }
}

const editScene = scene => {
  editingScene.value = { ...scene }
  showEditModal.value = true
}

const deleteScene = async scene => {
  if (!confirm(`Delete scene "${scene.name}"?`)) {return}
  try {
    await scenesApi.delete(scene.id)
    await loadScenes()
  } catch (err) {
    console.error('Failed to delete scene:', err)
    alert('Failed to delete scene')
  }
}

const saveScene = async sceneData => {
  try {
    if (editingScene.value?.id) {
      await scenesApi.update(editingScene.value.id, sceneData)
    } else {
      await scenesApi.create(sceneData)
    }
    await loadScenes()
    closeModal()
  } catch (err) {
    console.error('Failed to save scene:', err)
    alert('Failed to save scene')
  }
}

const closeModal = () => {
  showCreateModal.value = false
  showEditModal.value = false
  editingScene.value = null
}

// Helper to format target room display
const formatTarget = scene => {
  const target = scene.settingsJson?.target || scene.settings?.target || 'all'
  if (!target || target === 'all') {
    return '🏠 All Rooms'
  }
  // Capitalize and format room name
  const roomName = target.toString()
    .replace(/_/g, ' ')
    .replace(/-/g, ' ')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')

  const roomIcons = {
    bedroom: '🛏️',
    living_room: '🛋️',
    'living room': '🛋️',
    kitchen: '🍳',
    bathroom: '🚿',
    hallway: '🚪'
  }
  const icon = roomIcons[target.toLowerCase()] || '💡'
  return `${icon} ${roomName}`
}

// Helper to get badge styling based on target
const getTargetBadgeClass = scene => {
  const target = scene.settingsJson?.target || scene.settings?.target || 'all'
  if (!target || target === 'all') {
    return 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
  }
  return 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
}

// NLP Methods
const processNlpCommand = async () => {
  if (!nlpCommand.value.trim()) {return}

  nlpProcessing.value = true
  nlpResult.value = null

  try {
    nlpResult.value = await nlpApi.parse(nlpCommand.value)
  } catch (err) {
    console.error('NLP error:', err)
    nlpResult.value = { valid: false, error: 'Failed to process command' }
  } finally {
    nlpProcessing.value = false
  }
}

const confirmNlpCommand = async () => {
  if (!nlpResult.value?.valid) {return}

  nlpProcessing.value = true
  try {
    const result = await nlpApi.confirm(nlpResult.value)
    if (result.executed) {
      nlpCommand.value = ''
      nlpResult.value = null
      // Refresh scenes in case a new one was created
      await loadScenes()
    }
  } catch (err) {
    console.error('Execute error:', err)
  } finally {
    nlpProcessing.value = false
  }
}

// Voice Input
const toggleVoiceInput = () => {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    alert('Voice input is not supported in your browser')
    return
  }

  if (isListening.value) {
    recognition?.stop()
    isListening.value = false
    return
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SpeechRecognition()
  recognition.continuous = false
  recognition.interimResults = false
  recognition.lang = 'en-US'

  recognition.onstart = () => {
    isListening.value = true
  }

  recognition.onresult = event => {
    const {transcript} = event.results[0][0]
    nlpCommand.value = transcript
    isListening.value = false
    // Auto-process after voice input
    processNlpCommand()
  }

  recognition.onerror = () => {
    isListening.value = false
  }

  recognition.onend = () => {
    isListening.value = false
  }

  recognition.start()
}

onMounted(() => {
  loadScenes()
  if (!wsConnected.value) {
    connectWs()
  }
})

onUnmounted(() => {
  if (toastTimeout) {
    clearTimeout(toastTimeout)
  }
})
</script>

<style scoped>
.scenes-page {
  min-height: calc(100vh - 200px);
}
</style>
