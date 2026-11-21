<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getHistorySummary } from '../../api/history'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import PageState from '../../components/PageState.vue'
import PeriodCard from '../../components/PeriodCard.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import { formatDay } from '../../utils/dates'

const state = ref('loading') // loading | ready | error
const data = ref(null)

async function load() {
  state.value = 'loading'
  try {
    data.value = await getHistorySummary()
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

onMounted(load)

function streakText(count) {
  return count === 1 ? '1 devotion in a row' : `${count} devotions in a row`
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <header>
      <h1 class="text-2xl font-bold text-navy-900">Participation history</h1>
      <p class="mt-1 text-sm text-muted">
        Your journey of sharing, day by day. It’s here to encourage you, not to compare you with anyone.
      </p>
    </header>

    <PageState
      :state="state"
      loading-text="Loading your history…"
      error-text="Your history could not be loaded."
      @retry="load"
    >
      <section aria-label="Totals">
        <div class="grid gap-4 sm:grid-cols-3">
          <PeriodCard title="This week" :subtitle="`since ${formatDay(data.weekStart)}`" :totals="data.thisWeek" />
          <PeriodCard title="This month" :subtitle="`since ${formatDay(data.monthStart)}`" :totals="data.thisMonth" />
          <PeriodCard title="All time" :totals="data.allTime" />
        </div>
        <p class="mt-3 text-sm text-muted">
          Only the days it was your turn are counted. Exempt days, days you waited for someone else, and days with no
          devotion are never counted against you.
        </p>
      </section>

      <section aria-labelledby="rooms-title" class="space-y-3">
        <h2 id="rooms-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Your rooms</h2>

        <p v-if="data.rooms.length === 0" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
          Your history begins the day after you join a cell group.
        </p>

        <ul v-else class="space-y-4">
          <li v-for="room in data.rooms" :key="room.roomId" class="space-y-3 rounded-[18px] border border-line bg-white p-4">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <RoomTitle :church-name="room.churchName" :room-name="room.roomName" :leader-name="room.leaderName" :level="3" />
              <div class="flex flex-wrap gap-2">
                <StatusPill v-if="room.roomStatus === 'ARCHIVED'" status="ARCHIVED" />
                <span v-if="!room.current" class="rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
                  No longer in this room
                </span>
              </div>
            </div>

            <dl class="grid gap-3 sm:grid-cols-2">
              <div>
                <dt class="text-xs font-bold uppercase tracking-[2px] text-muted">Current streak</dt>
                <dd class="mt-1 text-sm font-semibold text-navy-900">{{ streakText(room.currentStreak) }}</dd>
              </div>
              <div>
                <dt class="text-xs font-bold uppercase tracking-[2px] text-muted">Best streak</dt>
                <dd class="mt-1 text-sm font-semibold text-navy-900">{{ streakText(room.bestStreak) }}</dd>
              </div>
            </dl>

            <div class="flex flex-wrap gap-2">
              <RouterLink :to="{ name: 'member-history-room', params: { roomId: room.roomId } }" :class="btnPrimary">
                View history<span class="sr-only"> of {{ room.roomName }}</span>
              </RouterLink>
              <RouterLink
                v-if="room.current && room.roomStatus === 'ACTIVE'"
                :to="{ name: 'member-board', params: { roomId: room.roomId } }"
                :class="btnOutline"
              >
                Growing Together<span class="sr-only"> in {{ room.roomName }}</span>
              </RouterLink>
            </div>
          </li>
        </ul>
      </section>
    </PageState>
  </div>
</template>
