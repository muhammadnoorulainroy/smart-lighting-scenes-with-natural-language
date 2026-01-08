<template>
  <transition
    enter-active-class="transition ease-out duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition ease-in duration-200"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="show && device"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <div class="card p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-3">
            <div
              :class="deviceIconClass"
              class="w-12 h-12 rounded-xl flex items-center justify-center"
            >
              <component :is="deviceIcon" class="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 class="text-xl font-semibold">{{ device.name }}</h2>
              <p class="text-sm text-neutral-500">{{ deviceTypeLabel }}</p>
            </div>
          </div>
          <button
            class="p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-neutral-800"
            @click="$emit('close')"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- Status Badge -->
        <div class="mb-6">
          <span
            :class="
              isOnline
                ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
            "
            class="px-3 py-1 rounded-full text-sm font-medium"
          >
            {{ isOnline ? 'Online' : 'Offline' }}
          </span>
          <span v-if="liveDeviceState?.lastSeen" class="text-xs text-neutral-500 ml-2">
            Last seen: {{ formatTime(liveDeviceState.lastSeen) }}
          </span>
        </div>

        <!-- LED Device State -->
        <div v-if="isLedDevice && isOnline" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-medium text-neutral-700 dark:text-neutral-300">Light State</h3>
            <span
              v-if="deviceStates[device.id]"
              class="flex items-center gap-1.5 text-xs px-2 py-1 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
            >
              <span class="relative flex h-1.5 w-1.5">
                <span
                  class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"
                />
                <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-green-500" />
              </span>
              Live
            </span>
          </div>

          <!-- Power State -->
          <div
            class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <span>Power</span>
            <span
              :class="
                liveDeviceState?.isOn || liveDeviceState?.on ? 'text-green-600' : 'text-neutral-500'
              "
              class="font-medium"
            >
              {{ liveDeviceState?.isOn || liveDeviceState?.on ? 'ON' : 'OFF' }}
            </span>
          </div>

          <!-- Color Preview -->
          <div
            v-if="liveDeviceState?.rgbColor || liveDeviceState?.rgb || liveDeviceState?.color"
            class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center justify-between mb-2">
              <span>Color</span>
              <span class="font-mono text-sm">{{
                liveDeviceState.rgbColor || liveDeviceState.rgb || liveDeviceState.color
              }}</span>
            </div>
            <div
              class="h-8 rounded-lg border border-neutral-300 dark:border-neutral-600"
              :style="{
                backgroundColor: formatRgbColor(
                  liveDeviceState.rgbColor || liveDeviceState.rgb || liveDeviceState.color
                )
              }"
            />
          </div>

          <!-- Brightness -->
          <div
            v-if="currentBrightness !== null"
            class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center justify-between mb-2">
              <span>Brightness</span>
              <span class="font-medium">{{ currentBrightness }}%</span>
            </div>
            <div class="w-full bg-neutral-300 dark:bg-neutral-600 rounded-full h-2">
              <div
                class="bg-yellow-400 h-2 rounded-full transition-all"
                :style="{ width: `${currentBrightness}%` }"
              />
            </div>
          </div>

          <!-- Saturation (Humidity-based) -->
          <div
            v-if="currentSaturation !== null"
            class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center justify-between mb-2">
              <span>Saturation <span class="text-xs text-neutral-500">(humidity)</span></span>
              <span class="font-medium">{{ currentSaturation }}%</span>
            </div>
            <div class="w-full bg-neutral-300 dark:bg-neutral-600 rounded-full h-2">
              <div
                class="bg-purple-500 h-2 rounded-full transition-all"
                :style="{ width: `${currentSaturation}%` }"
              />
            </div>
          </div>

          <!-- Color Temperature (Temperature-based) -->
          <div
            v-if="currentColorTemp !== null"
            class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center justify-between mb-2">
              <span>Color Temp <span class="text-xs text-neutral-500">(temperature)</span></span>
              <span class="font-medium">{{ currentColorTemp }}K</span>
            </div>
            <div
              class="w-full rounded-full h-2 overflow-hidden"
              style="background: linear-gradient(to right, #ffb347, #fffaf0, #87ceeb)"
            >
              <div
                class="h-2 bg-white/30 transition-all"
                :style="{ width: `${colorTempPercent}%`, marginLeft: 'auto' }"
              />
            </div>
            <div class="flex justify-between text-xs text-neutral-500 mt-1">
              <span>Warm (2700K)</span>
              <span>Cool (6500K)</span>
            </div>
          </div>

          <div v-if="canEdit" class="mt-6 pt-6 border-t border-neutral-200 dark:border-neutral-700">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-medium text-neutral-700 dark:text-neutral-300">Manual Control</h3>
              <label class="relative inline-flex items-center cursor-pointer">
                <input v-model="manualMode" type="checkbox" class="sr-only peer" />
                <div
                  class="w-11 h-6 bg-neutral-300 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-neutral-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-neutral-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"
                />
                <span class="ml-2 text-sm text-neutral-600 dark:text-neutral-400">{{
                  manualMode ? 'Manual' : 'Auto'
                }}</span>
              </label>
            </div>

            <div v-if="manualMode" class="space-y-4">
              <!-- Power Toggle -->
              <div
                class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
              >
                <span>Power</span>
                <button
                  :class="controlState.on ? 'bg-green-500' : 'bg-neutral-400'"
                  class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                  @click="controlState.on = !controlState.on"
                >
                  <span
                    :class="controlState.on ? 'translate-x-6' : 'translate-x-1'"
                    class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                  />
                </button>
              </div>

              <!-- RGB Color Picker -->
              <div class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
                <div class="flex items-center justify-between mb-3">
                  <span>Color</span>
                  <div class="flex items-center gap-2">
                    <div
                      class="w-8 h-8 rounded-lg border-2 border-neutral-300 dark:border-neutral-600"
                      :style="{ backgroundColor: controlState.color }"
                    />
                    <span class="font-mono text-sm">{{ controlState.color }}</span>
                  </div>
                </div>
                <input
                  v-model="controlState.color"
                  type="color"
                  class="w-full h-10 rounded-lg cursor-pointer border-0"
                />
              </div>

              <!-- Brightness Slider -->
              <div class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <span>Brightness</span>
                  <span class="font-medium">{{ controlState.brightness }}%</span>
                </div>
                <input
                  v-model.number="controlState.brightness"
                  type="range"
                  min="0"
                  max="100"
                  class="w-full h-2 bg-neutral-300 dark:bg-neutral-600 rounded-lg appearance-none cursor-pointer accent-yellow-400"
                />
                <div class="flex justify-between text-xs text-neutral-500 mt-1">
                  <span>0%</span>
                  <span>50%</span>
                  <span>100%</span>
                </div>
              </div>

              <!-- Color Temperature Slider -->
              <div class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <span>Color Temperature</span>
                  <span class="font-medium">{{ controlState.colorTemp }}K</span>
                </div>
                <input
                  v-model.number="controlState.colorTemp"
                  type="range"
                  min="2700"
                  max="6500"
                  step="100"
                  class="w-full h-2 rounded-lg appearance-none cursor-pointer"
                  style="background: linear-gradient(to right, #ffb347, #fffaf0, #87ceeb)"
                />
                <div class="flex justify-between text-xs text-neutral-500 mt-1">
                  <span>Warm</span>
                  <span>Neutral</span>
                  <span>Cool</span>
                </div>
              </div>

              <!-- Apply Button -->
              <button
                class="w-full btn btn-primary py-3 flex items-center justify-center gap-2"
                :disabled="applyingChanges"
                @click="applyLedChanges"
              >
                <svg
                  v-if="applyingChanges"
                  class="animate-spin h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                >
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
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                <span>{{ applyingChanges ? 'Applying...' : 'Apply Changes' }}</span>
              </button>

              <p class="text-xs text-neutral-500 text-center">
                Manual mode overrides automatic sensor-based adjustments
              </p>
            </div>
          </div>
        </div>

        <!-- Offline LED -->
        <div
          v-else-if="isLedDevice && !isOnline"
          class="p-6 bg-neutral-100 dark:bg-neutral-800 rounded-lg text-center"
        >
          <svg
            class="w-12 h-12 mx-auto text-neutral-400 mb-3"
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
          <p class="text-neutral-500">Light is offline</p>
        </div>

        <!-- Sensor Device -->
        <div v-if="isSensorDevice && isOnline" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-medium text-neutral-700 dark:text-neutral-300">Sensor Readings</h3>
            <span
              v-if="sensorValues.length > 0 && sensorValues[0]?.isLive"
              class="flex items-center gap-1.5 text-xs px-2 py-1 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
            >
              <span class="relative flex h-1.5 w-1.5">
                <span
                  class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"
                />
                <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-green-500" />
              </span>
              Live
            </span>
          </div>

          <div v-if="sensorValues.length > 0" class="grid grid-cols-2 gap-3">
            <div
              v-for="sensor in sensorValues"
              :key="sensor.id"
              class="p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg transition-all duration-300"
              :class="{ 'ring-2 ring-green-400/50': sensor.isLive }"
            >
              <div class="text-xs text-neutral-500 mb-1">{{ sensor.name }}</div>
              <div class="text-lg font-semibold">
                {{ sensor.value }}
                <span class="text-sm font-normal text-neutral-500">{{ sensor.unit }}</span>
              </div>
            </div>
          </div>

          <div
            v-else
            class="p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg text-center text-neutral-500"
          >
            No sensor data available
          </div>
        </div>

        <!-- Offline Sensor -->
        <div
          v-else-if="isSensorDevice && !isOnline"
          class="p-6 bg-neutral-100 dark:bg-neutral-800 rounded-lg text-center"
        >
          <svg
            class="w-12 h-12 mx-auto text-neutral-400 mb-3"
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
          <p class="text-neutral-500">Sensor is offline</p>
          <p class="text-xs text-neutral-400 mt-1">No readings available</p>
        </div>

        <!-- Device Info -->
        <div class="mt-6 pt-6 border-t border-neutral-200 dark:border-neutral-700">
          <h3 class="font-medium text-neutral-700 dark:text-neutral-300 mb-3">
            Device Information
          </h3>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-neutral-500">Type</span>
              <span>{{ deviceTypeLabel }}</span>
            </div>
            <div v-if="device.metaJson?.board" class="flex justify-between">
              <span class="text-neutral-500">Board</span>
              <span>{{ device.metaJson.board }}</span>
            </div>
            <div v-if="device.metaJson?.sensor_id" class="flex justify-between">
              <span class="text-neutral-500">Sensor ID</span>
              <span class="font-mono text-xs">{{ device.metaJson.sensor_id }}</span>
            </div>
            <div v-if="device.mqttStateTopic" class="flex justify-between">
              <span class="text-neutral-500">MQTT Topic</span>
              <span class="font-mono text-xs truncate max-w-[200px]">{{
                device.mqttStateTopic
              }}</span>
            </div>
          </div>
        </div>

        <!-- Close Button -->
        <div class="mt-6">
          <button class="btn btn-secondary w-full" @click="$emit('close')">Close</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, defineComponent, ref, watch, reactive } from 'vue'
