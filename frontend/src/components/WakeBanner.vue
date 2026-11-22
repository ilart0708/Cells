<script setup>
import { serverWaking } from '@/utils/serverWake'
</script>

<template>
  <div
    v-if="serverWaking"
    role="status"
    class="fixed inset-x-0 top-0 z-50 bg-gold-100 px-4 py-3 pt-[calc(0.75rem+env(safe-area-inset-top))] text-center text-sm font-semibold text-navy-900"
  >
    Waking up the server… this can take up to a minute the first time.
  </div>
</template>
