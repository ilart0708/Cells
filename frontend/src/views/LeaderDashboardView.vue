<script setup>
import PlaceholderDashboard from '../components/PlaceholderDashboard.vue'
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <PlaceholderDashboard :pages="[]" />
    <RouterLink
      :to="{ name: 'leader-rooms' }"
      class="flex min-h-[44px] items-center justify-center rounded-[14px] bg-gold-500 px-5 text-base font-bold text-navy-900"
    >
      Go to My Rooms
    </RouterLink>
  </div>
</template>
