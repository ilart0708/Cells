<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: Number, required: true },
  max: { type: Number, required: true },
  summary: { type: String, required: true }, // read by screen readers, e.g. "2 of 5 members shared"
})

const percent = computed(() =>
  props.max > 0 ? Math.min(100, Math.round((props.value / props.max) * 100)) : 0,
)
</script>

<template>
  <div>
    <div class="flex items-baseline justify-between gap-3 text-sm">
      <span class="font-semibold text-navy-900">{{ label }}</span>
      <span class="text-muted">{{ value }} of {{ max }}</span>
    </div>
    <div
      class="mt-2 h-2.5 overflow-hidden rounded-md bg-track"
      role="progressbar"
      :aria-valuenow="value"
      aria-valuemin="0"
      :aria-valuemax="max"
      :aria-label="summary"
    >
      <div class="h-full rounded-md bg-gold-500" :style="{ width: percent + '%' }"></div>
    </div>
  </div>
</template>