import { devicesApi } from '../../api/devices'
import { lightingApi } from '../../api/lighting'
import { useWebSocket } from '../../stores/websocket'
import { useToast } from '../../stores/toast'

const props = defineProps({
  show: { type: Boolean, required: true },
  device: { type: Object, default: null },
  canEdit: { type: Boolean, default: true }
})

defineEmits(['close'])

// WebSocket for real-time updates
const { sensorData, deviceStates } = useWebSocket()
const toast = useToast()

// Manual control state
const manualMode = ref(false)
const applyingChanges = ref(false)
const controlState = reactive({
  on: true,
  color: '#00FF64',
  brightness: 50,
  colorTemp: 4000
})

// Initialize control state when device changes or modal opens
watch(
  () => [props.show, props.device],
  () => {
    if (props.show && props.device) {
      const state = props.device.deviceState || {}
      controlState.on = state.isOn ?? state.on ?? true
      controlState.brightness = state.brightnessPct ?? state.brightness ?? 50
      controlState.colorTemp = state.colorTempKelvin ?? state.color_temp ?? 4000
      // Parse RGB color
      if (state.rgbColor) {
        controlState.color = state.rgbColor
      } else if (state.rgb) {
        controlState.color = formatRgbColor(state.rgb)
      }
      manualMode.value = false
    }
  },
  { immediate: true }
)

