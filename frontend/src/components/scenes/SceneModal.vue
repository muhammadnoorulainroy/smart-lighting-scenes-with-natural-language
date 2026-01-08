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
        <h2 class="text-xl font-semibold mb-6">
          {{ scene?.id ? 'Edit Scene' : 'Create Scene' }}
        </h2>

        <form class="space-y-4" @submit.prevent="handleSubmit">
          <!-- Name -->
          <div>
            <label class="block text-sm font-medium mb-1">Scene Name</label>
            <input
              v-model="formData.name"
              type="text"
              class="input w-full"
              placeholder="e.g., Cozy Evening"
              required
            />
          </div>

          <!-- Icon -->
          <div>
            <label class="block text-sm font-medium mb-1">Icon</label>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="icon in icons"
                :key="icon"
                type="button"
                class="w-10 h-10 text-xl rounded-lg border-2 transition-colors"
                :class="
                  formData.icon === icon
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30'
                    : 'border-neutral-200 dark:border-neutral-700 hover:border-primary-300'
                "
                @click="formData.icon = icon"
              >
                {{ icon }}
              </button>
            </div>
          </div>

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium mb-1">Description</label>
            <input
              v-model="formData.description"
              type="text"
              class="input w-full"
              placeholder="Brief description of this scene"
            />
          </div>

          <!-- Target -->
          <div>
            <label class="block text-sm font-medium mb-1">Apply To</label>
            <select v-model="formData.target" class="input w-full">
              <option value="all">All Lights</option>
              <option value="bedroom">Bedroom</option>
              <option value="living_room">Living Room</option>
              <option value="kitchen">Kitchen</option>
              <option value="bathroom">Bathroom</option>
              <option value="hallway">Hallway</option>
            </select>
          </div>

          <!-- Brightness -->
          <div>
            <label class="block text-sm font-medium mb-1">
              Brightness: {{ formData.brightness }}%
            </label>
            <input
              v-model.number="formData.brightness"
              type="range"
              min="0"
              max="100"
              class="w-full accent-yellow-400"
            />
          </div>

          <!-- Color -->
          <div>
            <label class="block text-sm font-medium mb-1">Color</label>
            <div class="flex gap-4 items-center">
              <input
                v-model="formData.color"
                type="color"
                class="w-16 h-10 rounded cursor-pointer"
              />
              <span class="font-mono text-sm">{{ formData.color }}</span>
            </div>
          </div>

          <!-- Color Temperature -->
          <div>
            <label class="block text-sm font-medium mb-1">
              Color Temperature: {{ formData.colorTemp }}K
            </label>
            <input
              v-model.number="formData.colorTemp"
              type="range"
              min="2700"
              max="6500"
              step="100"
              class="w-full"
              style="background: linear-gradient(to right, #ffb347, #fffaf0, #87ceeb)"
            />
            <div class="flex justify-between text-xs text-neutral-500 mt-1">
              <span>Warm</span>
              <span>Neutral</span>
              <span>Cool</span>
            </div>
          </div>

          <!-- Preview -->
          <div class="p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
            <div class="text-sm font-medium mb-2">Preview</div>
            <div class="h-12 rounded-lg transition-all" :style="previewStyle" />
          </div>

          <!-- Actions -->
          <div class="flex gap-3 pt-4">
            <button type="submit" class="btn btn-primary flex-1">
              {{ scene?.id ? 'Update Scene' : 'Create Scene' }}
            </button>
            <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, required: true },
  scene: { type: Object, default: null }
})

const emit = defineEmits(['close', 'save'])

const icons = ['💡', '🎬', '☀️', '🧘', '🎉', '💼', '🌙', '🌅', '🎨', '🔥', '❄️', '🌿']

const formData = ref({
  name: '',
  description: '',
  icon: '💡',
  target: 'all',
  brightness: 50,
  color: '#FFFFFF',
  colorTemp: 4000
})

// Initialize form when scene prop changes
watch(
  () => [props.show, props.scene],
  () => {
    if (props.show) {
      if (props.scene) {
        formData.value = {
          name: props.scene.name || '',
          description: props.scene.description || '',
          icon: props.scene.icon || '💡',
          target: props.scene.settings?.target || 'all',
          brightness: props.scene.settings?.brightness || 50,
          color: rgbToHex(props.scene.settings?.rgb) || '#FFFFFF',
          colorTemp: props.scene.settings?.color_temp || 4000
        }
      } else {
        // Reset form for new scene
        formData.value = {
          name: '',
          description: '',
          icon: '💡',
          target: 'all',
          brightness: 50,
          color: '#FFFFFF',
          colorTemp: 4000
        }
      }
    }
  },
  { immediate: true }
)

const previewStyle = computed(() => {
  const brightness = formData.value.brightness / 100
  const { color } = formData.value
  // Apply brightness to color
  const r = parseInt(color.slice(1, 3), 16)
  const g = parseInt(color.slice(3, 5), 16)
  const b = parseInt(color.slice(5, 7), 16)
  return {
    backgroundColor: `rgb(${Math.round(r * brightness)}, ${Math.round(g * brightness)}, ${Math.round(b * brightness)})`
  }
})

const rgbToHex = rgb => {
  if (!rgb || !Array.isArray(rgb) || rgb.length < 3) {
    return '#FFFFFF'
  }
  const [r, g, b] = rgb
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`.toUpperCase()
}

const hexToRgb = hex => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return [r, g, b]
}

const handleSubmit = () => {
  const sceneData = {
    name: formData.value.name,
    description: formData.value.description,
    icon: formData.value.icon,
    settings: {
      target: formData.value.target,
      brightness: formData.value.brightness,
      rgb: hexToRgb(formData.value.color),
      color_temp: formData.value.colorTemp
    }
  }

  emit('save', sceneData)
}
</script>
