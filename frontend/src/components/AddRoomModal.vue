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
      <div class="card p-6 max-w-md w-full">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold">Add New Room</h2>
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
            <div>
              <label class="block text-sm font-medium mb-1">Room Name *</label>
              <input
                v-model="form.name"
                type="text"
                class="input w-full"
                placeholder="e.g., Office, Garage"
                required
              />
            </div>

            <div>
              <label class="block text-sm font-medium mb-1">Description</label>
              <textarea
                v-model="form.description"
                class="input w-full"
                rows="3"
                placeholder="Optional description"
              />
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Creating...' : 'Create Room' }}
            </button>
          </div>

          <p v-if="error" class="text-red-500 text-sm mt-4">{{ error }}</p>
        </form>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { roomsApi } from '../api/rooms'

const props = defineProps({
  show: { type: Boolean, required: true }
})

const emit = defineEmits(['close', 'created'])

const form = ref({
  name: '',
  description: ''
})
const loading = ref(false)
const error = ref('')

watch(
  () => props.show,
  newVal => {
    if (newVal) {
      form.value = { name: '', description: '' }
      error.value = ''
    }
  }
)

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    error.value = 'Room name is required'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const newRoom = await roomsApi.createRoom(form.value)
    emit('created', newRoom)
    emit('close')
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to create room'
  } finally {
    loading.value = false
  }
}
</script>
