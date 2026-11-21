<script setup>
defineProps({ status: { type: String, required: true } }) // SHARED | MISSED | BLOCKED | EXEMPT | PENDING

// Gentle words: this history is here to encourage, never to shame. The words carry the meaning.
const LABELS = {
  SHARED: 'Shared',
  MISSED: 'Didn’t share',
  BLOCKED: 'Waited on others',
  EXEMPT: 'Exempt',
  PENDING: 'Open today',
}

const TONES = {
  SHARED: 'bg-gold-100 text-navy-900',
  MISSED: 'bg-track text-navy-900',
  BLOCKED: 'border border-line-input bg-white text-muted',
  EXEMPT: 'border border-line-input bg-white text-muted',
  PENDING: 'bg-navy-900 text-white',
}
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-bold', TONES[status] ?? TONES.EXEMPT]">
    {{ LABELS[status] ?? status }}
  </span>
</template>
