<script setup>
import PlaceholderDashboard from '../components/PlaceholderDashboard.vue'

const pages = [
  { name: 'Room Monitoring', phase: 8 },
  { name: 'Member Management', phase: 8 },
  { name: 'Participation Analytics', phase: 8 },
]
</script>

<template>
  <PlaceholderDashboard :pages="pages" />
</template>
