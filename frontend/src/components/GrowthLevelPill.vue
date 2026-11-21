<script setup>
// A member's level (Seed, Sprout, ...). Different from LevelPill, which is a passage's learning level.
defineProps({ level: { type: Object, required: true } }) // { code, name, threshold }
</script>

<template>
  <span class="inline-flex items-center rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
    {{ level.name }}
  </span>
</template>
