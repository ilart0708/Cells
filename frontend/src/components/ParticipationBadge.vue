<script setup>
// The participation number: a navy circle with a gold number, as in the design guide.
defineProps({ number: { type: Number, required: true } })
</script>

<template>
  <span
    class="flex size-8 shrink-0 items-center justify-center rounded-full bg-navy-900 text-sm font-bold text-gold-500"
  >
    <span class="sr-only">Number </span>{{ number }}
  </span>
</template>
