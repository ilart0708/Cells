<script setup>
import BaseDialog from './BaseDialog.vue'
import { btnOutline, btnSecondary } from './buttonClasses'

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  confirmLabel: { type: String, default: 'Confirm' },
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const open = defineModel('open', { type: Boolean, default: false })
defineEmits(['confirm'])
</script>

<template>
  <BaseDialog v-model:open="open" :title="title">
    <p class="text-sm text-muted">{{ message }}</p>
    <p v-if="error" role="alert" class="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
      {{ error }}
    </p>
    <div class="mt-6 flex justify-end gap-3">
      <button type="button" :class="btnOutline" @click="open = false">Cancel</button>
      <button type="button" :class="btnSecondary" :disabled="busy" @click="$emit('confirm')">
        {{ busy ? 'Working…' : confirmLabel }}
      </button>
    </div>
  </BaseDialog>
</template>
