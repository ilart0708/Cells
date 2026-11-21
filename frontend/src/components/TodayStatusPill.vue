<script setup>
// A member's day in one room, as the administrator sees it. The words carry the meaning.
defineProps({ status: { type: String, required: true } }) // SHARED | EXEMPT | STILL_TO_SHARE | NO_DEVOTION

const LABELS = {
  SHARED: 'Shared',
  EXEMPT: 'Exempt',
  STILL_TO_SHARE: 'Still to share',
  NO_DEVOTION: 'No devotion today',
}

const TONES = {
  SHARED: 'bg-gold-100 text-navy-900',
  EXEMPT: 'border border-line-input bg-white text-muted',
  STILL_TO_SHARE: 'bg-track text-navy-900',
  NO_DEVOTION: 'border border-line-input bg-white text-muted',
}
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-bold', TONES[status] ?? TONES.EXEMPT]">
    {{ LABELS[status] ?? status }}
  </span>
</template>
