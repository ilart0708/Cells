<script setup>
import PlaceholderDashboard from '../components/PlaceholderDashboard.vue'

const pages = [
  { name: 'My Rooms', phase: 4 },
  { name: 'Create Room', phase: 4 },
  { name: 'Manage Members', phase: 4 },
  { name: 'Participation Order Management', phase: 4 },
  { name: 'Create Devotion', phase: 5 },
  { name: 'Devotion History', phase: 5 },
  { name: 'Daily Exemption Controls', phase: 6 },
  { name: 'Member Submissions', phase: 6 },
  { name: 'Group Summary / Personal Reflection', phase: 7 },
]
</script>

<template>
  <PlaceholderDashboard :pages="pages" />
</template>
