<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [
  { label: 'Dashboard', to: { name: 'pastor-dashboard' } },
  { label: 'My Leaders', to: { name: 'pastor-leaders' } },
]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
