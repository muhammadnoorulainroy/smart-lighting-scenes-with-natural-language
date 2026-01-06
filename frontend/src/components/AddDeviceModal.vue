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
      v-if="show"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <div class="card p-6 max-w-md w-full max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">Add New Device</h2>
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

        <form @submit.prevent="handleSubmit">
          <div class="space-y-4">
            <!-- Device Type -->
            <div>
              <label class="block text-sm font-medium mb-1">Device Type *</label>
              <select v-model="form.type" class="input w-full" required>
                <option value="" disabled>Select device type</option>
                <option value="SENSOR">Sensor (Single)</option>
                <option value="MULTI_SENSOR">Device with Multiple Sensors</option>
                <option value="LED">LED (RGB Light)</option>
                <option value="MICROCONTROLLER">Micro-controller</option>
              </select>
            </div>

            <!-- Room Selection (not shown for MICROCONTROLLER) -->
            <div v-if="form.type !== 'MICROCONTROLLER'">
              <label class="block text-sm font-medium mb-1">Room *</label>
              <select
                v-model="form.roomId"
                class="input w-full"
                :required="form.type !== 'MICROCONTROLLER'"
              >
                <option value="" disabled>Select a room</option>
                <option v-for="room in rooms" :key="room.id" :value="room.id">
                  {{ room.name }}
                </option>
              </select>
              <p class="text-xs text-neutral-500 mt-1">Device must be assigned to a room</p>
            </div>

            <!-- Device Name -->
            <div>
              <label class="block text-sm font-medium mb-1">Device Name *</label>
              <input
                v-model="form.name"
                type="text"
                class="input w-full"
                placeholder="e.g., Bedroom Light, Living Room Sensor"
                required
              />
            </div>

            <!-- LED-specific fields -->
            <div v-if="form.type === 'LED'">
              <label class="block text-sm font-medium mb-1">LED Index</label>
              <input
                v-model.number="form.ledIndex"
                type="number"
                class="input w-full"
                min="0"
                max="255"
                placeholder="LED strip index (0-255)"
              />
            </div>

            <!-- Single Sensor fields -->
            <div v-if="form.type === 'SENSOR'">
              <label class="block text-sm font-medium mb-1">Sensor Type</label>
              <select v-model="form.sensorType" class="input w-full">
                <option value="">Select sensor type</option>
                <option v-for="sensor in availableSensors" :key="sensor.id" :value="sensor.id">
                  {{ sensor.name }}
                </option>
              </select>
            </div>

            <!-- Multi-Sensor dropdown with checkboxes -->
            <div v-if="form.type === 'MULTI_SENSOR'">
              <label class="block text-sm font-medium mb-1">Select Sensors</label>
              <div class="relative">
                <button
                  type="button"
                  class="input w-full text-left flex items-center justify-between"
                  @click="sensorDropdownOpen = !sensorDropdownOpen"
                >
                  <span v-if="selectedSensorCount === 0" class="text-neutral-500">
                    Select sensors...
                  </span>
                  <span v-else>{{ selectedSensorCount }} sensor(s) selected</span>
                  <svg
                    class="w-5 h-5 transition-transform"
                    :class="{ 'rotate-180': sensorDropdownOpen }"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M19 9l-7 7-7-7"
                    />
                  </svg>
                </button>

                <!-- Dropdown content -->
                <div
                  v-if="sensorDropdownOpen"
                  class="absolute z-10 mt-1 w-full bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-lg shadow-lg max-h-72 overflow-y-auto"
                >
                  <!-- Preset buttons -->
                  <div
                    class="px-4 py-2 border-b border-neutral-200 dark:border-neutral-700 flex gap-2"
                  >
                    <button
                      type="button"
                      class="text-xs px-2 py-1 bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 rounded hover:bg-primary-200 dark:hover:bg-primary-800"
                      @click="selectAllFeatherSense"
                    >
                      Adafruit Feather Sense (All)
                    </button>
                    <button
                      type="button"
                      class="text-xs px-2 py-1 bg-neutral-100 dark:bg-neutral-700 text-neutral-700 dark:text-neutral-300 rounded hover:bg-neutral-200 dark:hover:bg-neutral-600"
                      @click="form.selectedSensors = []"
                    >
                      Clear All
                    </button>
                  </div>
                  <label
                    v-for="sensor in availableSensors"
                    :key="sensor.id"
                    class="flex items-center px-4 py-2 hover:bg-neutral-100 dark:hover:bg-neutral-700 cursor-pointer"
                  >
                    <input
                      v-model="form.selectedSensors"
                      type="checkbox"
                      :value="sensor.id"
                      class="mr-3 h-4 w-4 text-primary-500 rounded border-neutral-300 focus:ring-primary-500"
                    />
                    <div>
                      <div class="font-medium">{{ sensor.name }}</div>
                      <div class="text-xs text-neutral-500">{{ sensor.description }}</div>
                    </div>
                  </label>
                </div>
              </div>
              <p class="text-xs text-neutral-500 mt-1">
                Select all sensors available on this device
              </p>
            </div>

            <!-- Microcontroller fields -->
            <div v-if="form.type === 'MICROCONTROLLER'">
              <label class="block text-sm font-medium mb-1">Device ID</label>
              <input
                v-model="form.deviceId"
                type="text"
                class="input w-full"
                placeholder="e.g., esp32-001"
              />
              <p class="text-xs text-neutral-500 mt-1">Unique identifier for the microcontroller</p>
            </div>

          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
            <button
              type="submit"
              class="btn btn-primary"
              :disabled="loading || (!form.roomId && form.type !== 'MICROCONTROLLER')"
            >
              {{ loading ? 'Creating...' : 'Create Device' }}
            </button>
          </div>

          <p v-if="error" class="text-red-500 text-sm mt-4">{{ error }}</p>
        </form>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { devicesApi } from '../api/devices'

