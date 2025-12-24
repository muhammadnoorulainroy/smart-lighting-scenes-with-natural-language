<template>
  <transition
    enter-active-class="transition ease-out duration-200"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition ease-in duration-150"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="show"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      @click.self="$emit('close')"
    >
      <div class="card p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <h2 class="text-xl font-semibold mb-6">{{ schedule?.id ? 'Edit Schedule' : 'Create Schedule' }}</h2>

        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div>
            <label class="block text-sm font-medium mb-1">Schedule Name</label>
            <input v-model="formData.name" type="text" class="input w-full" placeholder="e.g., Morning Lights" required />
          </div>

          <div>
            <label class="block text-sm font-medium mb-1">Description</label>
            <input v-model="formData.description" type="text" class="input w-full" placeholder="Brief description" />
          </div>

          <div>
            <label class="block text-sm font-medium mb-1">Trigger Type</label>
            <select v-model="formData.triggerType" class="input w-full">
              <option value="time">Time-based</option>
              <option value="sun">Sun event (sunrise/sunset)</option>
            </select>
          </div>

          <div v-if="formData.triggerType === 'time'">
            <label class="block text-sm font-medium mb-1">Time</label>
            <input v-model="formData.time" type="time" class="input w-full" required />
          </div>

          <div v-if="formData.triggerType === 'sun'" class="space-y-3">
            <div>
              <label class="block text-sm font-medium mb-1">Sun Event</label>
              <select v-model="formData.sunEvent" class="input w-full">
                <option value="sunrise">Sunrise</option>
                <option value="sunset">Sunset</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Offset (minutes)</label>
              <input v-model.number="formData.offsetMinutes" type="number" class="input w-full" placeholder="e.g., -30 for 30 min before" />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-2">Days</label>
            <div class="flex flex-wrap gap-2">
              <button v-for="day in days" :key="day.value" type="button" class="px-3 py-1 rounded-full text-sm border-2 transition-colors" :class="formData.selectedDays.includes(day.value) ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30' : 'border-neutral-200 dark:border-neutral-700'" @click="toggleDay(day.value)">
                {{ day.label }}
              </button>
            </div>
          </div>

          <hr class="border-neutral-200 dark:border-neutral-700" />

          <h3 class="font-medium">Action</h3>

          <div>
            <label class="block text-sm font-medium mb-1">Action Type</label>
            <select v-model="formData.actionType" class="input w-full">
              <option value="light.on">Turn On</option>
              <option value="light.off">Turn Off</option>
              <option value="light.brightness">Set Brightness</option>
              <option value="scene.apply">Apply Scene</option>
            </select>
          </div>

          <div v-if="formData.actionType !== 'scene.apply'">
            <label class="block text-sm font-medium mb-1">Target</label>
            <select v-model="formData.target" class="input w-full">
              <option value="all">All Lights</option>
              <option value="bedroom">Bedroom</option>
              <option value="living_room">Living Room</option>
              <option value="kitchen">Kitchen</option>
              <option value="bathroom">Bathroom</option>
              <option value="hallway">Hallway</option>
            </select>
          </div>

          <div v-if="formData.actionType === 'light.brightness'">
            <label class="block text-sm font-medium mb-1">Brightness: {{ formData.brightness }}%</label>
            <input v-model.number="formData.brightness" type="range" min="0" max="100" class="w-full" />
          </div>

          <div v-if="formData.actionType === 'scene.apply'">
            <label class="block text-sm font-medium mb-1">Scene</label>
            <select v-model="formData.sceneId" class="input w-full">
              <option v-for="scene in availableScenes" :key="scene.id" :value="scene.id">{{ scene.name }}</option>
            </select>
          </div>

          <div class="flex gap-3 pt-4">
            <button type="submit" class="btn btn-primary flex-1">{{ schedule?.id ? 'Update' : 'Create' }}</button>
            <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { scenesApi } from '../../api/scenes'

const props = defineProps({ show: { type: Boolean, required: true }, schedule: { type: Object, default: null } })
const emit = defineEmits(['close', 'save'])

const days = [
  { value: 'mon', label: 'Mon' }, { value: 'tue', label: 'Tue' }, { value: 'wed', label: 'Wed' },
  { value: 'thu', label: 'Thu' }, { value: 'fri', label: 'Fri' }, { value: 'sat', label: 'Sat' }, { value: 'sun', label: 'Sun' }
]

const availableScenes = ref([])

const formData = ref({
  name: '', description: '', triggerType: 'time', time: '07:00', sunEvent: 'sunset', offsetMinutes: 0,
  selectedDays: ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'],
  actionType: 'light.on', target: 'all', brightness: 50, sceneId: null
})

const toggleDay = day => {
  const idx = formData.value.selectedDays.indexOf(day)
  if (idx >= 0) {formData.value.selectedDays.splice(idx, 1)}
  else {formData.value.selectedDays.push(day)}
}

watch(() => [props.show, props.schedule], () => {
  if (props.show) {
    if (props.schedule) {
      const s = props.schedule
      const tc = s.triggerConfig || {}
      const action = (s.actions || [])[0] || {}
      formData.value = {
        name: s.name || '', description: s.description || '',
        triggerType: s.triggerType || 'time',
        time: tc.at?.substring(0, 5) || '07:00',
        sunEvent: tc.event || 'sunset',
        offsetMinutes: tc.offset_minutes || 0,
        selectedDays: tc.weekdays || ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'],
        actionType: action.intent || action.type === 'scene' ? 'scene.apply' : 'light.on',
        target: action.target || 'all',
        brightness: action.params?.brightness || 50,
        sceneId: action.scene_id || action.scene || null
      }
    } else {
      formData.value = {
        name: '', description: '', triggerType: 'time', time: '07:00', sunEvent: 'sunset', offsetMinutes: 0,
        selectedDays: ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'],
        actionType: 'light.on', target: 'all', brightness: 50, sceneId: null
      }
    }
  }
}, { immediate: true })

const handleSubmit = () => {
  const triggerConfig = formData.value.triggerType === 'time'
    ? { at: `${formData.value.time  }:00`, weekdays: formData.value.selectedDays }
    : { event: formData.value.sunEvent, offset_minutes: formData.value.offsetMinutes }

  let action = { type: 'light', intent: formData.value.actionType, target: formData.value.target }
  if (formData.value.actionType === 'light.brightness') {action.params = { brightness: formData.value.brightness }}
  if (formData.value.actionType === 'scene.apply') {action = { type: 'scene', scene_id: formData.value.sceneId }}

  emit('save', {
    name: formData.value.name, description: formData.value.description, enabled: true,
    triggerType: formData.value.triggerType, triggerConfig, actions: [action]
  })
}

onMounted(async () => {
  try { availableScenes.value = await scenesApi.getAll() }
  catch (err) { console.error('Failed to load scenes:', err) }
})
</script>
