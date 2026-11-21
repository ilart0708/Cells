<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getBoard, saveBoardSetting } from '../../api/board'
import { ApiError } from '../../api/http'
import { getMyRoom } from '../../api/memberRooms'
import { btnOutline } from '../../components/buttonClasses'
import GrowthLevelPill from '../../components/GrowthLevelPill.vue'
import PageState from '../../components/PageState.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import ToggleSwitch from '../../components/ToggleSwitch.vue'
import { usePolling } from '../../composables/usePolling'
import { formatMonth } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const board = ref(null)
const refreshing = ref(false)
const saving = ref(false)
const notice = ref('')
const problem = ref('')
const announcement = ref('') // read out by screen readers only
let latestRequest = 0

const entries = computed(() => board.value?.entries ?? [])

const daysLeftText = computed(() => {
  const days = board.value?.daysLeftInMonth ?? 0
  return `${days} ${days === 1 ? 'day' : 'days'} left this month. Everyone starts fresh on the 1st.`
})

// ---------- loading ----------
async function loadAll() {
  const id = ++latestRequest
  state.value = 'loading'
  board.value = null
  notice.value = ''
  problem.value = ''
  try {
    const [roomData, boardData] = await Promise.all([getMyRoom(props.roomId), getBoard(props.roomId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData.room
    board.value = boardData
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    // The server answers 404 when the room is archived or the member was removed from it.
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// mode: 'manual' (the Refresh button), 'quiet' (after an action) or 'poll' (silent, on a timer)
async function fetchBoard(mode) {
  const id = ++latestRequest
  if (mode === 'manual') refreshing.value = true
  try {
    const data = await getBoard(props.roomId)
    if (id !== latestRequest) return
    board.value = data
    if (mode === 'manual') announcement.value = 'Updated.'
  } catch (e) {
    if (id !== latestRequest) return
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (mode === 'manual') announcement.value = 'Could not refresh. Please try again.'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

// ---------- the member's choice ----------
async function toggleShown() {
  if (saving.value || !board.value) return
  saving.value = true
  problem.value = ''
  const show = !board.value.iAmShown
  try {
    await saveBoardSetting(show)
    notice.value = show
      ? 'You are shown on your rooms’ boards again.'
      : 'You are hidden from your rooms’ boards. Your EXP, level and history are unchanged.'
    announcement.value = notice.value
    await fetchBoard('quiet')
  } catch (e) {
    problem.value = e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
  } finally {
    saving.value = false
  }
}

// Members share during the day, so the board keeps itself current while the tab is visible.
usePolling(() => {
  if (state.value !== 'ready' || saving.value) return
  fetchBoard('poll')
}, 60000)

onMounted(loadAll)
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
      loading-text="Loading Growing Together…"
      not-found-title="This room could not be found."
      not-found-hint="You may no longer be in this room, or it may have been archived. Your dashboard lists the rooms you belong to."
      error-text="Growing Together could not be loaded."
      @retry="loadAll"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Growing Together</h1>
          <RoomTitle
            :church-name="room.churchName"
            :room-name="room.roomName"
            :leader-name="room.leaderName"
            :level="2"
          />
          <p class="text-sm text-muted">{{ formatMonth(board.monthStart) }} · {{ daysLeftText }}</p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="fetchBoard('manual')">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <!-- The spirit of the board, in the server's words so every screen says the same thing -->
      <section class="rounded-[18px] border border-gold-line bg-gold-50 p-5">
        <p class="text-xs font-bold uppercase tracking-[2px] text-gold-700">Not a competition</p>
        <p class="mt-2 text-base font-semibold text-navy-900">{{ board.message }}</p>
      </section>

      <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
        {{ notice }}
      </p>
      <p v-if="problem" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
        {{ problem }}
      </p>


<section
  class="space-y-3 rounded-[18px] border border-line bg-white p-4"
  aria-labelledby="visibility-title"
>
  <h2
    id="visibility-title"
    class="text-xs font-bold uppercase tracking-[2px] text-muted"
  >
    Your choice
  </h2>

  <!-- Toggle row -->
  <div class="flex w-full min-w-0 items-center justify-between gap-3">
    <ToggleSwitch
      :checked="board.iAmShown"
      label="Show me on my rooms’ boards"
      :disabled="saving"
      @toggle="toggleShown"
    />
  </div>

  <!-- Description -->
  <p class="text-sm leading-relaxed text-muted">
    This applies to every room you are in. Hiding yourself never changes
    your EXP, level, streak or history.
  </p>

  <!-- Hidden status -->
  <p
    v-if="!board.iAmShown"
    class="text-sm font-semibold leading-relaxed text-navy-900"
  >
    You’re hidden, so your name isn’t on the board. You can still see everyone
    who is shown.
  </p>
</section>


      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="board-title">
        <div class="border-b border-line p-4">
          <h2 id="board-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">This month</h2>
          <p class="mt-1 text-sm text-muted">
            Positions appear once someone has shared this month. Your level and streak stay with you from month to month.
          </p>
        </div>

        <p v-if="entries.length === 0" class="p-6 text-center text-sm text-muted">
          No one is shown on this board yet.
        </p>

        <ol v-else aria-label="Growing Together this month" class="divide-y divide-line">
          <li
            v-for="(entry, index) in entries"
            :key="`${entry.fullName}-${index}`"
            :class="['flex flex-wrap items-center gap-3 px-4 py-3', entry.you ? 'bg-gold-50' : '']"
          >
            <span
              v-if="entry.position"
              class="flex size-8 shrink-0 items-center justify-center rounded-full bg-navy-900 text-sm font-bold text-gold-500"
            >
              <span class="sr-only">Position </span>{{ entry.position }}
            </span>
            <span
              v-else
              class="flex size-8 shrink-0 items-center justify-center rounded-full border border-line-input text-sm font-bold text-muted"
            >
              <span class="sr-only">No position yet</span><span aria-hidden="true">–</span>
            </span>

            <div class="min-w-0 flex-1 space-y-1">
              <p class="truncate font-semibold text-navy-900">{{ entry.fullName }}</p>
              <p class="text-sm text-muted">
                {{ entry.pointsThisMonth }} EXP this month · {{ entry.sharedThisMonth }} shared · streak
                {{ entry.currentStreak }}
              </p>
            </div>

            <span v-if="entry.you" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">You</span>
            <GrowthLevelPill :level="entry.level" />
          </li>
        </ol>
      </section>

      <p class="text-sm text-muted">Only the members and the leader of this room can see this board.</p>
    </PageState>
  </div>
</template>