const props = defineProps({
  show: { type: Boolean, required: true },
  rooms: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'created'])

// Available sensors for multi-sensor devices
const availableSensors = [
  {
    id: 'temperature',
    name: 'Temperature',
    description: 'BMP280 - Ambient temperature (°C)',
    chip: 'BMP280'
  },
  {
    id: 'pressure',
    name: 'Barometric Pressure',
    description: 'BMP280 - Pressure/altitude (hPa)',
    chip: 'BMP280'
  },
  { id: 'humidity', name: 'Humidity', description: 'SHT31 - Relative humidity (%)', chip: 'SHT31' },
  {
    id: 'light',
    name: 'Light/Luminosity',
    description: 'APDS9960 - Ambient light (lux)',
    chip: 'APDS9960'
  },
  {
    id: 'proximity',
    name: 'Proximity',
    description: 'APDS9960 - Nearby objects',
    chip: 'APDS9960'
  },
  { id: 'color', name: 'Color (RGB)', description: 'APDS9960 - Color values', chip: 'APDS9960' },
  { id: 'gesture', name: 'Gesture', description: 'APDS9960 - Hand gestures', chip: 'APDS9960' },
  {
    id: 'accelerometer',
    name: 'Accelerometer',
    description: 'LSM6DS3TR-C - 6-DoF (m/s²)',
    chip: 'LSM6DS3TR-C'
  },
  {
    id: 'gyroscope',
    name: 'Gyroscope',
    description: 'LSM6DS3TR-C - 6-DoF (°/s)',
    chip: 'LSM6DS3TR-C'
  },
  {
    id: 'magnetometer',
    name: 'Magnetometer',
    description: 'LIS3MDL - Magnetic field (µT)',
    chip: 'LIS3MDL'
  },
  {
    id: 'audio',
    name: 'Audio/Sound Level',
    description: 'PDM Microphone - Sound (dB)',
    chip: 'PDM'
  }
]

// Preset: All sensors on Adafruit Feather nRF52840 Sense
const ADAFRUIT_FEATHER_SENSE_SENSORS = [
  'temperature',
  'pressure',
  'humidity',
  'light',
  'proximity',
  'color',
  'gesture',
  'accelerometer',
  'gyroscope',
  'magnetometer',
  'audio'
]

const form = ref({
  roomId: '',
  name: '',
  type: '',
  ledIndex: null,
  sensorType: '',
  selectedSensors: [],
  deviceId: ''
})

const loading = ref(false)
const error = ref('')
const sensorDropdownOpen = ref(false)

const selectedSensorCount = computed(() => form.value.selectedSensors.length)

watch(
  () => props.show,
  newVal => {
    if (newVal) {
      form.value = {
        roomId: '',
        name: '',
        type: '',
        ledIndex: null,
        sensorType: '',
        selectedSensors: [],
        deviceId: ''
      }
      error.value = ''
      sensorDropdownOpen.value = false
    }
  }
)

// Close dropdown when clicking outside
watch(sensorDropdownOpen, newVal => {
  if (newVal) {
    const closeDropdown = e => {
      if (!e.target.closest('.relative')) {
        sensorDropdownOpen.value = false
        document.removeEventListener('click', closeDropdown)
      }
    }
    setTimeout(() => document.addEventListener('click', closeDropdown), 0)
  }
})

// Clear room when switching to MICROCONTROLLER
watch(
  () => form.value.type,
  newType => {
    if (newType === 'MICROCONTROLLER') {
      form.value.roomId = ''
    }
  }
)

// Select all sensors for Adafruit Feather nRF52840 Sense
const selectAllFeatherSense = () => {
  form.value.selectedSensors = [...ADAFRUIT_FEATHER_SENSE_SENSORS]
}

const handleSubmit = async () => {
  if (form.value.type !== 'MICROCONTROLLER' && !form.value.roomId) {
    error.value = 'Please select a room'
    return
  }
  if (!form.value.name.trim()) {
    error.value = 'Device name is required'
    return
  }
  if (!form.value.type) {
    error.value = 'Device type is required'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const deviceData = {
      roomId: form.value.type !== 'MICROCONTROLLER' ? form.value.roomId : null,
      name: form.value.name,
      type: form.value.type,
      metaJson: buildMetaJson()
    }

    const newDevice = await devicesApi.create(deviceData)
    emit('created', newDevice)
    emit('close')
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to create device'
  } finally {
    loading.value = false
  }
}

const buildMetaJson = () => {
  const meta = {}

  switch (form.value.type) {
    case 'LED':
      meta.led_index = form.value.ledIndex
      meta.rgb = true
      meta.brightness = true
      break

    case 'SENSOR':
      meta.sensor_type = form.value.sensorType
      meta.capabilities = form.value.sensorType ? [form.value.sensorType] : []
      break

    case 'MULTI_SENSOR':
      meta.sensors = form.value.selectedSensors
      meta.capabilities = form.value.selectedSensors
      break

    case 'MICROCONTROLLER':
      meta.device_id = form.value.deviceId
      meta.type = 'controller'
      break
  }

  return meta
}
</script>
