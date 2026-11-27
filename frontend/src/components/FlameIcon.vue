<script setup>
// The streak flame: devotions shared in a row.
defineProps({ size: { type: Number, default: 18 } })
</script>

<template>
  <svg viewBox="0 0 32 32" :width="size" :height="size" aria-hidden="true" class="shrink-0 text-gold-700" fill="currentColor">
    <path d="M16 3 C17 9 23 11 23 19 A7 7 0 0 1 9 19 C9 15.500 11 14 12.500 11.500 C13 14.500 15 15 15.500 13 C16 10 14 7 16 3 Z" />
  </svg>
</template>
