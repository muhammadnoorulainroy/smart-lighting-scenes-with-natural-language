<template>
  <div class="settings-page container mx-auto px-4 py-8 max-w-4xl">
    <!-- Header -->
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-3xl font-display font-semibold">System Settings</h1>
        <p class="text-neutral-600 dark:text-neutral-400 mt-1">
          Configure ESP32 lighting behavior and sensor parameters
        </p>
      </div>
      <div class="flex gap-3">
        <button
          class="btn btn-secondary"
          :disabled="saving"
          @click="syncToDevices"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Sync to Devices
        </button>
        <button
          class="btn btn-primary"
          :disabled="!hasChanges || saving"
          @click="saveAll"
        >
          <svg v-if="saving" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span v-else>Save All Changes</span>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full" />
    </div>

    <!-- Settings Content -->
    <div v-else class="space-y-6">
      <!-- Lighting Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-2xl">🌿</span>
          <div>
            <h2 class="text-xl font-semibold">Lighting Mode</h2>
            <p class="text-sm text-neutral-500">Control how lights respond to sensors</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-medium mb-2">Global Mode</label>
            <div class="flex gap-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  v-model="config.lighting.globalMode"
                  type="radio"
                  value="auto"
                  class="text-primary-600"
                />
                <span>🌿 Auto (sensor-based)</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  v-model="config.lighting.globalMode"
                  type="radio"
                  value="manual"
                  class="text-primary-600"
                />
                <span>✋ Manual</span>
              </label>
            </div>
          </div>

          <div class="space-y-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.lighting.autoDimEnabled"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span class="text-sm">Enable lux-based auto dimming</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.lighting.sensorOverrideEnabled"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span class="text-sm">Allow sensors to adjust scene values</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Brightness Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-2xl">💡</span>
          <div>
            <h2 class="text-xl font-semibold">Brightness Limits</h2>
            <p class="text-sm text-neutral-500">Set min/max brightness and lux thresholds</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-medium mb-2">
              Minimum Brightness: {{ config.lighting.minBrightness }}%
            </label>
            <input
              v-model.number="config.lighting.minBrightness"
              type="range"
              min="0"
              max="50"
              class="w-full accent-primary-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Maximum Brightness: {{ config.lighting.maxBrightness }}%
            </label>
            <input
              v-model.number="config.lighting.maxBrightness"
              type="range"
              min="50"
              max="100"
              class="w-full accent-primary-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">Lux Min (dark room threshold)</label>
            <input
              v-model.number="config.lighting.luxMin"
              type="number"
              min="0"
              max="500"
              class="input w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">Lux Max (bright room threshold)</label>
            <input
              v-model.number="config.lighting.luxMax"
              type="number"
              min="500"
              max="10000"
              class="input w-full"
            />
          </div>
        </div>
      </div>

      <!-- Climate Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-2xl">🌡️</span>
          <div>
            <h2 class="text-xl font-semibold">Climate Color Adjustment</h2>
            <p class="text-sm text-neutral-500">How temperature and humidity affect light color</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-medium mb-2">
              Temperature Range: {{ config.climate.tempMin }}°C - {{ config.climate.tempMax }}°C
            </label>
            <div class="flex gap-4">
              <input
                v-model.number="config.climate.tempMin"
                type="number"
                min="10"
                max="25"
                class="input w-24"
                placeholder="Min"
              />
              <span class="self-center">to</span>
              <input
                v-model.number="config.climate.tempMax"
                type="number"
                min="25"
                max="40"
                class="input w-24"
                placeholder="Max"
              />
            </div>
            <p class="text-xs text-neutral-500 mt-1">Cold = blue shift, Hot = warm shift</p>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Blend Strength: {{ config.climate.tempBlendStrength }}%
            </label>
            <input
              v-model.number="config.climate.tempBlendStrength"
              type="range"
              min="0"
              max="100"
              class="w-full accent-primary-500"
            />
            <p class="text-xs text-neutral-500 mt-1">How much temperature affects color</p>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Humidity Range: {{ config.climate.humidityMin }}% - {{ config.climate.humidityMax }}%
            </label>
            <div class="flex gap-4">
              <input
                v-model.number="config.climate.humidityMin"
                type="number"
                min="0"
                max="50"
                class="input w-24"
              />
              <span class="self-center">to</span>
              <input
                v-model.number="config.climate.humidityMax"
                type="number"
                min="50"
                max="100"
                class="input w-24"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">Saturation Range</label>
            <div class="flex gap-4 items-center">
              <span class="text-sm">{{ config.climate.saturationAtMinHumidity }}%</span>
              <span class="text-neutral-400">→</span>
              <span class="text-sm">{{ config.climate.saturationAtMaxHumidity }}%</span>
            </div>
            <p class="text-xs text-neutral-500 mt-1">Low humidity = less saturation</p>
          </div>
        </div>
      </div>

      <!-- Audio Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-2xl">🎉</span>
          <div>
            <h2 class="text-xl font-semibold">Audio / Disco Mode</h2>
            <p class="text-sm text-neutral-500">Sound-reactive lighting effects</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.audio.discoEnabled"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span>Enable Disco Mode</span>
            </label>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Audio Threshold: {{ config.audio.audioThreshold }}
            </label>
            <input
              v-model.number="config.audio.audioThreshold"
              type="range"
              min="5"
              max="100"
              class="w-full accent-primary-500"
            />
            <p class="text-xs text-neutral-500 mt-1">Lower = more sensitive</p>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">Disco Duration (ms)</label>
            <input
              v-model.number="config.audio.discoDuration"
              type="number"
              min="1000"
              max="10000"
              step="500"
              class="input w-full"
            />
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Flash Brightness: {{ config.audio.flashBrightness }}%
            </label>
            <input
              v-model.number="config.audio.flashBrightness"
              type="range"
              min="50"
              max="100"
              class="w-full accent-primary-500"
            />
          </div>
        </div>
      </div>

      <!-- Display Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-2xl">📺</span>
          <div>
            <h2 class="text-xl font-semibold">OLED Display</h2>
            <p class="text-sm text-neutral-500">ESP32 display behavior</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.display.oledAutoSleep"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span>Auto-sleep OLED to save power</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.display.showSensorData"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span>Show sensor data on display</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="config.display.showTime"
                type="checkbox"
                class="rounded text-primary-600"
              />
              <span>Show time on display</span>
            </label>
          </div>
          <div>
            <label class="block text-sm font-medium mb-2">
              Sleep Timeout: {{ config.display.oledTimeout }} seconds
            </label>
            <input
              v-model.number="config.display.oledTimeout"
              type="range"
              min="5"
              max="60"
              class="w-full accent-primary-500"
            />
          </div>
        </div>
      </div>

      <!-- Reset Button -->
      <div class="text-center pt-4">
        <button
          class="btn btn-secondary"
          @click="confirmReset"
        >
          Reset All to Defaults
        </button>
      </div>
    </div>

    <!-- Success Toast -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-4"
    >
      <div
        v-if="showToast"
        class="fixed bottom-8 right-8 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-2"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { configApi } from '../api/config'

