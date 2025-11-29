<template>
  <div class="feature-card group">
    <div class="feature-icon" :class="iconBgClass">
      <component :is="icon" class="w-8 h-8 text-white" />
    </div>
    <h3 class="text-xl font-semibold mt-6 mb-3 transition-colors" :class="hoverClass">
      {{ title }}
    </h3>
    <p class="text-neutral-600 dark:text-neutral-400">
      {{ description }}
    </p>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: Object, required: true },
  title: { type: String, required: true },
  description: { type: String, required: true },
  iconBgClass: { type: String, required: true },
  hoverClass: { type: String, required: true }
})
</script>

<style scoped>
.feature-card {
  @apply card p-8 hover:shadow-xl transition-all duration-300 hover:-translate-y-2 border-2 border-transparent hover:border-primary-200 dark:hover:border-primary-800;
}
.feature-icon {
  @apply w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg transform group-hover:scale-110 transition-transform duration-300;
}
</style>
