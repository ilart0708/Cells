<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { exemptMember, getLeaderBoard, removeExemption } from '../../api/leaderParticipation'
import { getRoom } from '../../api/rooms'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import ExemptionSwitch from '../../components/ExemptionSwitch.vue'
import LevelPill from '../../components/LevelPill.vue'
import PageState from '../../components/PageState.vue'
import ParticipationBadge from '../../components/ParticipationBadge.vue'
import ParticipationPill from '../../components/ParticipationPill.vue'
import ProgressBar from '../../components/ProgressBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import { usePolling } from '../../composables/usePolling'
import { formatDay } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const board = ref(null)
const refreshing = ref(false)
const notice = ref('')
const problem = ref('')
const announcement = ref('') // read out by screen readers only
const busyId = ref(null) // the member whose switch is being changed
let latestRequest = 0

const open = computed(() => room.value?.status === 'ACTIVE')
const order = computed(() => board.value?.order ?? [])
const devotion = computed(() => board.value?.devotion ?? null)
// Exempt members are shown but left out of the count, so they do not lower the group's progress.
const expectedCount = computed(() => (board.value ? board.value.memberCount - board.value.exemptedCount : 0))
const blockers = computed(() => order.value.filter((entry) => entry.blocking))
const blockerNames = computed(() => blockers.value.map((entry) => entry.fullName).join(', '))

// ---------- loading ----------
function applyBoard(data) {
  const before = board.value
  board.value = data
  if (before && data.submittedCount > before.submittedCount) {
    announcement.value = `A member just shared. ${data.submittedCount} of ${data.memberCount - data.exemptedCount} have shared today.`
  }
}

