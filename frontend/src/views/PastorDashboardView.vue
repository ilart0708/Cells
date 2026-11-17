<script setup>
import PlaceholderDashboard from '../components/PlaceholderDashboard.vue'

const pages = [
  { name: 'Leader Statistics', phase: 8 },
  { name: 'Room Statistics', phase: 8 },
  { name: 'Participation Reports', phase: 8 },
]
</script>

<template>
  <PlaceholderDashboard :pages="pages" />
</template>
