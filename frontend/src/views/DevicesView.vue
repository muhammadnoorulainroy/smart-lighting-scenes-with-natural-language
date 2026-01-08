<template>
  <div class="devices-page container mx-auto px-4 py-8">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-3xl font-display font-semibold mb-2">Devices</h1>
        <p class="text-neutral-600 dark:text-neutral-400">Manage all your smart lighting devices</p>
      </div>
      <button v-if="canEdit" class="btn btn-primary" @click="showAddDeviceModal = true">
        Add Device
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
    </div>

    <!-- Error State -->
    <div
      v-else-if="error"
      class="card p-6 text-center bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800"
    >
      <p class="text-red-600 dark:text-red-400">{{ error }}</p>
      <button class="btn btn-secondary mt-4" @click="loadDevices">Retry</button>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="devices.length === 0"
      class="card p-12 text-center bg-neutral-50 dark:bg-neutral-800/50"
    >
      <svg
        class="w-16 h-16 mx-auto mb-4 text-neutral-400"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"
        />
      </svg>
      <h3 class="text-xl font-semibold mb-2">No Devices Yet</h3>
      <p class="text-neutral-600 dark:text-neutral-400 mb-6">
        Add your first device to get started
      </p>
      <button v-if="canEdit" class="btn btn-primary" @click="showAddDeviceModal = true">
        Add Your First Device
      </button>
    </div>

    <!-- Device Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="device in devices"
        :key="device.id"
        class="card p-4 hover:shadow-lg transition-shadow cursor-pointer"
        @click="openDeviceDetail(device)"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-3">
            <div
              class="w-10 h-10 rounded-lg flex items-center justify-center"
              :class="getDeviceIconClass(device)"
            >
              <component :is="getDeviceIcon(device)" class="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 class="font-medium">{{ device.name }}</h3>
              <p class="text-sm text-neutral-500">{{ device.roomName || 'No Room' }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span
              class="text-xs px-2 py-1 rounded-full"
              :class="
                isOnline(device)
                  ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                  : 'bg-neutral-100 text-neutral-500 dark:bg-neutral-800'
              "
            >
              {{ isOnline(device) ? 'Online' : 'Offline' }}
            </span>
            <button
              v-if="canEdit && !isDefaultDevice(device)"
              class="p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-red-500 hover:text-red-600"
              title="Delete device"
              @click.stop="openDeleteModal(device)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                />
              </svg>
            </button>
          </div>
        </div>

        <div class="text-xs text-neutral-500 space-y-1">
          <p>Type: {{ formatDeviceType(device.type) }}</p>
          <p v-if="device.deviceState?.lastSeen">
            Last seen: {{ formatTime(device.deviceState.lastSeen) }}
          </p>
        </div>
      </div>
    </div>

    <!-- Add Device Modal -->
    <AddDeviceModal
      :show="showAddDeviceModal"
      :rooms="rooms"
      @close="showAddDeviceModal = false"
      @created="handleDeviceCreated"
    />

    <!-- Device Detail Modal -->
    <DeviceDetailModal
      v-if="selectedDevice"
      :show="showDeviceDetailModal"
      :device="selectedDevice"
      :can-edit="canEdit"
      @close="closeDeviceDetail"
      @updated="handleDeviceUpdated"
    />

    <!-- Delete Confirmation Modal -->
    <ConfirmModal
      :show="showDeleteModal"
      title="Delete Device"
      :message="`Are you sure you want to delete '${deviceToDelete?.name}'? This action cannot be undone.`"
      confirm-text="Delete"
      type="danger"
      :loading="deleting"
      @confirm="confirmDeleteDevice"
      @cancel="cancelDeleteDevice"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineComponent } from 'vue'
import { devicesApi } from '../api/devices'
import { roomsApi } from '../api/rooms'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../stores/toast'
import AddDeviceModal from '../components/AddDeviceModal.vue'
import DeviceDetailModal from '../components/rooms/DeviceDetailModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

const authStore = useAuthStore()
const toast = useToast()
const canEdit = computed(() => authStore.isResident)

// State
const devices = ref([])
const rooms = ref([])
const loading = ref(false)
const error = ref(null)
const showAddDeviceModal = ref(false)
const showDeviceDetailModal = ref(false)
const selectedDevice = ref(null)

