<script setup>
import { onMounted, ref, watch } from 'vue'
import { ApiError } from '../api/http'
import { getMemberGuidance } from '../api/guidance'
import { usePolling } from '../composables/usePolling'
import { btnOutline } from './buttonClasses'
import GuidanceNote from './GuidanceNote.vue'

const props = defineProps({
  roomId: { type: Number, required: true },
  devotionId: { type: Number, required: true },
  // Changes when the room's content may have changed (someone shared): triggers a quiet reload.
  refreshKey: { type: [String, Number], default: '' },
})

const state = ref('loading') // loading | ready | error | hidden
const data = ref(null)
let latestRequest = 0

async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (!quiet || !data.value) state.value = 'loading'
  try {
    const result = await getMemberGuidance(props.roomId, props.devotionId)
    if (id !== latestRequest) return // a newer request replaced this one
    data.value = result
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    // 404: the page itself is about to say "not found", so there is nothing to show here.
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'hidden'
    else if (!quiet || !data.value) state.value = 'error' // a failed quiet reload keeps what is shown
  }
}

// The leader may write or edit while the member is reading.
usePolling(() => {
  if (state.value === 'ready') load({ quiet: true })
}, 60000)

onMounted(() => load())

watch(
  () => props.devotionId,
  () => {
    data.value = null
    load()
  },
)
watch(() => props.refreshKey, () => load({ quiet: true }))
</script>

<template>
  <section v-if="state !== 'hidden'" aria-labelledby="guidance-title" class="space-y-3">
    <h2 id="guidance-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">From your leader</h2>

    <p v-if="state === 'loading'" role="status" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
      Loading…
    </p>

    <div v-else-if="state === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">Your leader’s notes could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="load()">Try again</button>
    </div>

    <p v-else-if="!data.unlocked" class="text-sm text-muted">{{ data.lockedMessage }}</p>

    <p v-else-if="!data.summary && !data.reflection" class="rounded-[18px] border border-line bg-white p-5 text-center text-sm text-muted">
      Your leader hasn’t written a summary or reflection for this devotion yet.
    </p>

    <div v-else class="space-y-3">
      <GuidanceNote v-if="data.summary" kind="SUMMARY" :note="data.summary" />
      <GuidanceNote v-if="data.reflection" kind="REFLECTION" :note="data.reflection" />
    </div>
  </section>
</template>