// Apply LED changes
const applyLedChanges = async () => {
  if (!props.device?.id) {
    return
  }

  applyingChanges.value = true
  try {
    // Parse hex color to RGB array
    const hex = controlState.color.replace('#', '')
    const r = parseInt(hex.substring(0, 2), 16)
    const g = parseInt(hex.substring(2, 4), 16)
    const b = parseInt(hex.substring(4, 6), 16)

    const command = {
      on: controlState.on,
      rgb: [r, g, b],
      brightness: controlState.brightness,
      color_temp: controlState.colorTemp,
      mode: 'manual' // Override auto mode
    }

    const result = await lightingApi.sendLedCommand(props.device.id, command)

    // Show pending notification while waiting for ACK
    if (result?.correlationId) {
      toast.pendingWithTimeout(
        `Sending command to ${props.device.name}...`,
        30000,
        `Command to "${props.device.name}" timed out. Device may be offline.`
      )
    } else {
      toast.success('Command sent successfully')
    }
  } catch (err) {
    console.error('Failed to apply LED changes:', err)
    toast.error('Failed to apply changes. Please try again.')
  } finally {
    applyingChanges.value = false
  }
}

// Get live LED state from WebSocket (merges with device prop)
const liveDeviceState = computed(() => {
  if (!props.device?.id) {
    return props.device?.deviceState || {}
  }

  const wsState = deviceStates[props.device.id]
  if (wsState) {
    return {
      ...props.device?.deviceState,
      ...wsState,
      lastSeen: wsState.lastSeen || props.device?.deviceState?.lastSeen
    }
  }
  return props.device?.deviceState || {}
})

