<script setup>
import { useAuthStore } from '../stores/auth'

defineProps({
  pages: { type: Array, required: true }, // [{ name, phase }]
})

const auth = useAuthStore()
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <section class="bg-room-card rounded-[18px] p-5 text-white">
      <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your area</p>
      <h1 class="mt-1 text-2xl font-bold">Welcome, {{ auth.user.fullName }}</h1>
      <p class="mt-1 text-sm text-on-navy-soft">{{ auth.user.email }}</p>
    </section>

    <section v-if="pages.length > 0" class="rounded-[18px] border border-line bg-white p-4" aria-labelledby="next-title">
      <h2 id="next-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">
        Coming next
      </h2>
      <ul class="mt-3 divide-y divide-line">
        <li v-for="page in pages" :key="page.name" class="flex items-center justify-between gap-3 py-3">
          <span class="text-sm font-semibold text-navy-900">{{ page.name }}</span>
          <span class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">
            Phase {{ page.phase }}
          </span>
        </li>
      </ul>
    </section>
  </div>
</template>
