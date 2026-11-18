<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  {
    label: 'Dashboard',
    to: { name: 'member-dashboard' },
    alsoActiveOn: ['member-room', 'member-devotion-current', 'member-devotions', 'member-devotion'],
  },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
