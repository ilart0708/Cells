<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave } from 'vue-router'
import { getLeaderGuidance, saveReflection, saveSummary } from '../../api/guidance'
import { ApiError } from '../../api/http'
import { getDevotionAnswers } from '../../api/leaderParticipation'
import { getRoom } from '../../api/rooms'
import { btnOutline } from '../../components/buttonClasses'
import DevotionCard from '../../components/DevotionCard.vue'
import GuidanceEditor from '../../components/GuidanceEditor.vue'
import PageState from '../../components/PageState.vue'
import ParticipationBadge from '../../components/ParticipationBadge.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import { formatDateTime } from '../../utils/format'

const props = defineProps({
  roomId: { type: Number, required: true },
  devotionId: { type: Number, required: true },
})

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const data = ref(null)
const refreshing = ref(false)
const announcement = ref('') // read out by screen readers only
let latestRequest = 0

const devotion = computed(() => data.value?.devotion ?? null)
const answers = computed(() => data.value?.submissions ?? [])

// ---------- the summary and the reflection ----------
const guidance = ref(null)
const guidanceState = ref('loading') // loading | ready | error
const summaryDirty = ref(false)
const reflectionDirty = ref(false)
const unsaved = computed(() => summaryDirty.value || reflectionDirty.value)
let latestGuidance = 0

async function loadGuidance({ quiet = false } = {}) {
  const id = ++latestGuidance
  if (!quiet || !guidance.value) guidanceState.value = 'loading'
  try {
    const result = await getLeaderGuidance(props.roomId, props.devotionId)
    if (id !== latestGuidance) return
    guidance.value = result
    guidanceState.value = 'ready'
  } catch {
    if (id !== latestGuidance) return
    if (!quiet || !guidance.value) guidanceState.value = 'error' // a failed quiet reload keeps what is shown
  }
}

function onSummarySaved(note) {
  guidance.value = { ...guidance.value, summary: note }
  announcement.value = 'Group summary saved.'
}

function onReflectionSaved(note) {
  guidance.value = { ...guidance.value, reflection: note }
  announcement.value = 'Reflection saved.'
}

// Do not lose unsaved writing by accident.
onBeforeRouteLeave(() => !unsaved.value || window.confirm('You have text you haven’t saved yet. Leave without saving?'))

function warnBeforeUnload(event) {
  if (unsaved.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

// ---------- the answers ----------
async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (quiet) refreshing.value = true
  else state.value = 'loading'
  loadGuidance({ quiet })
  try {
    const [roomData, answerData] = await Promise.all([getRoom(props.roomId), getDevotionAnswers(props.roomId, props.devotionId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData
    data.value = answerData
    state.value = 'ready'
    if (quiet) announcement.value = 'Updated.'
  } catch (e) {
    if (id !== latestRequest) return
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (!quiet) state.value = 'error'
    else announcement.value = 'Could not refresh. Please try again.'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', warnBeforeUnload)
  load()
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeUnload))

watch(
  () => [props.roomId, props.devotionId],
  () => {
    guidance.value = null
    summaryDirty.value = false
    reflectionDirty.value = false
    load()
  },
)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'leader-devotions', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Devotions
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading the answers…"
      not-found-title="This devotion could not be found."
      not-found-hint="Go back to the devotions of your room."
      error-text="The answers could not be loaded."
      @retry="load()"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Answers</h1>
          <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" :level="2" />
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="load({ quiet: true })">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <DevotionCard
        :reference="devotion.reference"
        :title="devotion.title ?? ''"
        :level="devotion.learningLevel"
        :instructions="devotion.instructions ?? ''"
        :date="devotion.date"
      />

      <section aria-labelledby="answers-title" class="space-y-3">
        <div>
          <h2 id="answers-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">
            {{ answers.length }} {{ answers.length === 1 ? 'answer' : 'answers' }}
          </h2>
          <p class="mt-1 text-sm text-muted">
            Only you and the members of this room can read these. Members read them after sharing their own, or once
            the day is over.
          </p>
        </div>

        <p v-if="answers.length === 0" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
          No one has shared their understanding of this devotion yet.
        </p>

        <ul v-else class="space-y-3">
          <li v-for="answer in answers" :key="answer.submissionId" class="space-y-2 rounded-[18px] border border-line bg-white p-4">
            <div class="flex flex-wrap items-center gap-3">
              <ParticipationBadge v-if="answer.participationNumber" :number="answer.participationNumber" />
              <p class="min-w-0 flex-1 font-semibold text-navy-900">{{ answer.fullName }}</p>
              <span v-if="!answer.participationNumber" class="rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
                No longer in the room
              </span>
            </div>
            <p class="text-sm text-muted">{{ formatDateTime(answer.submittedAt) }}</p>
            <p class="whitespace-pre-line break-words text-sm text-navy-900">{{ answer.content }}</p>
          </li>
        </ul>
      </section>

      <!-- Your summary and your reflection -->
      <section aria-labelledby="guidance-title" class="space-y-3">
        <div>
          <h2 id="guidance-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Your summary and reflection</h2>
          <p class="mt-1 text-sm text-muted">
            Both are optional and independent. Members read them after sharing their own understanding, or once the day
            is over. You can edit them later.
          </p>
        </div>

        <p v-if="guidanceState === 'loading'" role="status" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
          Loading…
        </p>

        <div v-else-if="guidanceState === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
          <p class="text-sm font-semibold text-red-700">Your summary and reflection could not be loaded.</p>
          <button type="button" :class="btnOutline" @click="loadGuidance()">Try again</button>
        </div>

        <template v-else>
          <p v-if="!guidance.canWrite" role="status" class="rounded-xl border border-line bg-white px-4 py-3 text-sm text-muted">
            {{ guidance.writeBlockedReason }}
          </p>

          <GuidanceEditor
            v-model:dirty="summaryDirty"
            title="Group summary"
            description="Look at what everyone shared, and write the main message the group saw together in the passage and what you learned."
            :note="guidance.summary"
            :disabled="!guidance.canWrite"
            :save="(content) => saveSummary(roomId, devotionId, content)"
            @saved="onSummarySaved"
            @changed="loadGuidance({ quiet: true })"
          />

          <GuidanceEditor
            v-model:dirty="reflectionDirty"
            title="Personal reflection"
            description="Share your own understanding of the passage, in your own words."
            :note="guidance.reflection"
            :disabled="!guidance.canWrite"
            :save="(content) => saveReflection(roomId, devotionId, content)"
            @saved="onReflectionSaved"
            @changed="loadGuidance({ quiet: true })"
          />
        </template>
      </section>
    </PageState>
  </div>
</template>
