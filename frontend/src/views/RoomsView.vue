<template>
  <div class="rooms">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-3xl font-display font-semibold mb-2">Rooms</h1>
        <p class="text-neutral-600 dark:text-neutral-400">
          Manage your smart lighting rooms and devices
        </p>
      </div>
      <div v-if="canEdit" class="flex gap-3">
        <button class="btn btn-secondary" @click="showAddDeviceModal = true">
          Add Device
        </button>
        <button class="btn btn-primary" @click="showAddRoomModal = true">
          Add Room
        </button>
      </div>
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
      <svg
        class="w-12 h-12 mx-auto mb-4 text-red-500"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
        />
      </svg>
      <h3 class="text-lg font-semibold mb-2 text-red-700 dark:text-red-300">
        Error Loading Rooms
      </h3>
      <p class="text-red-600 dark:text-red-400 mb-4">{{ error }}</p>
      <button class="btn btn-primary" @click="loadRooms">Retry</button>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="rooms.length === 0"
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
          d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
        />
      </svg>
      <h3 class="text-xl font-semibold mb-2">No Rooms Yet</h3>
      <p class="text-neutral-600 dark:text-neutral-400 mb-6">
        Get started by creating your first room and adding devices
      </p>
      <button v-if="canEdit" class="btn btn-primary" @click="showAddRoomModal = true">Create Your First Room</button>
    </div>

    <!-- Rooms Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <RoomCard
        v-for="room in rooms"
        :key="room.id"
        :room="room"
        :devices="room.devices || room.devicesList || []"
        :is-expanded="expandedRoomId === room.id"
        :can-edit="canEdit"
        @toggle="toggleRoom(room.id)"
        @device-click="handleViewDevice"
        @delete="handleDeleteRoom"
      />
    </div>

    <!-- Modals -->
    <AddRoomModal
      :show="showAddRoomModal"
      @close="showAddRoomModal = false"
      @created="handleRoomAdded"
    />

    <AddDeviceModal
      :show="showAddDeviceModal"
      :rooms="rooms"
      @close="showAddDeviceModal = false"
      @created="handleDeviceAdded"
    />

    <DeviceDetailModal
      v-if="selectedDevice"
      :show="showDeviceDetailModal"
      :device="selectedDevice"
      :can-edit="canEdit"
      @close="handleCloseDeviceDetail"
      @updated="handleDeviceUpdated"
    />

    <!-- Delete Room Confirmation Modal -->
    <ConfirmModal
      :show="showDeleteModal"
      title="Delete Room"
      :message="`Are you sure you want to delete '${roomToDelete?.name}'? All devices in this room will also be deleted.`"
      confirm-text="Delete"
      type="danger"
      :loading="deleting"
      @confirm="confirmDeleteRoom"
      @cancel="cancelDeleteRoom"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { roomsApi } from '../api/rooms'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../stores/toast'
import RoomCard from '../components/rooms/RoomCard.vue'
import AddRoomModal from '../components/AddRoomModal.vue'
import AddDeviceModal from '../components/AddDeviceModal.vue'
import DeviceDetailModal from '../components/rooms/DeviceDetailModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

const authStore = useAuthStore()
const toast = useToast()

// Role-based access - GUEST can only view rooms
const canEdit = computed(() => authStore.isResident)

// State
const rooms = ref([])
const loading = ref(false)
const error = ref(null)
const showAddRoomModal = ref(false)
const showAddDeviceModal = ref(false)
const showDeviceDetailModal = ref(false)
const selectedDevice = ref(null)
const expandedRoomId = ref(null)

// Delete modal state
const showDeleteModal = ref(false)
const roomToDelete = ref(null)
const deleting = ref(false)

// Load rooms from API
const loadRooms = async () => {
  loading.value = true
  error.value = null
  try {
    rooms.value = await roomsApi.getAll()
  } catch (err) {
    console.error('Error loading rooms:', err)
    error.value = err.response?.data?.message || 'Failed to load rooms. Please try again.'
  } finally {
    loading.value = false
  }
}

// Handle room added
const handleRoomAdded = () => {
  loadRooms() // Reload to get fresh data
  showAddRoomModal.value = false
  toast.success('Room created successfully')
}

// Handle device added
const handleDeviceAdded = () => {
  loadRooms() // Reload rooms to get updated device list
  showAddDeviceModal.value = false
  toast.success('Device added successfully')
}

// Handle toggle room expand/collapse
const toggleRoom = (roomId) => {
  expandedRoomId.value = expandedRoomId.value === roomId ? null : roomId
}

// Handle view device
const handleViewDevice = (device) => {
  selectedDevice.value = device
  showDeviceDetailModal.value = true
}

// Handle close device detail
const handleCloseDeviceDetail = () => {
  showDeviceDetailModal.value = false
  selectedDevice.value = null
}

// Handle device updated
const handleDeviceUpdated = () => {
  loadRooms() // Reload rooms to get updated data
}

// Handle delete room - open modal
const handleDeleteRoom = (room) => {
  roomToDelete.value = room
  showDeleteModal.value = true
}

const cancelDeleteRoom = () => {
  showDeleteModal.value = false
  roomToDelete.value = null
}

const confirmDeleteRoom = async () => {
  if (!roomToDelete.value) return

  deleting.value = true
  try {
    await roomsApi.deleteRoom(roomToDelete.value.id)
    rooms.value = rooms.value.filter((r) => r.id !== roomToDelete.value.id)
    toast.success('Room deleted successfully')
    showDeleteModal.value = false
    roomToDelete.value = null
  } catch (err) {
    console.error('Error deleting room:', err)
    toast.error('Failed to delete room')
  } finally {
    deleting.value = false
  }
}

// Load rooms on mount
onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
/* Any additional styles */
</style>
