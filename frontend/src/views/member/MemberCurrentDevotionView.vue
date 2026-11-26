<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave } from 'vue-router'
import { ApiError } from '../../api/http'
import { getMyRoom } from '../../api/memberRooms'
import { getParticipationBoard, submitUnderstanding } from '../../api/participation'
import { btnOutline } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import DevotionCard from '../../components/DevotionCard.vue'
import PageState from '../../components/PageState.vue'
import ParticipationOrderList from '../../components/ParticipationOrderList.vue'
import ProgressBar from '../../components/ProgressBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import SubmissionForm from '../../components/SubmissionForm.vue'
import SubmissionsPanel from '../../components/SubmissionsPanel.vue'
import { usePolling } from '../../composables/usePolling'
import { formatDay } from '../../utils/dates'
import { formatDateTime } from '../../utils/format'
import GuidancePanel from '../../components/GuidancePanel.vue'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const board = ref(null)
const refreshing = ref(false)
const notice = ref('')
const pageError = ref('')
const announcement = ref('') // read out by screen readers only
let latestRequest = 0

// ---------- the draft (kept in memory only: it is a personal reflection) ----------
const draft = ref('')
const draftFor = ref(null) // which devotion the draft was written for
const staleDraft = ref('') // text that was not shared before a new day's devotion replaced the old one
const fieldError = ref('')
const submit = reactive({ open: false, busy: false, error: '' })

const devotion = computed(() => board.value?.devotion ?? null)
const myStatus = computed(() => board.value?.myStatus ?? null)
const order = computed(() => board.value?.order ?? [])
const dirty = computed(() => myStatus.value === 'ELIGIBLE' && draft.value.trim() !== '')

// Exempt members are shown but left out of the count, so they do not lower the group's progress.
const sharedCount = computed(() => order.value.filter((entry) => entry.status === 'SUBMITTED').length)
const exemptCount = computed(() => order.value.filter((entry) => entry.status === 'EXEMPTED').length)
const expectedCount = computed(() => order.value.length - exemptCount.value)

// The answers panel reloads quietly when this changes (someone shared, or my own status changed).
const answersKey = computed(() => `${myStatus.value}:${sharedCount.value}`)

watch(draft, (text) => {
  if (text) draftFor.value = devotion.value?.id ?? null
})

// ---------- loading ----------
// Returns true when the member's turn has just arrived.
function applyBoard(data) {
  const before = board.value

  // A new day (or a different devotion) replaced the one this draft was written for: never send it there.
  const draftIsForAnotherDevotion =
    draft.value.trim() !== '' && draftFor.value !== null && data.devotion && data.devotion.id !== draftFor.value
  if (draftIsForAnotherDevotion) {
    staleDraft.value = draft.value
    draft.value = ''
    draftFor.value = null
    fieldError.value = ''
  }

  board.value = data

  const turnArrived = before?.myStatus === 'WAITING' && data.myStatus === 'ELIGIBLE'
  if (turnArrived) {
    notice.value = 'It’s your turn! You can share your understanding now.'
    announcement.value = notice.value
  }
  return turnArrived
}

