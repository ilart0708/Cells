<script setup>
import PlaceholderDashboard from '../../components/PlaceholderDashboard.vue'

const pages = [
  { name: 'My Cell Group', phase: 4 },
  { name: 'Current Devotion', phase: 5 },
  { name: 'Bible Passage', phase: 5 },
  { name: 'Submit Understanding', phase: 6 },
  { name: 'Participation Order', phase: 6 },
  { name: 'Participation History', phase: 7 },
  { name: 'Experience Statistics', phase: 7 },
]
</script>

<template>
  <PlaceholderDashboard :pages="pages" />
</template>
