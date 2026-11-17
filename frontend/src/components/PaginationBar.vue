<script setup>
import { btnOutline } from './buttonClasses'

defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  loading: { type: Boolean, default: false },
})

defineEmits(['change'])
</script>

<template>
  <nav
    v-if="totalPages > 1"
    aria-label="Pagination"
    class="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm text-muted"
  >
    <span>Page {{ page + 1 }} of {{ totalPages }} · {{ totalElements }} total</span>
    <span class="flex gap-2">
      <button type="button" :class="btnOutline" :disabled="page === 0 || loading" @click="$emit('change', page - 1)">
        Previous
      </button>
      <button
        type="button"
        :class="btnOutline"
        :disabled="page >= totalPages - 1 || loading"
        @click="$emit('change', page + 1)"
      >
        Next
      </button>
    </span>
  </nav>
</template>
