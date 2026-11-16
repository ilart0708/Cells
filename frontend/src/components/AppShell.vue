<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ROLE_LABEL } from '../constants/roles'

defineProps({
  navItems: { type: Array, required: true }, // [{ label, to }]
})

const auth = useAuthStore()
const router = useRouter()

async function logout() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="min-h-screen bg-app-bg md:flex">
    <!-- Desktop sidebar -->
    <aside class="hidden w-48 shrink-0 flex-col bg-navy-900 md:flex">
      <div class="px-5 py-6 font-heading text-xl font-extrabold tracking-[4px] text-white">
        CELLS
      </div>
      <nav aria-label="Main" class="flex flex-col gap-1 px-3">
        <RouterLink v-for="item in navItems" :key="item.label" :to="item.to" custom v-slot="{ href, navigate, isActive }">
          <a
            :href="href"
            :aria-current="isActive ? 'page' : undefined"
            :class="[
              'flex min-h-11 items-center rounded-xl px-3 text-sm font-semibold',
              isActive ? 'bg-gold-500/15 text-gold-500' : 'text-on-navy hover:bg-white/10',
            ]"
            @click="navigate"
          >
            {{ item.label }}
          </a>
        </RouterLink>
      </nav>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex items-center gap-3 border-b border-line bg-white px-4 py-3 md:px-8">
        <span class="font-heading text-xl font-extrabold tracking-[4px] text-navy-900 md:hidden">
          CELLS
        </span>
        <div class="flex-1"></div>
        <span class="hidden text-sm font-semibold text-muted sm:inline">{{ auth.user.fullName }}</span>
        <span
          class="rounded-full bg-navy-900 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-gold-500"
        >
          {{ ROLE_LABEL[auth.role] }}
        </span>
        <button
          type="button"
          class="min-h-11 rounded-xl bg-navy-900 px-4 text-sm font-bold text-white hover:bg-navy-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
          @click="logout"
        >
          Log out
        </button>
      </header>

      <main class="flex-1 p-4 pb-24 md:p-8">
        <RouterView />
      </main>
    </div>

    <!-- Mobile bottom tab bar -->
    <nav
      aria-label="Main"
      class="fixed inset-x-0 bottom-0 flex h-[78px] border-t border-line bg-white md:hidden"
    >
      <RouterLink v-for="item in navItems" :key="item.label" :to="item.to" custom v-slot="{ href, navigate, isActive }">
        <a
          :href="href"
          :aria-current="isActive ? 'page' : undefined"
          :class="[
            'flex flex-1 items-center justify-center text-xs font-semibold',
            isActive ? 'text-navy-900' : 'text-muted',
          ]"
          @click="navigate"
        >
          {{ item.label }}
        </a>
      </RouterLink>
    </nav>
  </div>
</template>
