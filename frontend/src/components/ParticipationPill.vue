<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }, // SUBMITTED | ELIGIBLE | WAITING | EXEMPTED
  you: { type: Boolean, default: false },
})

// The words carry the meaning; the tint only reinforces it.
const label = computed(() => {
  switch (props.status) {
    case 'SUBMITTED':
      return 'Submitted'
    case 'ELIGIBLE':
      return props.you ? 'Your turn' : 'Their turn'
    case 'WAITING':
      return 'Waiting'
    case 'EXEMPTED':
      return 'Exempted'
    default:
      return props.status
  }
})

const TONES = {
  SUBMITTED: 'bg-gold-100 text-navy-900',
  ELIGIBLE: 'bg-navy-900 text-white',
  WAITING: 'bg-track text-navy-900',
  EXEMPTED: 'border border-line-input bg-white text-muted',
}
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-bold', TONES[status] ?? TONES.WAITING]">
    {{ label }}
  </span>
</template>
