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
        <button class="btn btn-secondary" :disabled="saving" @click="syncToDevices">
          Sync to Devices
        </button>
        <button class="btn btn-primary" :disabled="!hasChanges || saving" @click="saveAll">
          <svg v-if="saving" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle
              class="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="4"
            />
            <path
              class="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
            />
          </svg>
          <span v-else>Save All Changes</span>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
      <div
        class="animate-spin w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full"
      />
    </div>

    <!-- Settings Content -->
    <div v-else class="space-y-6">
      <!-- Lighting Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <div
            class="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center"
          >
            <svg
              class="w-5 h-5 text-primary-600 dark:text-primary-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
              />
            </svg>
          </div>
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
                <span>Auto (sensor-based)</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  v-model="config.lighting.globalMode"
                  type="radio"
                  value="manual"
                  class="text-primary-600"
                />
                <span>Manual</span>
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
          <div
            class="w-10 h-10 rounded-lg bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center"
          >
            <svg
              class="w-5 h-5 text-yellow-600 dark:text-yellow-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"
              />
            </svg>
          </div>
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
          <div
            class="w-10 h-10 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center"
          >
            <svg
              class="w-5 h-5 text-red-600 dark:text-red-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
              />
            </svg>
          </div>
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
              <span class="text-neutral-400">-></span>
              <span class="text-sm">{{ config.climate.saturationAtMaxHumidity }}%</span>
            </div>
            <p class="text-xs text-neutral-500 mt-1">Low humidity = less saturation</p>
          </div>
        </div>
      </div>

      <!-- Audio Settings -->
      <div class="card p-6">
        <div class="flex items-center gap-3 mb-4">
          <div
            class="w-10 h-10 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center"
          >
            <svg
              class="w-5 h-5 text-purple-600 dark:text-purple-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3"
              />
            </svg>
          </div>
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
          <div
            class="w-10 h-10 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center"
          >
            <svg
              class="w-5 h-5 text-blue-600 dark:text-blue-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              />
            </svg>
          </div>
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
        <button class="btn btn-secondary" @click="confirmReset">Reset All to Defaults</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { configApi } from '../api/config'
import { useToast } from '../stores/toast'

const toast = useToast()

// State
const loading = ref(true)
const saving = ref(false)

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
const showNotification = message => {
  toast.success(message)
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.settings-page {
  min-height: calc(100vh - 200px);
}

input[type='range'] {
  height: 6px;
  border-radius: 3px;
}
</style>
