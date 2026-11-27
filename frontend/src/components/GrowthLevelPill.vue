<script setup>
// A member's level (Seed, Sprout, ...). Different from LevelPill, which is a passage's learning level.
import LevelBadge from './LevelBadge.vue'

defineProps({ level: { type: Object, required: true } }) // { code, name, threshold }
</script>

<template>
  <span class="inline-flex items-center gap-1.5 rounded-full bg-track py-1 pl-1 pr-3 text-xs font-bold text-navy-900">
    <LevelBadge :code="level.code" :size="20" />
    {{ level.name }}
  </span>
</template>
