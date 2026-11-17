<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  { label: 'Dashboard', to: { name: 'admin-dashboard' } },
  { label: 'Churches', to: { name: 'admin-churches' } },
  { label: 'Pastors', to: { name: 'admin-pastors' } },
  { label: 'Leaders', to: { name: 'admin-leaders' } },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
