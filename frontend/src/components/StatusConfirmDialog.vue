<script setup>
import ConfirmDialog from './ConfirmDialog.vue'

defineProps({
  name: { type: String, default: '' },
  nextStatus: { type: String, required: true }, // 'ACTIVE' or 'DISABLED'
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const open = defineModel('open', { type: Boolean, default: false })
defineEmits(['confirm'])
</script>

<template>
  <ConfirmDialog
    v-model:open="open"
    :title="`${nextStatus === 'DISABLED' ? 'Disable' : 'Enable'} ${name}?`"
    :message="
      nextStatus === 'DISABLED'
        ? 'They will no longer be able to sign in, and anyone already signed in is signed out within 15 minutes. Their data is kept and you can enable them again at any time.'
        : 'They will be able to sign in again.'
    "
    :confirm-label="nextStatus === 'DISABLED' ? 'Disable' : 'Enable'"
    :busy="busy"
    :error="error"
    @confirm="$emit('confirm')"
  />
</template>
