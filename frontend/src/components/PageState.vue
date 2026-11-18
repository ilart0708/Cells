<script setup>
import { btnOutline } from './buttonClasses'

defineProps({
  state: { type: String, required: true }, // loading | ready | notfound | error
  loadingText: { type: String, default: 'Loading…' },
  notFoundTitle: { type: String, default: 'This page could not be found.' },
  notFoundHint: { type: String, default: '' },
  errorText: { type: String, default: 'The page could not be loaded.' },
})

defineEmits(['retry'])
</script>

<template>
  <p v-if="state === 'loading'" role="status" class="p-6 text-center text-sm text-muted">{{ loadingText }}</p>

  <div v-else-if="state === 'notfound'" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
    <p class="font-semibold text-navy-900">{{ notFoundTitle }}</p>
    <p v-if="notFoundHint" class="mt-1 text-sm text-muted">{{ notFoundHint }}</p>
  </div>

  <div v-else-if="state === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
    <p class="text-sm font-semibold text-red-700">{{ errorText }}</p>
    <button type="button" :class="btnOutline" @click="$emit('retry')">Try again</button>
  </div>

  <slot v-else />
</template>
