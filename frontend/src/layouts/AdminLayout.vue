<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  { label: 'Dashboard', to: { name: 'admin-dashboard' } },
  { label: 'Rooms', to: { name: 'admin-rooms' }, alsoActiveOn: ['admin-room-stats'] },
  { label: 'Members', to: { name: 'admin-members' }, alsoActiveOn: ['admin-member'] },
  { label: 'Analytics', to: { name: 'admin-analytics' }, alsoActiveOn: ['admin-leader-stats'], more: true },
  { label: 'Churches', to: { name: 'admin-churches' }, more: true },
  { label: 'Pastors', to: { name: 'admin-pastors' }, more: true },
  { label: 'Leaders', to: { name: 'admin-leaders' }, more: true },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
