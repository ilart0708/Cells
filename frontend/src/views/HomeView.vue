<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ApiError, request } from '../api/http'

const auth = useAuthStore()
const router = useRouter()

const areas = ['any', 'admin', 'pastor', 'leader', 'member']
const results = ref({})

async function probe(area) {
  try {
    const data = await request(`/probe/${area}`)
    results.value[area] = `200 – ${data.area}`
  } catch (error) {
    results.value[area] = error instanceof ApiError ? `${error.status} – ${error.message}` : 'error'
  }
}

// Fires five calls at once: after the token expires they must share ONE refresh.
async function probeBurst() {
  await Promise.all(Array.from({ length: 5 }, () => probe('any')))
}

async function logout() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <main class="mx-auto max-w-2xl p-6">
    <header class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-slate-900">{{ auth.user.fullName }}</h1>
        <p class="text-sm text-slate-600">
          {{ auth.user.email }} ·
          <span class="rounded bg-indigo-100 px-2 py-0.5 text-xs font-semibold text-indigo-700">{{
            auth.role
          }}</span>
        </p>
      </div>
      <button
        type="button"
        class="rounded-md px-3 py-2 text-sm font-semibold text-slate-700 ring-1 ring-slate-300 hover:bg-slate-100"
        @click="logout"
      >
        Log out
      </button>
    </header>

    <section class="mt-8" aria-labelledby="probe-title">
      <h2 id="probe-title" class="text-sm font-semibold uppercase tracking-wide text-slate-500">
        Temporary: API role checks
      </h2>
      <ul class="mt-3 space-y-2">
        <li v-for="area in areas" :key="area" class="flex items-center gap-3">
          <button
            type="button"
            class="w-28 rounded-md bg-slate-800 px-3 py-1.5 text-sm text-white hover:bg-slate-700"
            @click="probe(area)"
          >
            /probe/{{ area }}
          </button>
          <span class="text-sm text-slate-700">{{ results[area] }}</span>
        </li>
      </ul>
      <button
        type="button"
        class="mt-4 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-500"
        @click="probeBurst"
      >
        Fire 5 requests at once
      </button>
    </section>
  </main>
</template>
