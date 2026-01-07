<template>
  <div
    class="room-card card overflow-hidden cursor-pointer transition-all hover:shadow-xl hover:-translate-y-1"
    :class="{ 'ring-2 ring-primary-500': isExpanded }"
    @click="$emit('toggle')"
  >
    <!-- Room Header -->
    <div class="p-5">
      <div class="flex items-start justify-between">
        <div class="flex items-center gap-3">
          <div :class="roomIconClass" class="w-12 h-12 rounded-xl flex items-center justify-center">
            <component :is="roomIcon" class="w-6 h-6 text-white" />
          </div>
          <div>
            <h3 class="font-semibold text-lg">{{ room.name }}</h3>
            <p class="text-sm text-neutral-500">
              {{ deviceCount }} device{{ deviceCount !== 1 ? 's' : '' }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <!-- Default Badge -->
          <span
            v-if="room.isDefault"
            class="px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 rounded-full"
          >
            Default
          </span>
          <!-- Delete Button -->
          <button
            v-if="canEdit && !room.isDefault"
            class="p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-red-500 hover:text-red-600"
            title="Delete room"
            @click.stop="$emit('delete', room)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
          <!-- Expand Icon -->
          <svg
            class="w-5 h-5 text-neutral-400 transition-transform"
            :class="{ 'rotate-180': isExpanded }"
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
        </div>
      </div>

      <!-- Room Stats -->
      <div class="mt-4 flex items-center gap-4 text-sm">
        <div class="flex items-center gap-1.5">
          <div
            class="w-2 h-2 rounded-full"
            :class="onlineLedCount > 0 ? 'bg-yellow-400' : 'bg-neutral-300'"
          />
          <span class="text-neutral-600 dark:text-neutral-400"
            >{{ ledCount }} LED{{ ledCount !== 1 ? 's' : '' }}</span
          >
        </div>
        <div class="flex items-center gap-1.5">
          <div
            class="w-2 h-2 rounded-full"
            :class="onlineSensorCount > 0 ? 'bg-cyan-400' : 'bg-neutral-300'"
          />
          <span class="text-neutral-600 dark:text-neutral-400"
            >{{ sensorCount }} Sensor{{ sensorCount !== 1 ? 's' : '' }}</span
          >
        </div>
      </div>

      <!-- Active Scene (if any) -->
      <div
        v-if="activeScene"
        class="mt-3 px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg"
      >
        <div class="flex items-center gap-2 text-sm">
          <svg
            class="w-4 h-4 text-primary-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"
            />
          </svg>
          <span class="text-primary-700 dark:text-primary-300 font-medium">{{ activeScene }}</span>
        </div>
      </div>
    </div>

    <!-- Expanded Content -->
    <transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="max-h-0 opacity-0"
      enter-to-class="max-h-[1000px] opacity-100"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="max-h-[1000px] opacity-100"
      leave-to-class="max-h-0 opacity-0"
    >
      <div
        v-if="isExpanded"
        class="border-t border-neutral-200 dark:border-neutral-700 overflow-hidden"
      >
        <div class="p-5 space-y-4">
          <h4 class="font-medium text-neutral-700 dark:text-neutral-300">Devices</h4>

          <div v-if="devices.length > 0" class="grid gap-3">
            <DeviceCard
              v-for="device in devices"
              :key="device.id"
              :device="device"
              @click="$emit('deviceClick', device)"
            />
          </div>

          <div v-else class="text-center py-8 text-neutral-500">
            <svg
              class="w-12 h-12 mx-auto text-neutral-300 mb-3"
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
            <p>No devices in this room</p>
            <p class="text-sm">Add devices from the Dashboard</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, defineComponent } from 'vue'
import DeviceCard from './DeviceCard.vue'

const props = defineProps({
  room: { type: Object, required: true },
  devices: { type: Array, default: () => [] },
  isExpanded: { type: Boolean, default: false },
  activeScene: { type: String, default: null },
  canEdit: { type: Boolean, default: true }
})

defineEmits(['toggle', 'deviceClick', 'delete'])

// Room Icons
const BedroomIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" /></svg>`
})

const LivingRoomIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>`
})

const KitchenIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>`
})

const BathroomIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" /></svg>`
})

const HallwayIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z" /></svg>`
})

const DefaultRoomIcon = defineComponent({
  template: `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" /></svg>`
})

const roomIcon = computed(() => {
  const name = props.room.name?.toLowerCase()
  if (name?.includes('bedroom')) {
    return BedroomIcon
  }
  if (name?.includes('living')) {
    return LivingRoomIcon
  }
  if (name?.includes('kitchen')) {
    return KitchenIcon
  }
  if (name?.includes('bath')) {
    return BathroomIcon
  }
  if (name?.includes('hallway') || name?.includes('hall')) {
    return HallwayIcon
  }
  return DefaultRoomIcon
})

const roomIconClass = computed(() => {
  const name = props.room.name?.toLowerCase()
  if (name?.includes('bedroom')) {
    return 'bg-gradient-to-br from-purple-400 to-purple-600'
  }
  if (name?.includes('living')) {
    return 'bg-gradient-to-br from-blue-400 to-blue-600'
  }
  if (name?.includes('kitchen')) {
    return 'bg-gradient-to-br from-orange-400 to-orange-600'
  }
  if (name?.includes('bath')) {
    return 'bg-gradient-to-br from-cyan-400 to-cyan-600'
  }
  if (name?.includes('hallway') || name?.includes('hall')) {
    return 'bg-gradient-to-br from-emerald-400 to-emerald-600'
  }
  return 'bg-gradient-to-br from-neutral-400 to-neutral-600'
})

const deviceCount = computed(() => props.devices.length)

const ledDevices = computed(() => props.devices.filter(d => ['LIGHT', 'LED'].includes(d.type)))
const sensorDevices = computed(() =>
  props.devices.filter(d => ['SENSOR', 'MULTI_SENSOR'].includes(d.type))
)

const ledCount = computed(() => ledDevices.value.length)
const sensorCount = computed(() => sensorDevices.value.length)

const isDeviceOnline = device => {
  const lastSeen = device?.deviceState?.lastSeen
  if (!lastSeen) {
    return false
  }
  const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000)
  return new Date(lastSeen) > fiveMinutesAgo
}

const onlineLedCount = computed(() => ledDevices.value.filter(isDeviceOnline).length)
const onlineSensorCount = computed(() => sensorDevices.value.filter(isDeviceOnline).length)
</script>

<style scoped>
.room-card {
  @apply border-2 border-transparent;
}
</style>
