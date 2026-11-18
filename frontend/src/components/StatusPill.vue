<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, required: true } })

const LABELS = {
  ACTIVE: 'Active',
  PENDING: 'Pending',
  ACCEPTED: 'Accepted',
  EXPIRED: 'Expired',
  REVOKED: 'Revoked',
  DISABLED: 'Disabled',
  ARCHIVED: 'Archived',
  CANCELLED: 'Cancelled',
}
const POSITIVE = new Set(['ACTIVE', 'ACCEPTED'])

const label = computed(() => LABELS[props.status] ?? props.status)
// The text always carries the meaning; the tint is only reinforcement.
const tone = computed(() => (POSITIVE.has(props.status) ? 'bg-gold-100' : 'bg-track'))
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-bold text-navy-900', tone]">
    {{ label }}
  </span>
</template>