// State
const loading = ref(true)
const saving = ref(false)
const showToast = ref(false)
const toastMessage = ref('')

// Original config for comparison
const originalConfig = ref(null)

// Reactive config object
const config = reactive({
  lighting: {
    globalMode: 'auto',
    autoDimEnabled: true,
    sensorOverrideEnabled: true,
    minBrightness: 0,
    maxBrightness: 100,
    luxMin: 50,
    luxMax: 2000
  },
  climate: {
    tempMin: 20,
    tempMax: 28,
    tempBlendStrength: 95,
    humidityMin: 30,
    humidityMax: 70,
    saturationAtMinHumidity: 60,
    saturationAtMaxHumidity: 100
  },
  audio: {
    discoEnabled: true,
    audioThreshold: 25,
    discoDuration: 3000,
    discoSpeed: 100,
    flashBrightness: 100
  },
  display: {
    oledAutoSleep: true,
    oledTimeout: 15,
    showSensorData: true,
    showTime: true
  }
})

// Check if there are unsaved changes
const hasChanges = computed(() => {
  if (!originalConfig.value) {
    return false
  }
  return JSON.stringify(config) !== JSON.stringify(originalConfig.value)
})

// Load config from backend
const loadConfig = async () => {
  loading.value = true
  try {
    const data = await configApi.getAll()

    // Merge with defaults to ensure all keys exist
    if (data.lighting) {
      Object.assign(config.lighting, data.lighting)
    }
    if (data.climate) {
      Object.assign(config.climate, data.climate)
    }
    if (data.audio) {
      Object.assign(config.audio, data.audio)
    }
    if (data.display) {
      Object.assign(config.display, data.display)
    }

    // Store original for comparison
    originalConfig.value = JSON.parse(JSON.stringify(config))
  } catch (err) {
    console.error('Failed to load config:', err)
  } finally {
    loading.value = false
  }
}

// Save all changes
const saveAll = async () => {
  saving.value = true
  try {
    await configApi.updateAll({
      lighting: { ...config.lighting },
      climate: { ...config.climate },
      audio: { ...config.audio },
      display: { ...config.display }
    })

    originalConfig.value = JSON.parse(JSON.stringify(config))
    showNotification('Settings saved and synced to devices!')
  } catch (err) {
    console.error('Failed to save config:', err)
    showNotification('Failed to save settings')
  } finally {
    saving.value = false
  }
}

// Sync to devices without saving
const syncToDevices = async () => {
  try {
    await configApi.syncToDevices()
    showNotification('Config synced to all devices')
  } catch (err) {
    console.error('Failed to sync:', err)
  }
}

// Reset to defaults
const confirmReset = async () => {
  if (!confirm('Reset all settings to defaults? This cannot be undone.')) {
    return
  }

  try {
    const data = await configApi.resetAll()

    if (data.lighting) {
      Object.assign(config.lighting, data.lighting)
    }
    if (data.climate) {
      Object.assign(config.climate, data.climate)
    }
    if (data.audio) {
      Object.assign(config.audio, data.audio)
    }
    if (data.display) {
      Object.assign(config.display, data.display)
    }

    originalConfig.value = JSON.parse(JSON.stringify(config))
    showNotification('Settings reset to defaults')
  } catch (err) {
    console.error('Failed to reset:', err)
  }
}

// Show notification toast
const showNotification = (message) => {
  toastMessage.value = message
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.settings-page {
  min-height: calc(100vh - 200px);
}

input[type="range"] {
  height: 6px;
  border-radius: 3px;
}
</style>
