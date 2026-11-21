<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  hint: { type: String, default: '' },
})
</script>

<template>
  <div class="rounded-[18px] border border-line bg-white p-4">
    <p class="text-xs font-bold uppercase tracking-[2px] text-muted">{{ label }}</p>
    <p class="mt-1 text-3xl font-extrabold text-navy-900">{{ value }}</p>
    <p v-if="hint" class="mt-1 text-sm text-muted">{{ hint }}</p>
  </div>
</template>
