<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  {
    label: 'Dashboard',
    to: { name: 'pastor-dashboard' },
    alsoActiveOn: ['pastor-leader-stats', 'pastor-room-stats'],
  },
  { label: 'My Leaders', to: { name: 'pastor-leaders' } },
  { label: 'Reports', to: { name: 'pastor-reports' } },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
