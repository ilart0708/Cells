<script setup>
import PlaceholderDashboard from '../components/PlaceholderDashboard.vue'

const pages = [
  { name: 'Daily Exemption Controls', phase: 6 },
  { name: 'Member Submissions', phase: 6 },
  { name: 'Group Summary / Personal Reflection', phase: 7 },
]
</script>

<template>
  <PlaceholderDashboard :pages="pages" />
</template>
