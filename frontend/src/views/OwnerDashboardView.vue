<template>
  <div class="owner-dashboard">
    <div class="mb-8">
      <h1 class="text-3xl font-display font-semibold mb-2">Owner Dashboard</h1>
      <p class="text-neutral-600 dark:text-neutral-400">System management and monitoring</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6 mb-8">
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Total Users</div>
        <div class="text-3xl font-semibold">
          {{ stats.totalUsers }}
        </div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Rooms</div>
        <div class="text-3xl font-semibold">
          {{ stats.totalRooms }}
        </div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Devices</div>
        <div class="text-3xl font-semibold">
          {{ stats.totalDevices }}
        </div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Active Scenes</div>
        <div class="text-3xl font-semibold">
          {{ stats.totalScenes }}
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="card p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">User Management</h2>
          <button class="btn btn-primary text-sm" @click="showUserModal = true">Add User</button>
        </div>
        <div class="space-y-3">
          <div
            v-for="user in users"
            :key="user.id"
            class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center space-x-3">
              <div
                class="w-10 h-10 rounded-full bg-primary-500 flex items-center justify-center text-white font-semibold"
              >
                {{ user.name.charAt(0).toUpperCase() }}
              </div>
              <div>
                <div class="font-medium">
                  {{ user.name }}
                </div>
                <div class="text-sm text-neutral-600 dark:text-neutral-400">
                  {{ user.email }}
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-3">
              <select
                :value="user.role"
                class="input text-sm py-1 px-2"
                :disabled="user.id === currentUserId"
                @change="updateUserRole(user.id, $event.target.value)"
              >
                <option value="OWNER">Owner</option>
                <option value="RESIDENT">Resident</option>
                <option value="GUEST">Guest</option>
              </select>
              <button
                :class="user.isActive ? 'text-red-600' : 'text-green-600'"
                class="text-sm font-medium"
                :disabled="user.id === currentUserId"
                @click="toggleUserStatus(user)"
              >
                {{ user.isActive ? 'Disable' : 'Enable' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">Recent Events</h2>
          <router-link to="/logs" class="text-sm text-primary-500 hover:text-primary-600">
            View All
          </router-link>
        </div>
        <div class="space-y-3 max-h-96 overflow-y-auto">
          <div
            v-for="event in recentEvents"
            :key="event.id"
            class="p-3 border-l-4 border-primary-500 bg-neutral-50 dark:bg-neutral-900 rounded"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-sm font-medium">{{ formatEventType(event.type) }}</span>
              <span class="text-xs text-neutral-500">{{ formatTime(event.timestamp) }}</span>
            </div>
            <div class="text-sm text-neutral-600 dark:text-neutral-400">
              {{ formatEventDetails(event) }}
            </div>
          </div>
          <div v-if="recentEvents.length === 0" class="text-center text-neutral-500 py-8">
            No events found
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
      <div class="card p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">Rooms & Devices</h2>
          <div class="space-x-2">
            <button class="btn btn-secondary text-sm" @click="showRoomModal = true">
              Add Room
            </button>
            <button class="btn btn-secondary text-sm" @click="showDeviceModal = true">
              Add Device
            </button>
          </div>
        </div>
        <div class="space-y-4">
          <div
            v-for="room in rooms"
            :key="room.id"
            class="p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-semibold">
                {{ room.name }}
              </h3>
              <button class="text-sm text-red-600 hover:text-red-700" @click="deleteRoom(room.id)">
                Delete
              </button>
            </div>
            <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-2">
              {{ room.description }}
            </div>
            <div class="text-sm">
              <span class="font-medium">{{ room.devices?.length || 0 }}</span> devices
            </div>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">System Settings</h2>
          <button class="btn btn-secondary text-sm" @click="showSettingsModal = true">
            Configure
          </button>
        </div>
        <div class="space-y-4">
          <div
            class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div>
              <div class="font-medium">MQTT Broker</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400">
                {{ settings.mqttHost || 'Not configured' }}
              </div>
            </div>
            <span
              class="text-xs px-2 py-1 rounded"
              :class="
                settings.mqttConnected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              "
            >
              {{ settings.mqttConnected ? 'Connected' : 'Disconnected' }}
            </span>
          </div>
          <div
            class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div>
              <div class="font-medium">Location</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400">
                {{ settings.latitude }}, {{ settings.longitude }}
              </div>
            </div>
          </div>
          <div
            class="flex items-center justify-between p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"
          >
            <div>
              <div class="font-medium">LLM Model</div>
              <div class="text-sm text-neutral-600 dark:text-neutral-400">
                {{ settings.llmModel || 'Not configured' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users'
import { eventsApi } from '../api/events'
import { roomsApi } from '../api/rooms'

const authStore = useAuthStore()

const stats = ref({
  totalUsers: 0,
  totalRooms: 0,
  totalDevices: 0,
  totalScenes: 0
})

const users = ref([])
const rooms = ref([])
const recentEvents = ref([])
const settings = ref({
  mqttHost: 'localhost:1883',
  mqttConnected: false,
  latitude: '37.7749',
  longitude: '-122.4194',
  llmModel: 'llama2'
})

const showUserModal = ref(false)
const showRoomModal = ref(false)
const showDeviceModal = ref(false)
const showSettingsModal = ref(false)

const currentUserId = computed(() => authStore.user?.id)

const loadData = async () => {
  try {
    const [usersData, roomsData, eventsData] = await Promise.all([
      usersApi.getAll(),
      roomsApi.getAll ? roomsApi.getAll() : roomsApi.getRooms(),
      eventsApi.getAll({ limit: 10 })
    ])

    users.value = usersData
    rooms.value = roomsData
    recentEvents.value = eventsData.content || eventsData || []

    stats.value = {
      totalUsers: users.value.length,
      totalRooms: rooms.value.length,
      totalDevices: rooms.value.reduce((sum, room) => sum + (room.devices?.length || 0), 0),
      totalScenes: 0
    }
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  }
}

const updateUserRole = async (userId, newRole) => {
  try {
    await usersApi.updateRole(userId, newRole)
    await loadData()
  } catch (error) {
    console.error('Failed to update user role:', error)
  }
}

const toggleUserStatus = async user => {
  try {
    if (user.isActive) {
      await usersApi.disable(user.id)
    } else {
      await usersApi.enable(user.id)
    }
    await loadData()
  } catch (error) {
    console.error('Failed to toggle user status:', error)
  }
}

const deleteRoom = async roomId => {
  if (!confirm('Are you sure you want to delete this room?')) {
    return
  }
  try {
    await roomsApi.deleteRoom(roomId)
    await loadData()
  } catch (error) {
    console.error('Failed to delete room:', error)
  }
}

const formatTime = timestamp => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

const formatEventType = type => {
  return type?.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()) || 'Unknown'
}

const formatEventDetails = event => {
  if (event.details_json && typeof event.details_json === 'object') {
    return JSON.stringify(event.details_json)
  }
  return event.details || 'No details available'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.owner-dashboard {
  min-height: calc(100vh - 200px);
}
</style>
