<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getLeaderBoard } from '../api/leaderParticipation'
import { listRooms } from '../api/rooms'
import { btnOutline, btnPrimary } from '../components/buttonClasses'
import PageState from '../components/PageState.vue'
import ParticipationBadge from '../components/ParticipationBadge.vue'
import ParticipationPill from '../components/ParticipationPill.vue'
import ProgressBar from '../components/ProgressBar.vue'
import RoomTitle from '../components/RoomTitle.vue'
import StatTile from '../components/StatTile.vue'
import { usePolling } from '../composables/usePolling'
import { useAuthStore } from '../stores/auth'

const MAX_ROOMS = 20

const auth = useAuthStore()
const state = ref('loading') // loading | ready | error
const cards = ref([]) // [{ room, board | null }]: board is null when that room could not be checked
let latestRequest = 0

async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (!quiet || cards.value.length === 0) state.value = 'loading'
  try {
    const page = await listRooms({ status: 'ACTIVE', page: 0, size: MAX_ROOMS })
    const boards = await Promise.allSettled(page.content.map((room) => getLeaderBoard(room.id)))
    if (id !== latestRequest) return // a newer request replaced this one
    cards.value = page.content.map((room, i) => ({
      room,
      board: boards[i].status === 'fulfilled' ? boards[i].value : null,
    }))
    state.value = 'ready'
  } catch {
    if (id !== latestRequest) return
    if (!quiet || cards.value.length === 0) state.value = 'error' // a failed quiet reload keeps what is shown
  }
}

onMounted(() => load())
usePolling(() => load({ quiet: true }), 30000)

const known = computed(() => cards.value.filter((card) => card.board))
const totals = computed(() => {
  let members = 0
  let shared = 0
  let expected = 0
  for (const { board } of known.value) {
    members += board.memberCount
    shared += board.submittedCount
    expected += board.memberCount - board.exemptedCount
  }
  return { members, shared, expected, waiting: Math.max(0, expected - shared) }
})

function expectedOf(board) {
  return board.memberCount - board.exemptedCount
}

// Whose turn it is now: the first member the order is waiting on.
function nextUp(board) {
  return board.order.find((entry) => entry.status === 'ELIGIBLE') ?? null
}

function blockers(board) {
  return board.order.filter((entry) => entry.blocking)
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <section class="bg-room-card rounded-[18px] p-5 text-white">
      <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your groups today</p>
      <h1 class="mt-1 text-2xl font-bold">Welcome, {{ auth.user.fullName }}</h1>
      <p class="mt-1 text-sm text-on-navy-soft">See who has shared today and whose turn it is.</p>
    </section>

    <PageState
      :state="state"
      loading-text="Loading your groups…"
      error-text="Your groups could not be loaded."
      @retry="load()"
    >
      <div v-if="cards.length === 0" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">You have no active rooms yet.</p>
        <p class="text-sm text-muted">Create your first room to start your cell group.</p>
        <RouterLink :to="{ name: 'leader-rooms' }" :class="btnPrimary">Go to My Rooms</RouterLink>
      </div>

      <template v-else>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatTile label="Active rooms" :value="cards.length" />
          <StatTile label="Members" :value="totals.members" />
          <StatTile label="Shared today" :value="`${totals.shared} of ${totals.expected}`" />
          <StatTile label="Still to share" :value="totals.waiting" hint="Exempt members are not counted." />
        </div>

        <ul class="space-y-4">
          <li v-for="{ room, board } in cards" :key="room.id" class="space-y-4 rounded-[18px] border border-line bg-white p-4">
            <RoomTitle
              :church-name="room.churchName"
              :room-name="room.name"
              :leader-name="room.leaderName"
              :pastor-name="room.pastorName"
              :church-verified="room.churchVerified"
              :level="2"
            />

            <p v-if="!board" role="alert" class="rounded-xl bg-app-bg px-4 py-3 text-sm font-semibold text-red-700">
              This room’s status could not be loaded right now.
            </p>

            <template v-else>
              <p v-if="board.devotion" class="rounded-xl bg-gold-50 px-4 py-2 text-sm text-navy-900">
                <span class="font-bold text-gold-700">Today:</span>
                {{ board.devotion.reference }}<template v-if="board.devotion.title"> · {{ board.devotion.title }}</template>
              </p>
              <p v-else class="rounded-xl bg-app-bg px-4 py-3 text-sm text-muted">
                No devotion posted yet today.
              </p>

              <ProgressBar
                v-if="board.devotion"
                label="Participation today"
                :value="board.submittedCount"
                :max="expectedOf(board)"
                :summary="`${board.submittedCount} of ${expectedOf(board)} members shared today in ${room.name}`"
              />
              <p v-if="board.devotion && board.exemptedCount > 0" class="text-sm text-muted">
                {{ board.exemptedCount }} {{ board.exemptedCount === 1 ? 'member is' : 'members are' }} exempt today and not counted.
              </p>
              <p v-if="board.devotion && nextUp(board)" class="text-sm font-semibold text-navy-900">
                Their turn now: {{ nextUp(board).fullName }} (No. {{ nextUp(board).participationNumber }})
              </p>
              <p v-if="blockers(board).length > 0" role="status" class="rounded-xl bg-gold-50 px-4 py-3 text-sm text-navy-900">
                <span class="font-bold">The line is stuck.</span>
                {{ blockers(board).map((entry) => entry.fullName).join(', ') }} can’t share (account disabled).
                Open the room and give a day off or remove them.
              </p>

              <ul v-if="board.order.length > 0" :aria-label="`Members of ${room.name}`" class="divide-y divide-line rounded-xl border border-line">
                <li v-for="entry in board.order" :key="entry.memberId" class="flex items-center gap-3 px-3 py-2">
                  <ParticipationBadge v-if="entry.participationNumber" :number="entry.participationNumber" />
                  <p class="min-w-0 flex-1 break-words text-sm font-semibold text-navy-900">{{ entry.fullName }}</p>
                  <ParticipationPill v-if="entry.status" :status="entry.status" />
                </li>
              </ul>
              <p v-else class="text-sm text-muted">No members yet. Invite your first member from the room.</p>
            </template>

            <div class="flex flex-wrap gap-2">
              <RouterLink :to="{ name: 'leader-today', params: { roomId: room.id } }" :class="btnPrimary">
                Today’s board<span class="sr-only"> of {{ room.name }}</span>
              </RouterLink>
              <RouterLink :to="{ name: 'leader-room', params: { roomId: room.id } }" :class="btnOutline">
                Open room<span class="sr-only"> {{ room.name }}</span>
              </RouterLink>
            </div>
          </li>
        </ul>

        <RouterLink :to="{ name: 'leader-rooms' }" :class="btnOutline">All my rooms</RouterLink>
      </template>
    </PageState>
  </div>
</template>
