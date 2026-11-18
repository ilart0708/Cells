<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  { label: 'Dashboard', to: { name: 'leader-dashboard' } },
  {
    label: 'My Rooms',
    to: { name: 'leader-rooms' },
    alsoActiveOn: ['leader-room', 'leader-devotions', 'leader-devotion-new', 'leader-devotion-edit'],
  },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