async function loadAll() {
  const id = ++latestRequest
  state.value = 'loading'
  board.value = null
  draft.value = ''
  draftFor.value = null
  staleDraft.value = ''
  notice.value = ''
  pageError.value = ''
  fieldError.value = ''

  try {
    const [roomData, boardData] = await Promise.all([getMyRoom(props.roomId), getParticipationBoard(props.roomId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData.room
    applyBoard(boardData)
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    // The server answers 404 when the room is archived or the member was removed from it.
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// mode: 'manual' (the Refresh button) or 'poll' (silent, on a timer)
async function fetchBoard(mode) {
  const id = ++latestRequest
  if (mode === 'manual') refreshing.value = true
  try {
    const data = await getParticipationBoard(props.roomId)
    if (id !== latestRequest) return
    const turnArrived = applyBoard(data)
    if (mode === 'manual' && !turnArrived) {
      announcement.value = data.devotion ? 'Updated.' : 'Updated. No devotion has been posted yet today.'
    }
  } catch (e) {
    if (id !== latestRequest) return
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (mode === 'manual') announcement.value = 'Could not refresh. Please try again.'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

function refreshBoard(mode) {
  // No background refresh while the member is busy sending their answer.
  if (state.value !== 'ready' || submit.busy) return
  return fetchBoard(mode)
}

usePolling(() => refreshBoard('poll'))

// ---------- sharing ----------
function askSubmit() {
  pageError.value = ''
  fieldError.value = ''
  if (draft.value.trim() === '') {
    fieldError.value = 'Please write your understanding of the passage.'
    return
  }
  Object.assign(submit, { open: true, busy: false, error: '' })
}

async function confirmSubmit() {
  if (submit.busy || !devotion.value) return
  submit.busy = true
  submit.error = ''
  try {
    await submitUnderstanding(props.roomId, devotion.value.id, draft.value.trim())
    submit.open = false
    draft.value = ''
    draftFor.value = null
    notice.value = 'Your understanding was shared. Thank you!'
    announcement.value = notice.value
    await fetchBoard('poll') // show the result at once
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      submit.open = false
      state.value = 'notfound'
    } else if (e instanceof ApiError && e.status === 400) {
      submit.open = false
      fieldError.value = e.fieldErrors.content || e.message
    } else if (e instanceof ApiError && [409, 422].includes(e.status)) {
      // The turn, the day or the exemption changed under us: say so, and show what is true now.
      submit.open = false
      pageError.value = e.message
      await fetchBoard('poll')
    } else {
      // No connection or a server error: keep the dialog open so the member can try again.
      submit.error = e?.message || 'Something went wrong. Please try again.'
    }
  } finally {
    submit.busy = false
  }
}

// ---------- do not lose an unsent answer by accident ----------
onBeforeRouteLeave(
  () => !dirty.value || window.confirm('You have text you haven’t shared yet. Leave without sharing?'),
)

function warnBeforeUnload(event) {
  if (dirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', warnBeforeUnload)
  loadAll()
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeUnload))

watch(() => props.roomId, loadAll)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'member-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Your room
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading today’s devotion…"
      not-found-title="This room could not be found."
      not-found-hint="You may no longer be in this room, or it may have been archived. Your dashboard lists the rooms you belong to."
      error-text="Today’s devotion could not be loaded."
      @retry="loadAll"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Today’s devotion</h1>
          <RoomTitle
            :church-name="room.churchName"
            :room-name="room.roomName"
            :leader-name="room.leaderName" :pastor-name="room.pastorName" :church-verified="room.churchVerified"
            :level="2"
          />
          <p class="text-sm text-muted">{{ formatDay(board.today) }}</p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="fetchBoard('manual')">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
        {{ notice }}
      </p>
      <p v-if="pageError" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
        {{ pageError }}
      </p>

      <!-- Text typed for a devotion that has since been replaced (for example at midnight) -->
      <section v-if="staleDraft" role="alert" class="space-y-3 rounded-[18px] border border-gold-line bg-gold-50 p-5">
        <p class="font-semibold text-navy-900">
          The devotion changed while you were writing, so your unsent text was kept here instead of being shared
          with the new devotion.
        </p>
        <label for="stale-draft" class="sr-only">Your unsent text</label>
        <textarea
          id="stale-draft"
          readonly
          rows="5"
          :value="staleDraft"
          class="block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900"
        ></textarea>
        <button type="button" :class="btnOutline" @click="staleDraft = ''">Dismiss</button>
      </section>

      <template v-if="devotion">
        <DevotionCard
          :reference="devotion.reference"
          :title="devotion.title ?? ''"
          :level="devotion.learningLevel"
          :instructions="devotion.instructions ?? ''"
          :date="devotion.date"
        />

        <SubmissionForm
          v-if="myStatus === 'ELIGIBLE'"
          v-model="draft"
          :error="fieldError"
          :saving="submit.busy"
          @submit="askSubmit"
        />

        <section
          v-else-if="myStatus === 'SUBMITTED' && board.mySubmission"
          class="space-y-2 rounded-[18px] border border-gold-line bg-gold-50 p-5"
        >
          <p class="text-xs font-bold uppercase tracking-[2px] text-gold-700">Shared</p>
          <h2 class="text-lg font-bold text-navy-900">You shared your understanding</h2>
          <p class="text-sm text-muted">{{ formatDateTime(board.mySubmission.submittedAt) }}</p>
          <p class="whitespace-pre-line break-words text-sm text-navy-900">{{ board.mySubmission.content }}</p>
        </section>

        <section v-else-if="myStatus === 'WAITING' && board.waitingFor" class="space-y-1 rounded-[18px] border border-line bg-white p-5">
          <p class="text-xs font-bold uppercase tracking-[2px] text-muted">Please wait</p>
          <h2 class="text-lg font-bold text-navy-900">It isn’t your turn yet</h2>
          <p class="text-sm text-muted">
            You are No. {{ board.myParticipationNumber }}. Please wait for
            <strong class="text-navy-900">{{ board.waitingFor.fullName }}</strong>
            (No. {{ board.waitingFor.participationNumber }}) to share first. This page checks for updates by itself,
            and your turn appears here as soon as it comes.
          </p>
        </section>

        <section v-else-if="myStatus === 'EXEMPTED'" class="space-y-1 rounded-[18px] border border-line bg-white p-5">
          <p class="text-xs font-bold uppercase tracking-[2px] text-muted">Exempt today</p>
          <h2 class="text-lg font-bold text-navy-900">You don’t need to share today</h2>
          <p class="text-sm text-muted">
            Your leader has exempted you from today’s devotion, so nobody waits for you. If this is a mistake, please
            ask your leader.
          </p>
        </section>

        <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="order-title">
          <div class="space-y-3 border-b border-line p-4">
            <h2 id="order-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Participation order</h2>
            <ProgressBar
              label="Shared today"
              :value="sharedCount"
              :max="expectedCount"
              :summary="`${sharedCount} of ${expectedCount} members shared today`"
            />
            <p v-if="exemptCount > 0" class="text-sm text-muted">
              {{ exemptCount }} {{ exemptCount === 1 ? 'member is' : 'members are' }} exempt today and not counted.
            </p>
          </div>
          <ParticipationOrderList :order="order" />
        </section>

        <SubmissionsPanel
          :room-id="roomId"
          :devotion-id="devotion.id"
          :refresh-key="answersKey"
          hide-own
        />
        <GuidancePanel :room-id="roomId" :devotion-id="devotion.id" :refresh-key="answersKey" />
      </template>

      <section v-else class="rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">No devotion has been posted for today yet.</p>
        <p class="mt-1 text-sm text-muted">
          Your leader hasn’t posted today’s passage. This page checks for updates by itself, or press Refresh.
        </p>
      </section>

      <RouterLink :to="{ name: 'member-devotions', params: { roomId } }" :class="btnOutline">
        Past devotions
      </RouterLink>
    </PageState>

    <ConfirmDialog
      v-model:open="submit.open"
      title="Share your understanding?"
      message="Once you share it, your answer can’t be edited or deleted. Your leader and the other members of this room can read it, and the next member in the order can take their turn."
      confirm-label="Share"
      :busy="submit.busy"
      :error="submit.error"
      @confirm="confirmSubmit"
    />
  </div>
</template>
