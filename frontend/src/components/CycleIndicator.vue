<script setup>
import { useCycleCountdown } from '../composables/useCycleCountdown'

const { zone, label, visible } = useCycleCountdown()
</script>

<template>
  <p
    v-if="visible"
    class="flex flex-wrap items-center justify-end gap-x-2 text-sm text-muted"
    :aria-label="`The daily cycle resets at 12:00 AM ${zone}, in ${label}`"
  >
    <span class="text-xs font-bold uppercase tracking-wider text-gold-700">New day in {{ label }}</span>
    <span class="hidden sm:inline">Resets at 12:00 AM ({{ zone }})</span>
  </p>
</template>
