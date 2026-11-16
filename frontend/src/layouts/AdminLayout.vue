<script setup>
import AppShell from '../components/AppShell.vue'

const navItems = [{ label: 'Dashboard', to: { name: 'admin-dashboard' } }]
</script>

<template>
  <AppShell :nav-items="navItems" />
</template>