// Delete modal state
const showDeleteModal = ref(false)
const deviceToDelete = ref(null)
const deleting = ref(false)

// Check if a device is an ORIGINAL default device (cannot be deleted)
// Only the 5 original LEDs (led_index 0-4) and 2 original sensors are non-deletable
const isDefaultDevice = device => {
  const meta = device.metaJson || {}

  // Default LEDs have led_index 0-4
  if (
    ['LED', 'LIGHT'].includes(device.type) &&
    meta.led_index !== undefined &&
    meta.led_index >= 0 &&
    meta.led_index <= 4
  ) {
    return true
  }

  // Default sensors have specific sensor_ids like "SmartLight-Sensor-1"
  if (
    ['SENSOR', 'MULTI_SENSOR'].includes(device.type) &&
    meta.sensor_id &&
    meta.sensor_id.startsWith('SmartLight-Sensor')
  ) {
    return true
  }

  return false
}

// Icons
const LightIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>`
})

const SensorIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>`
})

const ChipIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" /></svg>`
})

// Load data
const loadDevices = async () => {
  loading.value = true
  error.value = null
  try {
    devices.value = await devicesApi.getAll()
  } catch (err) {
    error.value = 'Failed to load devices'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadRooms = async () => {
  try {
    rooms.value = await roomsApi.getAll()
  } catch (err) {
    console.error('Failed to load rooms:', err)
  }
}

onMounted(() => {
  loadDevices()
  loadRooms()
})

// Device helpers
const getDeviceIcon = device => {
  if (['LIGHT', 'LED'].includes(device.type)) {
    return LightIcon
  }
  if (['SENSOR', 'MULTI_SENSOR'].includes(device.type)) {
    return SensorIcon
  }
  return ChipIcon
}

const getDeviceIconClass = device => {
  if (['LIGHT', 'LED'].includes(device.type)) {
    return 'bg-gradient-to-br from-yellow-400 to-orange-500'
  }
  if (['SENSOR', 'MULTI_SENSOR'].includes(device.type)) {
    return 'bg-gradient-to-br from-green-400 to-teal-500'
  }
  return 'bg-gradient-to-br from-blue-400 to-indigo-500'
}

const formatDeviceType = type => {
  const types = {
    LED: 'LED Light',
    LIGHT: 'Light',
    SENSOR: 'Sensor',
    MULTI_SENSOR: 'Multi-Sensor',
    MICROCONTROLLER: 'Controller'
  }
  return types[type] || type
}

const isOnline = device => {
  const lastSeen = device?.deviceState?.lastSeen
  if (!lastSeen) {
    return false
  }
  const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000)
  return new Date(lastSeen) > fiveMinutesAgo
}

const formatTime = timestamp => {
  if (!timestamp) {
    return 'Never'
  }
  const date = new Date(timestamp)
  return date.toLocaleString()
}

// Actions
const openDeviceDetail = device => {
  selectedDevice.value = device
  showDeviceDetailModal.value = true
}

const closeDeviceDetail = () => {
  showDeviceDetailModal.value = false
  selectedDevice.value = null
}

const handleDeviceCreated = newDevice => {
  devices.value.push(newDevice)
  showAddDeviceModal.value = false
  toast.success('Device created successfully')
}

const handleDeviceUpdated = () => {
  loadDevices()
}

// Delete modal handlers
const openDeleteModal = device => {
  deviceToDelete.value = device
  showDeleteModal.value = true
}

const cancelDeleteDevice = () => {
  showDeleteModal.value = false
  deviceToDelete.value = null
}

const confirmDeleteDevice = async () => {
  if (!deviceToDelete.value) {
    return
  }

  deleting.value = true
  try {
    await devicesApi.delete(deviceToDelete.value.id)
    devices.value = devices.value.filter(d => d.id !== deviceToDelete.value.id)
    toast.success('Device deleted successfully')
    showDeleteModal.value = false
    deviceToDelete.value = null
  } catch (err) {
    console.error('Failed to delete device:', err)
    toast.error('Failed to delete device')
  } finally {
    deleting.value = false
  }
}
</script>
