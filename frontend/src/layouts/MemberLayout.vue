<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  {
    label: 'Dashboard',
    to: { name: 'member-dashboard' },
    alsoActiveOn: [
      'member-room',
      'member-devotion-current',
      'member-devotions',
      'member-devotion',
      'member-board',
    ],
  },
  { label: 'Experience', to: { name: 'member-experience' } },
  { label: 'History', to: { name: 'member-history' }, alsoActiveOn: ['member-history-room'] },
  { label: 'Account', to: { name: 'member-account' }, more: true },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
