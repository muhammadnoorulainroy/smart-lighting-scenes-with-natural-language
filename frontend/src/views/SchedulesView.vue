<template>
  <div class="schedules-page container mx-auto px-4 py-8">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h1 class="text-3xl font-display font-semibold">Lighting Schedules</h1>
        <p class="text-neutral-600 dark:text-neutral-400 mt-1">
          Automate your lights with time-based schedules
        </p>
      </div>
      <button v-if="canEdit" class="btn btn-primary" @click="showCreateModal = true">
        Create Schedule
      </button>
    </div>

    <div v-if="canEdit" class="card p-6 mb-8">
      <h3 class="font-medium mb-3">Quick Schedule via Natural Language</h3>
      <div class="flex items-center gap-4">
        <div class="flex-1 relative">
          <input
            v-model="nlpCommand"
            type="text"
            class="input w-full pr-12"
            placeholder="Try: 'Turn off all lights at 11pm every day'"
            @keyup.enter="processNlpCommand"
          />
          <button
            class="absolute right-2 top-1/2 -translate-y-1/2 p-2 text-neutral-500 hover:text-primary-500"
            :class="{ 'text-red-500 animate-pulse': isListening }"
            @click="toggleVoiceInput"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"
              />
            </svg>
          </button>
        </div>
        <button
          class="btn btn-accent"
          :disabled="!nlpCommand.trim() || nlpProcessing"
          @click="processNlpCommand"
        >
          <span v-if="nlpProcessing" class="flex items-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
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
            Processing...
          </span>
          <span v-else>Create</span>
        </button>
      </div>
      <div
        v-if="nlpResult"
        class="mt-4 p-4 rounded-lg"
        :class="
          nlpResult.valid ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'
        "
      >
        <div v-if="nlpResult.valid" class="space-y-3">
          <p class="font-medium">{{ nlpResult.preview }}</p>

          <!-- Conflict Warning -->
          <div
            v-if="nlpResult.conflictAnalysis?.hasConflicts"
            class="mt-4 p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-300 dark:border-amber-700 rounded-lg"
          >
            <div class="flex items-center gap-2 mb-3">
              <svg
                class="w-5 h-5 text-amber-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
              <span class="font-semibold text-amber-700 dark:text-amber-300"
                >Schedule Conflict Detected</span
              >
            </div>

            <p class="text-sm text-amber-700 dark:text-amber-300 mb-4">
              {{ nlpResult.conflictAnalysis.summary }}
            </p>

            <div
              v-for="(conflict, idx) in nlpResult.conflictAnalysis.conflicts"
              :key="idx"
              class="mb-4 p-3 bg-white dark:bg-neutral-800 rounded border border-amber-200 dark:border-amber-800"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium">
                  Conflicts with: <strong>{{ conflict.scheduleName2 }}</strong>
                </span>
                <span
                  class="text-xs px-2 py-1 rounded"
                  :class="{
                    'bg-red-100 text-red-700': conflict.severity === 'high',
                    'bg-amber-100 text-amber-700': conflict.severity === 'medium',
                    'bg-blue-100 text-blue-700': conflict.severity === 'low'
                  }"
                >
                  {{ conflict.severity }}
                </span>
              </div>
              <p class="text-sm text-neutral-600 dark:text-neutral-400 mb-3">
                {{ conflict.description }}
              </p>

              <div class="space-y-2">
                <p class="text-xs font-medium text-neutral-500 uppercase">Choose a Resolution:</p>
                <div
                  v-for="resolution in conflict.resolutions"
                  :key="resolution.id"
                  class="flex items-center gap-2"
                >
                  <input
                    :id="`res-${idx}-${resolution.id}`"
                    type="radio"
                    :name="`conflict-${idx}`"
                    :value="resolution.id"
                    :checked="selectedResolutions[idx] === resolution.id"
                    class="text-primary-600"
                    @change="selectedResolutions[idx] = resolution.id; selectedResolutionParams[idx] = resolution.changes"
                  />
                  <label :for="`res-${idx}-${resolution.id}`" class="text-sm cursor-pointer">
                    {{ resolution.description }}
                  </label>
                </div>
              </div>
            </div>

            <div class="flex gap-3 mt-4">
              <button
                class="btn btn-accent"
                :disabled="!allConflictsResolved || nlpProcessing"
                @click="applyResolutionsAndCreate"
              >
                <span v-if="nlpProcessing" class="flex items-center gap-2">
                  <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
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
                  Applying...
                </span>
                <span v-else>Apply Resolutions & Create</span>
              </button>
              <button class="btn btn-primary" :disabled="nlpProcessing" @click="confirmNlpCommand">
                <span v-if="nlpProcessing" class="flex items-center gap-2">
                  <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
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
                  Creating...
                </span>
                <span v-else>Create Anyway</span>
              </button>
              <button class="btn btn-secondary" :disabled="nlpProcessing" @click="nlpResult = null">
                Cancel
              </button>
            </div>
          </div>

          <!-- No conflicts - normal flow -->
          <div v-else class="flex gap-3">
            <button class="btn btn-primary" :disabled="nlpProcessing" @click="confirmNlpCommand">
              <span v-if="nlpProcessing" class="flex items-center gap-2">
                <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
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
                Creating...
              </span>
              <span v-else>Create Schedule</span>
            </button>
            <button class="btn btn-secondary" :disabled="nlpProcessing" @click="nlpResult = null">
              Cancel
            </button>
          </div>
        </div>
        <p v-else class="text-red-700 dark:text-red-300">{{ nlpResult.error }}</p>
      </div>
    </div>

    <div v-if="loading" class="text-center py-8">
      <div
        class="animate-spin w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full mx-auto"
      />
    </div>

    <div v-else-if="schedules.length === 0" class="card p-12 text-center">
      <h3 class="text-lg font-medium mb-2">No Schedules Yet</h3>
      <p class="text-neutral-500 mb-4">
        {{
          canEdit
            ? 'Create schedules to automate your lights.'
            : 'No schedules have been created yet.'
        }}
      </p>
      <button v-if="canEdit" class="btn btn-primary" @click="showCreateModal = true">
        Create First Schedule
      </button>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div
        v-for="schedule in schedules"
        :key="schedule.id"
        class="card p-5"
        :class="{ 'opacity-50': !schedule.enabled }"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-3">
              <span class="text-2xl">{{ getTriggerIcon(schedule.triggerType) }}</span>
              <div>
                <h3 class="font-semibold">{{ schedule.name }}</h3>
                <p class="text-sm text-neutral-500">{{ schedule.description }}</p>
              </div>
            </div>
            <div class="mt-3 text-sm text-neutral-600 dark:text-neutral-400">
              <div class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <span>{{ formatTrigger(schedule) }}</span>
              </div>
              <div class="flex items-center gap-2 mt-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
                  />
                </svg>
                <span>{{ formatActions(schedule) }}</span>
              </div>
            </div>
            <div class="mt-3 text-xs text-neutral-400">
              Triggered {{ schedule.triggerCount || 0 }} times
              <span v-if="schedule.lastTriggeredAt">
                | Last: {{ formatDate(schedule.lastTriggeredAt) }}</span
              >
            </div>
          </div>
          <div v-if="canEdit" class="flex flex-col gap-2 items-end">
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                :checked="schedule.enabled"
                class="sr-only peer"
                @change="toggleSchedule(schedule)"
              />
              <div
                class="w-11 h-6 bg-neutral-200 rounded-full peer dark:bg-neutral-700 peer-checked:after:translate-x-full after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"
              />
            </label>
            <div class="flex gap-1">
              <button
                class="p-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded"
                @click="editSchedule(schedule)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                  />
                </svg>
              </button>
              <button
                class="p-2 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-red-500"
                @click="deleteSchedule(schedule)"
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
          <!-- Show enabled status for view-only users -->
          <div v-else class="flex items-center">
            <span
              class="text-sm px-2 py-1 rounded-full"
              :class="
                schedule.enabled
                  ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                  : 'bg-neutral-100 text-neutral-500 dark:bg-neutral-800'
              "
            >
              {{ schedule.enabled ? 'Active' : 'Inactive' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <ScheduleModal
      :show="showCreateModal || showEditModal"
      :schedule="editingSchedule"
      @close="closeModal"
      @save="saveSchedule"
    />

    <ConfirmModal
      :show="showDeleteModal"
      title="Delete Schedule"
      :message="`Are you sure you want to delete '${scheduleToDelete?.name}'? This action cannot be undone.`"
      confirm-text="Delete"
      type="danger"
      :loading="deleting"
      @confirm="confirmDeleteSchedule"
      @cancel="cancelDeleteSchedule"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { schedulesApi } from '../api/schedules'
import { nlpApi } from '../api/nlp'
import { scenesApi } from '../api/scenes'
import { useWebSocket } from '../stores/websocket'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../stores/toast'
import ScheduleModal from '../components/schedules/ScheduleModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

const {
  lastScheduleTriggered,
  lastScheduleChange,
  clearLastScheduleTriggered,
  clearLastScheduleChange,
  connect: connectWs
} = useWebSocket()

const authStore = useAuthStore()
const toast = useToast()

// Role-based access - GUEST can only view schedules
const canEdit = computed(() => authStore.isResident)

const schedules = ref([])
const scenes = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingSchedule = ref(null)
const nlpCommand = ref('')
const nlpProcessing = ref(false)
const nlpResult = ref(null)
const isListening = ref(false)
const selectedResolutions = ref({})
const selectedResolutionParams = ref({})
let recognition = null

// Delete modal state
const showDeleteModal = ref(false)
const scheduleToDelete = ref(null)
const deleting = ref(false)

// Check if all conflicts have resolutions selected
const allConflictsResolved = computed(() => {
  if (!nlpResult.value?.conflictAnalysis?.conflicts) {
    return true
  }
  const { conflicts } = nlpResult.value.conflictAnalysis
  return conflicts.every((_, idx) => selectedResolutions.value[idx])
})

const loadSchedules = async () => {
  loading.value = true
  try {
    const [schedulesData, scenesData] = await Promise.all([
      schedulesApi.getAll(),
      scenesApi.getAll()
    ])
    schedules.value = schedulesData
    scenes.value = scenesData
  } catch (err) {
    console.error('Failed to load schedules:', err)
  } finally {
    loading.value = false
  }
}

const toggleSchedule = async schedule => {
  try {
    await schedulesApi.toggle(schedule.id)
    schedule.enabled = !schedule.enabled
  } catch (err) {
    console.error('Failed to toggle schedule:', err)
  }
}

const editSchedule = schedule => {
  editingSchedule.value = { ...schedule }
  showEditModal.value = true
}

const deleteSchedule = schedule => {
  scheduleToDelete.value = schedule
  showDeleteModal.value = true
}

const cancelDeleteSchedule = () => {
  showDeleteModal.value = false
  scheduleToDelete.value = null
}

const confirmDeleteSchedule = async () => {
  if (!scheduleToDelete.value) {
    return
  }

  deleting.value = true
  try {
    await schedulesApi.delete(scheduleToDelete.value.id)
    await loadSchedules()
    showDeleteModal.value = false
    scheduleToDelete.value = null
    toast.success('Schedule deleted successfully')
  } catch (err) {
    console.error('Failed to delete schedule:', err)
    toast.error('Failed to delete schedule')
  } finally {
    deleting.value = false
  }
}

const saveSchedule = async data => {
  try {
    if (editingSchedule.value?.id) {
      await schedulesApi.update(editingSchedule.value.id, data)
      toast.success('Schedule updated successfully')
    } else {
      await schedulesApi.create(data)
      toast.success('Schedule created successfully')
    }
    await loadSchedules()
    closeModal()
  } catch (err) {
    console.error('Failed to save:', err)
    toast.error('Failed to save schedule')
  }
}

const closeModal = () => {
  showCreateModal.value = false
  showEditModal.value = false
  editingSchedule.value = null
}

const processNlpCommand = async () => {
  if (!nlpCommand.value.trim()) {
    return
  }
  nlpProcessing.value = true
  nlpResult.value = null
  selectedResolutions.value = {}
  selectedResolutionParams.value = {}

  try {
    nlpResult.value = await nlpApi.parse(nlpCommand.value)
    if (nlpResult.value.valid && !nlpResult.value.isScheduled) {
      nlpResult.value = {
        valid: false,
        error: 'This looks like an immediate command. Add a time like "at 7pm".'
      }
    }
  } catch (err) {
    nlpResult.value = { valid: false, error: 'Failed to process' }
  } finally {
    nlpProcessing.value = false
  }
}

const confirmNlpCommand = async () => {
  if (!nlpResult.value?.valid) {
    return
  }
  nlpProcessing.value = true
  try {
    const result = await nlpApi.confirm(nlpResult.value)
    if (result.executed) {
      toast.success(result.result || 'Schedule created successfully')
      nlpCommand.value = ''
      nlpResult.value = null
      selectedResolutions.value = {}
      selectedResolutionParams.value = {}
      await loadSchedules()
    }
  } catch (err) {
    console.error('Execute error:', err)
    toast.error('Failed to create schedule')
  } finally {
    nlpProcessing.value = false
  }
}

const applyResolutionsAndCreate = async () => {
  if (!nlpResult.value?.valid || !allConflictsResolved.value) {
    return
  }

  nlpProcessing.value = true
  try {
    // Apply all selected resolutions
    const { conflicts } = nlpResult.value.conflictAnalysis
    for (let idx = 0; idx < conflicts.length; idx++) {
      const conflict = conflicts[idx]
      const resolutionId = selectedResolutions.value[idx]
      const params = selectedResolutionParams.value[idx] || {}

      if (resolutionId) {
        // For 'adjust_new' resolution, modify the new schedule's time before creating
        if (resolutionId === 'adjust_new' && params.new_time) {
          // Update the parsed command's schedule time
          if (nlpResult.value.parsed?.schedule) {
            nlpResult.value.parsed.schedule.time = params.new_time
          }
        } else if (resolutionId === 'ai_suggested') {
          const resolution = conflict.resolutions.find(r => r.id === 'ai_suggested')
          if (resolution?.description) {
            const timeMatch = resolution.description.match(/(\d{1,2}):(\d{2})/)
            if (timeMatch && nlpResult.value.parsed?.schedule) {
              const [, hours, minutes] = timeMatch
              nlpResult.value.parsed.schedule.time = `${hours.padStart(2, '0')}:${minutes}`
            }
          }
        } else {
          await nlpApi.resolveConflict(conflict.scheduleId2, resolutionId, params)
        }
      }
    }

    // Now create the new schedule
    const result = await nlpApi.confirm(nlpResult.value)
    if (result.executed) {
      toast.success(result.result || 'Schedule created with resolutions applied')
      nlpCommand.value = ''
      nlpResult.value = null
      selectedResolutions.value = {}
      selectedResolutionParams.value = {}
      await loadSchedules()
    }
  } catch (err) {
    console.error('Error applying resolutions:', err)
    toast.error(`Failed to apply resolutions: ${err.message || 'Unknown error'}`)
  } finally {
    nlpProcessing.value = false
  }
}

const toggleVoiceInput = () => {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    toast.warning('Voice input is not supported in your browser')
    return
  }
  if (isListening.value) {
    recognition?.stop()
    isListening.value = false
    return
  }
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SR()
  recognition.continuous = false
  recognition.interimResults = false
  recognition.lang = 'en-US'
  recognition.onstart = () => {
    isListening.value = true
  }
  recognition.onresult = e => {
    nlpCommand.value = e.results[0][0].transcript
    isListening.value = false
    processNlpCommand()
  }
  recognition.onerror = () => {
    isListening.value = false
  }
  recognition.onend = () => {
    isListening.value = false
  }
  recognition.start()
}

const getTriggerIcon = type => ({ time: '⏰', sun: '☀️', sensor: '📡' })[type] || '⏰'

const formatTrigger = schedule => {
  const c = schedule.triggerConfig
  if (schedule.triggerType === 'time') {
    const time = c.at?.substring(0, 5) || c.time
    const days = c.weekdays
    if (!days || days.length === 7) {
      return `Every day at ${time}`
    }
    if (days.length === 5 && !days.includes('sat') && !days.includes('sun')) {
      return `Weekdays at ${time}`
    }
    if (days.length === 2 && days.includes('sat') && days.includes('sun')) {
      return `Weekends at ${time}`
    }
    return `${days.join(', ')} at ${time}`
  }
  if (schedule.triggerType === 'sun') {
    const off = c.offset_minutes
    return off ? `${Math.abs(off)} min ${off > 0 ? 'after' : 'before'} ${c.event}` : `At ${c.event}`
  }
  return 'Unknown'
}

const formatActions = schedule => {
  const actions = schedule.actions || []
  if (!actions.length) {
    return 'No actions'
  }
  const a = actions[0]
  if (a.type === 'scene') {
    const sceneId = a.scene_id || a.scene
    const scene = scenes.value.find(s => s.id === sceneId || s.name === sceneId)
    const sceneName = scene?.name || sceneId
    return `Apply scene: ${sceneName}`
  }
  if (a.intent === 'light.off') {
    return `Turn off ${a.target || 'all'} lights`
  }
  if (a.intent === 'light.on') {
    return `Turn on ${a.target || 'all'} lights`
  }
  if (a.intent === 'light.brightness') {
    return `Set ${a.target || 'all'} to ${a.params?.brightness}%`
  }
  return a.intent || 'Unknown'
}

const formatDate = d => (d ? new Date(d).toLocaleString() : '')

watch(lastScheduleTriggered, event => {
  if (event) {
    toast.success(`Schedule "${event.scheduleName}" triggered! (${event.triggerCount} times total)`)
    const schedule = schedules.value.find(s => s.id === event.scheduleId)
    if (schedule) {
      schedule.triggerCount = event.triggerCount
      schedule.lastTriggeredAt = new Date(event.triggeredAt).toISOString()
    }
    clearLastScheduleTriggered()
  }
})

// Watch for schedule changes (create/update/delete/toggle) - refresh list
watch(lastScheduleChange, event => {
  if (event) {
    loadSchedules()
    clearLastScheduleChange()
  }
})

onMounted(() => {
  connectWs()
  loadSchedules()
})
</script>
