<template>
  <div class="dashboard">
    <div class="mb-8">
      <h1 class="text-3xl font-display font-semibold mb-2">Dashboard</h1>
      <p class="text-neutral-600 dark:text-neutral-400">System management and monitoring</p>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Total Users</div>
        <div class="text-3xl font-semibold">{{ stats.totalUsers }}</div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Rooms</div>
        <div class="text-3xl font-semibold">{{ stats.totalRooms }}</div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Devices</div>
        <div class="text-3xl font-semibold">{{ stats.totalDevices }}</div>
      </div>
      <div class="card p-6">
        <div class="text-sm text-neutral-600 dark:text-neutral-400 mb-1">Active Scenes</div>
        <div class="text-3xl font-semibold">{{ stats.totalScenes }}</div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- User Management -->
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
                <div class="font-medium">{{ user.name }}</div>
                <div class="text-sm text-neutral-600 dark:text-neutral-400">{{ user.email }}</div>
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
                :class="user.isActive ? 'text-red-600 hover:text-red-700' : 'text-green-600 hover:text-green-700'"
                class="text-sm font-medium"
                :disabled="user.id === currentUserId"
                @click="toggleUserStatus(user)"
              >
                {{ user.isActive ? 'Disable' : 'Enable' }}
              </button>
            </div>
          </div>
          <div v-if="users.length === 0" class="text-center text-neutral-500 py-8">
            No users found
          </div>
        </div>
      </div>

      <!-- Rooms & Devices -->
      <div class="card p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">Rooms & Devices</h2>
          <div class="space-x-2">
            <button class="btn btn-secondary text-sm" @click="showRoomModal = true">
              Add Room
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
              <div class="flex items-center gap-2">
                <h3 class="font-semibold">{{ room.name }}</h3>
                <span
                  v-if="room.isDefault"
                  class="text-xs px-2 py-0.5 bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 rounded"
                >
                  Default
                </span>
              </div>
              <button
                v-if="!room.isDefault"
                class="text-sm text-red-600 hover:text-red-700"
                @click="deleteRoom(room.id)"
              >
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
          <div v-if="rooms.length === 0" class="text-center text-neutral-500 py-8">
            No rooms found
          </div>
        </div>
      </div>
    </div>

    <!-- Add User Modal -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="showUserModal"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
        @click.self="closeUserModal"
      >
        <div class="card p-6 w-full max-w-md">
          <h3 class="text-xl font-semibold mb-4">Add New User</h3>
          
          <div v-if="userError" class="mb-4 p-3 bg-red-100 dark:bg-red-900/30 border border-red-300 dark:border-red-700 rounded-lg">
            <p class="text-sm text-red-700 dark:text-red-300">{{ userError }}</p>
          </div>

          <form @submit.prevent="addUser" class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1">Name</label>
              <input
                v-model="newUser.name"
                type="text"
                required
                class="input w-full"
                placeholder="Full name"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Email</label>
              <input
                v-model="newUser.email"
                type="email"
                required
                class="input w-full"
                placeholder="email@example.com"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Password</label>
              <input
                v-model="newUser.password"
                type="password"
                required
                minlength="6"
                class="input w-full"
                placeholder="At least 6 characters"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Role</label>
              <select v-model="newUser.role" class="input w-full">
                <option value="GUEST">Guest</option>
                <option value="RESIDENT">Resident</option>
                <option value="OWNER">Owner</option>
              </select>
            </div>
            <div class="flex gap-3 pt-2">
              <button type="button" class="btn btn-secondary flex-1" @click="closeUserModal">
                Cancel
              </button>
              <button type="submit" class="btn btn-primary flex-1" :disabled="addingUser">
                {{ addingUser ? 'Adding...' : 'Add User' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users'
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

const showUserModal = ref(false)
const showRoomModal = ref(false)
const addingUser = ref(false)
const userError = ref('')

const newUser = ref({
  name: '',
  email: '',
  password: '',
  role: 'GUEST'
})

const currentUserId = computed(() => authStore.user?.id)

const loadData = async () => {
  try {
    const [usersData, roomsData] = await Promise.all([
      usersApi.getAll(),
      roomsApi.getAll ? roomsApi.getAll() : roomsApi.getRooms()
    ])

    users.value = usersData
    rooms.value = roomsData

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

const validateName = (name) => {
  const nameRegex = /^[a-zA-Z\s'-]+$/
  return nameRegex.test(name)
}

const addUser = async () => {
  userError.value = ''
  
  // Frontend validation
  if (!validateName(newUser.value.name)) {
    userError.value = 'Name can only contain letters, spaces, hyphens, and apostrophes'
    return
  }
  
  addingUser.value = true
  
  try {
    await usersApi.create({
      email: newUser.value.email,
      password: newUser.value.password,
      name: newUser.value.name,
      role: newUser.value.role
    })
    
    await loadData()
    closeUserModal()
  } catch (error) {
    userError.value = error.response?.data?.error || error.message || 'Failed to add user'
  } finally {
    addingUser.value = false
  }
}

const closeUserModal = () => {
  showUserModal.value = false
  userError.value = ''
  newUser.value = { name: '', email: '', password: '', role: 'GUEST' }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  min-height: calc(100vh - 200px);
}
</style>
