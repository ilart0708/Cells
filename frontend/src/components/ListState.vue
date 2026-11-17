<script setup>
import { btnOutline } from './buttonClasses'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  count: { type: Number, required: true },
  emptyMessage: { type: String, required: true },
})

defineEmits(['retry'])
</script>

<template>
  <p v-if="loading && count === 0 && !error" role="status" class="p-6 text-center text-sm text-muted">
    Loading…
  </p>
  <div v-else-if="error" role="alert" class="space-y-3 p-6 text-center">
    <p class="text-sm font-semibold text-red-700">{{ error }}</p>
    <button type="button" :class="btnOutline" @click="$emit('retry')">Try again</button>
  </div>
  <p v-else-if="count === 0" class="p-6 text-center text-sm text-muted">{{ emptyMessage }}</p>
  <slot v-else />
</template>