async function loadAll() {
  const id = ++latestRequest
  state.value = 'loading'
  board.value = null
  notice.value = ''
  problem.value = ''
  try {
    const [roomData, boardData] = await Promise.all([getRoom(props.roomId), getLeaderBoard(props.roomId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData
    board.value = boardData
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// mode: 'manual' (the Refresh button), 'quiet' (after an action) or 'poll' (silent, on a timer)
async function fetchBoard(mode) {
  const id = ++latestRequest
  if (mode === 'manual') refreshing.value = true
  try {
    const [data, roomData] = await Promise.all([
      getLeaderBoard(props.roomId),
      mode === 'manual' ? getRoom(props.roomId) : Promise.resolve(null),
    ])
    if (id !== latestRequest) return
    applyBoard(data)
    if (roomData) room.value = roomData
    if (mode === 'manual') announcement.value = 'Updated.'
  } catch (e) {
    if (id !== latestRequest) return
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (mode === 'manual') announcement.value = 'Could not refresh. Please try again.'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

// ---------- exempting ----------
const removal = reactive({ open: false, target: null, busy: false, error: '' })

const removalMessage = computed(() => {
  const target = removal.target
  if (!target) return ''
  return (
    `${target.fullName} goes back into the line at No. ${target.participationNumber}. ` +
    'Members after them who haven’t shared yet will wait for them again. ' +
    'Members who have already shared are not affected.'
  )
})

function problemFrom(e) {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
}

function toggle(entry) {
  if (busyId.value !== null) return
  if (entry.exempt) {
    Object.assign(removal, { open: true, target: entry, busy: false, error: '' })
  } else {
    grant(entry)
  }
}

async function grant(entry) {
  busyId.value = entry.memberId
  notice.value = ''
  problem.value = ''
  try {
    await exemptMember(props.roomId, entry.memberId)
    notice.value = devotion.value
      ? `${entry.fullName} is exempt for today. The line moves past them.`
      : `${entry.fullName} is exempt for today.`
    announcement.value = notice.value
  } catch (e) {
    // For example: they shared a moment ago (422), or left the room (404). The refresh below shows the truth.
    problem.value = problemFrom(e)
  }
  await fetchBoard('quiet')
  busyId.value = null
}

async function confirmRemove() {
  if (removal.busy || !removal.target) return
  const entry = removal.target
  removal.busy = true
  removal.error = ''
  try {
    await removeExemption(props.roomId, entry.memberId)
    removal.open = false
    notice.value = `${entry.fullName}’s exemption was removed. They are back in the line.`
    announcement.value = notice.value
    problem.value = ''
    await fetchBoard('quiet')
  } catch (e) {
    removal.error = problemFrom(e)
  } finally {
    removal.busy = false
  }
}

// The page keeps itself current while the tab is visible (members share during the day).
usePolling(() => {
  if (state.value !== 'ready' || busyId.value !== null || removal.open) return
  fetchBoard('poll')
})

onMounted(loadAll)
watch(() => props.roomId, loadAll)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'leader-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Room
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading today…"
      not-found-title="This room could not be found."
      not-found-hint="Go back to My Rooms and choose one of your rooms."
      error-text="Today’s board could not be loaded."
      @retry="loadAll"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Today</h1>
          <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" :level="2" />
          <p class="text-sm text-muted">{{ formatDay(board.today) }}</p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="fetchBoard('manual')">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <p v-if="!open" role="status" class="rounded-xl border border-line bg-white px-4 py-3 text-sm text-muted">
        This room is archived, so exemptions can’t be changed. Restore it from My Rooms to continue.
      </p>

      <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
        {{ notice }}
      </p>
      <p v-if="problem" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
        {{ problem }}
      </p>

      <!-- Today's devotion -->
      <section v-if="devotion" class="space-y-3 rounded-[18px] border border-line bg-white p-5" aria-labelledby="devotion-title">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 id="devotion-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Today’s devotion</h2>
          <LevelPill :level="devotion.learningLevel" />
        </div>
        <p class="break-words text-lg font-bold text-navy-900">
          {{ devotion.reference }}<template v-if="devotion.title"> · {{ devotion.title }}</template>
        </p>
        <p class="text-sm text-muted">
          {{ devotion.submissionCount === 0 ? 'No answers yet.' : `${devotion.submissionCount} ${devotion.submissionCount === 1 ? 'answer' : 'answers'} so far.` }}
        </p>
        <p v-if="devotion.submissionCount > 0" class="text-sm text-muted">
          Today’s devotion is locked: members have already shared, so it can’t be edited or cancelled.
        </p>
        <div class="flex flex-wrap gap-2">
          <RouterLink
            v-if="devotion.submissionCount > 0"
            :to="{ name: 'leader-answers', params: { roomId, devotionId: devotion.id } }"
            :class="btnPrimary"
          >
            Read the answers
          </RouterLink>
          <RouterLink :to="{ name: 'leader-devotions', params: { roomId } }" :class="btnOutline">Devotions</RouterLink>
        </div>
      </section>

      <section v-else class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">No devotion has been posted for today yet.</p>
        <p class="text-sm text-muted">
          Members have nothing to share yet. You can already exempt members for today, and the exemptions stay once
          you post the devotion.
        </p>
        <RouterLink v-if="open" :to="{ name: 'leader-devotion-new', params: { roomId } }" :class="btnPrimary">
          Create devotion
        </RouterLink>
      </section>

      <!-- A disabled member is next in line, so everyone after them is stuck -->
      <div v-if="blockers.length > 0" role="alert" class="space-y-1 rounded-xl border border-gold-line bg-gold-50 px-4 py-3 text-sm text-navy-900">
        <p class="font-semibold">The line is waiting for someone who can’t sign in.</p>
        <p>
          {{ blockerNames }} {{ blockers.length === 1 ? 'is' : 'are' }} next in line, but the account
          {{ blockers.length === 1 ? 'is' : 'are' }} disabled. Exempt them for today, or remove them from the room,
          so everyone after them can continue.
        </p>
      </div>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="board-title">
        <div class="space-y-3 border-b border-line p-4">
          <h2 id="board-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Participation</h2>
          <ProgressBar
            label="Shared today"
            :value="board.submittedCount"
            :max="expectedCount"
            :summary="`${board.submittedCount} of ${expectedCount} members shared today`"
          />
          <p v-if="board.exemptedCount > 0" class="text-sm text-muted">
            {{ board.exemptedCount }} {{ board.exemptedCount === 1 ? 'member is' : 'members are' }} exempt today and not counted.
          </p>
          <p class="text-sm text-muted">
            <strong class="text-navy-900">Exempt for Today</strong> lets a member sit the day out: they can’t submit,
            nobody waits for them, and they aren’t counted as missed. It ends automatically at midnight.
          </p>
        </div>

        <p v-if="order.length === 0" class="p-6 text-center text-sm text-muted">
          This room has no members yet. Add people from the
          <RouterLink :to="{ name: 'leader-room', params: { roomId } }" class="font-bold text-navy-900 underline underline-offset-4">room page</RouterLink>.
        </p>

        <ol v-else aria-label="Today’s participation" class="divide-y divide-line">
          <li v-for="entry in order" :key="entry.memberId" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <ParticipationBadge :number="entry.participationNumber" />
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ entry.fullName }}</p>
              <p class="truncate text-sm text-muted">{{ entry.email }}</p>
              <p v-if="entry.waitingFor" class="text-sm text-muted">
                Waiting for {{ entry.waitingFor.fullName }} (No. {{ entry.waitingFor.participationNumber }})
              </p>
            </div>
            <StatusPill v-if="entry.accountStatus === 'DISABLED'" status="DISABLED" />
            <ParticipationPill v-if="entry.status" :status="entry.status" />
            <ExemptionSwitch
              v-if="entry.status !== 'SUBMITTED'"
              :checked="entry.exempt"
              :label="`Exempt ${entry.fullName} for today`"
              :disabled="!open || busyId !== null"
              @toggle="toggle(entry)"
            />
          </li>
        </ol>
      </section>
    </PageState>

    <ConfirmDialog
      v-model:open="removal.open"
      :title="`Remove ${removal.target?.fullName ?? ''}’s exemption?`"
      :message="removalMessage"
      confirm-label="Remove exemption"
      :busy="removal.busy"
      :error="removal.error"
      @confirm="confirmRemove"
    />
  </div>
</template>
