<template>
  <div
    class="device-card p-4 bg-neutral-50 dark:bg-neutral-800/50 rounded-xl border border-neutral-200 dark:border-neutral-700 hover:border-primary-300 dark:hover:border-primary-700 cursor-pointer transition-all hover:shadow-md"
    @click="$emit('click', device)"
  >
    <div class="flex items-start justify-between">
      <div class="flex items-center gap-3">
        <!-- Device Icon -->
        <div
          :class="iconBgClass"
          class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
        >
          <component :is="deviceIcon" class="w-5 h-5 text-white" />
        </div>
        <div class="min-w-0">
          <h4 class="font-medium truncate">{{ device.name }}</h4>
          <p class="text-xs text-neutral-500">{{ deviceTypeLabel }}</p>
        </div>
      </div>

      <!-- Status Indicator -->
      <div class="flex-shrink-0">
        <span
          v-if="isOnline"
          class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-green-500 mr-1.5 animate-pulse" />
          Online
        </span>
        <span
          v-else
          class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-neutral-200 text-neutral-600 dark:bg-neutral-700 dark:text-neutral-400"
        >
          Offline
        </span>
      </div>
    </div>

    <!-- LED State Preview -->
    <div
      v-if="isLedDevice && isOnline"
      class="mt-3 pt-3 border-t border-neutral-200 dark:border-neutral-700"
    >
      <div class="flex items-center justify-between text-sm">
        <div class="flex items-center gap-2">
          <!-- Color Preview -->
          <div
            v-if="currentDeviceState?.rgbColor"
            class="w-6 h-6 rounded-full border-2 border-white dark:border-neutral-600 shadow-sm"
            :style="{ backgroundColor: currentDeviceState.rgbColor }"
          />
          <span
            :class="
              currentDeviceState?.isOn ? 'text-green-600 dark:text-green-400' : 'text-neutral-500'
            "
          >
            {{ currentDeviceState?.isOn ? 'ON' : 'OFF' }}
          </span>
        </div>
        <div
          v-if="currentDeviceState?.brightnessPct"
          class="flex items-center gap-1 text-neutral-500"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"
            />
          </svg>
          {{ currentDeviceState.brightnessPct }}%
        </div>
      </div>
    </div>

    <!-- Offline LED -->
    <div
      v-else-if="isLedDevice && !isOnline"
      class="mt-3 pt-3 border-t border-neutral-200 dark:border-neutral-700"
    >
      <div class="flex items-center gap-2 text-sm text-neutral-400">
        <div
          class="w-6 h-6 rounded-full bg-neutral-300 dark:bg-neutral-600 border-2 border-white dark:border-neutral-700"
        />
        <span>Light unavailable</span>
      </div>
    </div>

    <!-- Sensor Values Preview -->
    <div
      v-if="isSensorDevice && isOnline"
      class="mt-3 pt-3 border-t border-neutral-200 dark:border-neutral-700"
    >
      <div class="grid grid-cols-3 gap-2 text-xs">
        <div v-if="hasCapability('temperature')" class="text-center">
          <div class="text-neutral-500">Temp</div>
          <div class="font-medium">23.5°C</div>
        </div>
        <div v-if="hasCapability('humidity')" class="text-center">
          <div class="text-neutral-500">Humidity</div>
          <div class="font-medium">45%</div>
        </div>
        <div v-if="hasCapability('light')" class="text-center">
          <div class="text-neutral-500">Light</div>
          <div class="font-medium">320 lux</div>
        </div>
      </div>
      <div v-if="sensorCount > 3" class="text-xs text-primary-500 text-center mt-2">
        +{{ sensorCount - 3 }} more sensors
      </div>
    </div>

    <!-- Offline Sensor -->
    <div
      v-else-if="isSensorDevice && !isOnline"
      class="mt-3 pt-3 border-t border-neutral-200 dark:border-neutral-700"
    >
      <div class="text-xs text-neutral-400 text-center">Sensor data unavailable</div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent } from 'vue'
import { useWebSocket } from '@/stores/websocket'

const props = defineProps({
  device: { type: Object, required: true }
})

defineEmits(['click'])

const wsStore = useWebSocket()

// Icons
const LightIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>`
})

const SensorIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>`
})

const isLedDevice = computed(() => ['LIGHT', 'LED'].includes(props.device?.type))
const isSensorDevice = computed(() => ['SENSOR', 'MULTI_SENSOR'].includes(props.device?.type))

// Get live state from WebSocket for LED devices
const liveDeviceState = computed(() => {
  if (!props.device?.id || !isLedDevice.value) {
    return null
  }
  return wsStore.deviceStates[props.device.id]
})

// Merged device state
const currentDeviceState = computed(() => {
  return liveDeviceState.value || props.device?.deviceState
})

const isOnline = computed(() => {
  const lastSeen = isLedDevice.value
    ? liveDeviceState.value?.lastSeen || props.device?.deviceState?.lastSeen
    : props.device?.deviceState?.lastSeen
  if (!lastSeen) {
    return false
  }
  const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000)
  return new Date(lastSeen) > fiveMinutesAgo
})

const deviceIcon = computed(() => (isLedDevice.value ? LightIcon : SensorIcon))

const iconBgClass = computed(() => {
  if (!isOnline.value) {
    return 'bg-neutral-400'
  }
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
    MULTI_SENSOR: 'Multi-Sensor',
    MICROCONTROLLER: 'Controller'
  }
  return labels[props.device?.type] || props.device?.type
})

const capabilities = computed(() => {
  return (
    props.device?.metaJson?.capabilities || props.device?.metaJson?.sensors?.map(s => s.id) || []
  )
})

const sensorCount = computed(() => capabilities.value.length)

const hasCapability = cap => capabilities.value.includes(cap)
</script>