// Sensor readings from API (initial load)
const sensorReadings = ref([])
const loadingReadings = ref(false)

// Icons
const LightIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>`
})

const SensorIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>`
})

const isLedDevice = computed(() => ['LIGHT', 'LED'].includes(props.device?.type))
const isSensorDevice = computed(() => ['SENSOR', 'MULTI_SENSOR'].includes(props.device?.type))

const isOnline = computed(() => {
  if (!props.device) {
    return false
  }
  // Use live state for LED devices
  const lastSeen = isLedDevice.value
    ? liveDeviceState.value?.lastSeen
    : props.device.deviceState?.lastSeen
  if (!lastSeen) {
    return false
  }
  const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000)
  return new Date(lastSeen) > fiveMinutesAgo
})

const deviceIcon = computed(() => (isLedDevice.value ? LightIcon : SensorIcon))

const deviceIconClass = computed(() => {
  if (isLedDevice.value) {
    return 'bg-gradient-to-br from-yellow-400 to-orange-500'
  }
  return 'bg-gradient-to-br from-blue-400 to-cyan-500'
})

const deviceTypeLabel = computed(() => {
  const labels = {
    LIGHT: 'RGB Light',
    LED: 'LED Light',
    SENSOR: 'Sensor',
    MULTI_SENSOR: 'Multi-Sensor Device',
    MICROCONTROLLER: 'Microcontroller'
  }
  return labels[props.device?.type] || props.device?.type
})

// Get current brightness from live state (handles different key names)
const currentBrightness = computed(() => {
  const state = liveDeviceState.value
  if (!state) {
    return null
  }
  return state.brightnessPct ?? state.brightness ?? null
})

// Get current saturation from live state (humidity-based)
const currentSaturation = computed(() => {
  const state = liveDeviceState.value
  if (!state) {
    return null
  }
  return state.saturationPct ?? state.saturation ?? null
})

// Get current color temperature from live state (temperature-based)
const currentColorTemp = computed(() => {
  const state = liveDeviceState.value
  if (!state) {
    return null
  }
  return state.colorTempKelvin ?? state.color_temp ?? null
})

// Calculate color temp position as percentage (2700K = 0%, 6500K = 100%)
const colorTempPercent = computed(() => {
  const temp = currentColorTemp.value
  if (!temp) {
    return 50
  }
  // Map 2700K-6500K to 0-100%
  const percent = ((temp - 2700) / (6500 - 2700)) * 100
  return Math.max(0, Math.min(100, percent))
})

