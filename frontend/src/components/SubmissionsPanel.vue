<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getRoomAnswers } from '../api/participation'
import { formatDateTime } from '../utils/format'
import { btnOutline } from './buttonClasses'
import ParticipationBadge from './ParticipationBadge.vue'
import ReportAnswerDialog from './ReportAnswerDialog.vue'

const props = defineProps({
  roomId: { type: Number, required: true },
  devotionId: { type: Number, required: true },
  // Changes when the room's answers may have changed (someone shared): triggers a quiet reload.
  refreshKey: { type: [String, Number], default: '' },
  // Leave out my own answer (the page already shows it elsewhere).
  hideOwn: { type: Boolean, default: false },
})

const reportOpen = ref(false)
const reportTarget = ref(null)
const reported = ref(new Set()) // answers I reported in this visit
const reportNotice = ref('')

function startReport(answer) {
  reportTarget.value = answer.id
  reportOpen.value = true
}

function onReported(id) {
  reported.value = new Set([...reported.value, id])
  reportNotice.value = 'Thank you. Your leader will review it.'
}

const state = ref('loading') // loading | ready | error
const data = ref(null)
let latestRequest = 0

async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (!quiet || !data.value) state.value = 'loading'
  try {
    const result = await getRoomAnswers(props.roomId, props.devotionId)
    if (id !== latestRequest) return // a newer request replaced this one
    data.value = result
    state.value = 'ready'
  } catch {
    if (id !== latestRequest) return
    if (!quiet || !data.value) state.value = 'error' // a failed quiet reload keeps what is shown
  }
}

const visible = computed(() => {
  const all = data.value?.submissions ?? []
  return props.hideOwn ? all.filter((answer) => !answer.you) : all
})

function countText(count) {
  if (count === 0) return 'No one has shared yet.'
  return count === 1 ? '1 member has shared so far.' : `${count} members have shared so far.`
}

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
  <section aria-labelledby="answers-title" class="space-y-3">
    <h2 id="answers-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">What others shared</h2>

    <p v-if="state === 'loading'" role="status" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
      Loading…
    </p>

    <div v-else-if="state === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">The answers could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="load()">Try again</button>
    </div>

    <div v-else-if="!data.unlocked" class="space-y-3 rounded-[18px] border border-line bg-white p-5">
      <p class="font-semibold text-navy-900">{{ data.lockedMessage }}</p>
      <p class="text-sm text-muted">{{ countText(data.submittedCount) }}</p>
      <slot name="locked" />
    </div>

    <p v-else-if="visible.length === 0" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
      {{ hideOwn ? 'No one else has shared yet.' : 'No one has shared yet.' }}
    </p>

    <ul v-else class="space-y-3">
      <li
        v-for="answer in visible"
        :key="answer.id"
        :class="['space-y-2 rounded-[18px] border p-4', answer.you ? 'border-gold-line bg-gold-50' : 'border-line bg-white']"
      >
        <div class="flex flex-wrap items-center gap-3">
          <ParticipationBadge v-if="answer.participationNumber" :number="answer.participationNumber" />
          <p class="min-w-0 flex-1 font-semibold text-navy-900">{{ answer.fullName }}</p>
          <span v-if="answer.you" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">You</span>
          <span v-else-if="!answer.participationNumber" class="rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
            No longer in the room
          </span>
        </div>
        <p class="text-sm text-muted">{{ formatDateTime(answer.submittedAt) }}</p>
        <p :class="['whitespace-pre-line break-words text-sm', answer.removed ? 'italic text-muted' : 'text-navy-900']">
          {{ answer.content }}
        </p>
        <div v-if="!answer.you && !answer.removed" class="flex justify-end">
          <span v-if="reported.has(answer.id)" class="text-sm font-semibold text-muted">Reported</span>
          <button
            v-else
            type="button"
            class="inline-flex min-h-11 items-center rounded-xl px-3 text-sm font-semibold text-muted underline underline-offset-4 hover:text-navy-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900"
            @click="startReport(answer)"
          >
            Report<span class="sr-only"> {{ answer.fullName }}’s answer</span>
          </button>
        </div>
      </li>
    </ul>
    <p v-if="reportNotice" role="status" class="rounded-xl bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ reportNotice }}
    </p>
    <ReportAnswerDialog v-model:open="reportOpen" :room-id="roomId" :submission-id="reportTarget" @reported="onReported" />
  </section>
</template>
