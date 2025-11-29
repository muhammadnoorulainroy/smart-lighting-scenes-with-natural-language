<template>
  <div class="rooms">
    <h1 class="text-3xl font-display font-semibold mb-8">Rooms</h1>
    <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div class="room-tile">
        <h3 class="text-xl font-semibold mb-4">Living Room</h3>
        <p class="text-neutral-600 dark:text-neutral-400">Room controls coming soon</p>
      </div>
      <div class="room-tile">
        <h3 class="text-xl font-semibold mb-4">Bedroom</h3>
        <p class="text-neutral-600 dark:text-neutral-400">Room controls coming soon</p>
      </div>
      <div class="room-tile">
        <h3 class="text-xl font-semibold mb-4">Kitchen</h3>
        <p class="text-neutral-600 dark:text-neutral-400">Room controls coming soon</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// Rooms logic will go here
</script>

<style scoped>
.room-tile {
  @apply card p-6 hover:shadow-lg transition-shadow;
}
</style>