// Format RGB color for CSS (handles array or string formats)
const formatRgbColor = color => {
  if (!color) {
    return 'transparent'
  }
  if (typeof color === 'string') {
    // Already a CSS color string
    if (color.startsWith('#') || color.startsWith('rgb')) {
      return color
    }
    // Might be comma-separated: "255,128,0"
    if (color.includes(',')) {
      const parts = color.split(',').map(Number)
      return `rgb(${parts[0]}, ${parts[1]}, ${parts[2]})`
    }
    return color
  }
  if (Array.isArray(color) && color.length >= 3) {
    return `rgb(${color[0]}, ${color[1]}, ${color[2]})`
  }
  return 'transparent'
}

// Metric display names
const metricNames = {
  temperature: 'Temperature',
  humidity: 'Humidity',
  pressure: 'Pressure',
  light: 'Light',
  luminosity: 'Luminosity',
  proximity: 'Proximity',
  audio: 'Sound Level',
  accelerometer: 'Acceleration',
  gyroscope: 'Rotation',
  magnetometer: 'Magnetic',
  color: 'Color',
  gesture: 'Gesture'
}

const sensorValues = computed(() => {
  if (!props.device?.metaJson) {
    return []
  }

  const sensorId = props.device.metaJson.sensor_id
  const capabilities =
    props.device.metaJson.capabilities || props.device.metaJson.sensors?.map(s => s.id) || []

  // Mapping for capability names to possible data keys (handles embedded naming differences)
  const capabilityAliases = {
    light: ['light', 'luminosity', 'lux', 'l'],
    luminosity: ['luminosity', 'light', 'lux', 'l'],
    temperature: ['temperature', 'temp', 't'],
    humidity: ['humidity', 'hum', 'h'],
    pressure: ['pressure', 'press', 'p'],
    audio: ['audio', 'audio_peak', 'a']
  }

  // Priority 1: Real-time WebSocket data
  if (sensorId && sensorData[sensorId]) {
    const realtime = sensorData[sensorId]
    const metricUnits = {
      temperature: '°C',
      humidity: '%',
      pressure: 'hPa',
      light: 'lux',
      luminosity: 'lux',
      audio: 'dB',
      audio_peak: 'dB'
    }

    return capabilities.map(cap => {
      // Try multiple aliases for each capability
      const aliases = capabilityAliases[cap] || [cap, cap.toLowerCase()]
      let value
      for (const alias of aliases) {
        if (realtime[alias] !== undefined) {
          value = realtime[alias]
          break
        }
      }
      return {
        id: cap,
        name: metricNames[cap] || cap,
        value: value !== undefined ? Number(value).toFixed(1) : '--',
        unit: metricUnits[cap] || '',
        isLive: true
      }
    })
  }

  // Priority 2: API readings (from database)
  if (sensorReadings.value.length > 0) {
    return sensorReadings.value.map(reading => ({
      id: reading.metric,
      name: metricNames[reading.metric] || reading.metric,
      value: reading.value?.toFixed(1) || '--',
      unit: reading.unit || '',
      timestamp: reading.timestamp,
      isLive: false
    }))
  }

  // Fallback: show capabilities without values (waiting for data)
  return capabilities.map(cap => ({
    id: cap,
    name: metricNames[cap] || cap,
    value: '--',
    unit: '',
    isLive: false
  }))
})

// Load sensor readings when modal opens
watch(
  () => props.show,
  async newVal => {
    if (newVal && props.device?.id && isSensorDevice.value) {
      loadingReadings.value = true
      try {
        const readings = await devicesApi.getSensorReadings(props.device.id)
        sensorReadings.value = readings
      } catch (err) {
        console.error('Failed to load sensor readings:', err)
        sensorReadings.value = []
      } finally {
        loadingReadings.value = false
      }
    } else {
      sensorReadings.value = []
    }
  }
)

const formatTime = timestamp => {
  if (!timestamp) {
    return 'Unknown'
  }
  const date = new Date(timestamp)
  return date.toLocaleString()
}
</script>
