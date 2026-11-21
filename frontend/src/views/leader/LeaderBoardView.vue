<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { getLeaderRoomBoard } from '../../api/leaderBoard'
import { getRoom } from '../../api/rooms'
import { btnOutline } from '../../components/buttonClasses'
import GrowthBoardList from '../../components/GrowthBoardList.vue'
import PageState from '../../components/PageState.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import { usePolling } from '../../composables/usePolling'
import { formatMonth } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const board = ref(null)
const refreshing = ref(false)
const announcement = ref('') // read out by screen readers only
let latestRequest = 0

const entries = computed(() => board.value?.entries ?? [])

const daysLeftText = computed(() => {
  const days = board.value?.daysLeftInMonth ?? 0
  return `${days} ${days === 1 ? 'day' : 'days'} left this month. Everyone starts fresh on the 1st.`
})


async function loadAll() {
  const id = ++latestRequest
  state.value = 'loading'
  board.value = null
  try {
    const [roomData, boardData] = await Promise.all([getRoom(props.roomId), getLeaderRoomBoard(props.roomId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData
    board.value = boardData
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

async function fetchBoard(mode) {
  const id = ++latestRequest
  if (mode === 'manual') refreshing.value = true
  try {
    const data = await getLeaderRoomBoard(props.roomId)
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

usePolling(() => {
  if (state.value === 'ready') fetchBoard('poll')
}, 60000)

onMounted(loadAll)
watch(() => props.roomId, loadAll)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'leader-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Room
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading Growing Together…"
      not-found-title="This room could not be found."
      not-found-hint="Go back to My Rooms and choose one of your rooms."
      error-text="Growing Together could not be loaded."
      @retry="loadAll"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Growing Together</h1>
          <div class="flex flex-wrap items-start gap-2">
            <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" :level="2" />
            <StatusPill v-if="room.status === 'ARCHIVED'" status="ARCHIVED" />
          </div>
          <p class="text-sm text-muted">{{ formatMonth(board.monthStart) }} · {{ daysLeftText }}</p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="fetchBoard('manual')">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section class="rounded-[18px] border border-gold-line bg-gold-50 p-5">
        <p class="text-xs font-bold uppercase tracking-[2px] text-gold-700">Not a competition</p>
        <p class="mt-2 text-base font-semibold text-navy-900">{{ board.message }}</p>
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="board-title">
        <div class="border-b border-line p-4">
          <h2 id="board-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">This month</h2>
          <p class="mt-1 text-sm text-muted">
            Positions appear once someone has shared this month. Members choose for themselves whether they appear on
            this board, so it may not list everyone in your room. Use the <strong class="text-navy-900">Today</strong>
            page for full participation.
          </p>
        </div>

        <p v-if="entries.length === 0" class="p-6 text-center text-sm text-muted">No one is shown on this board yet.</p>
        <GrowthBoardList v-else :entries="entries" />
      </section>

      <p class="text-sm text-muted">Only the members and the leader of this room can see this board.</p>
    </PageState>
  </div>
</template>
