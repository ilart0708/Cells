<script setup>
import { ref } from 'vue'
import { ApiError, request } from '../../api/http'

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
</script>

<template>
  <section class="rounded-[18px] border border-dashed border-line-input bg-white p-4" aria-labelledby="probe-title">
    <h2 id="probe-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">
      Temporary: direct API role checks
    </h2>
    <ul class="mt-3 space-y-2">
      <li v-for="area in areas" :key="area" class="flex flex-wrap items-center gap-3">
        <button
          type="button"
          class="min-h-11 w-36 rounded-xl bg-navy-900 px-3 text-sm font-semibold text-white hover:bg-navy-700"
          @click="probe(area)"
        >
          /probe/{{ area }}
        </button>
        <span class="text-sm text-muted">{{ results[area] }}</span>
      </li>
    </ul>
  </section>
</template>
