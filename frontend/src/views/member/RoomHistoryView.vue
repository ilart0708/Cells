<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getRoomHistory } from '../../api/history'
import { ApiError } from '../../api/http'
import { btnOutline } from '../../components/buttonClasses'
import HistoryStatusPill from '../../components/HistoryStatusPill.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import PeriodCard from '../../components/PeriodCard.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'
import { formatDateTime } from '../../utils/format'

const props = defineProps({ roomId: { type: Number, required: true } })

const summary = ref(null)
const missing = ref(false)

// The endpoint returns the room's summary together with one page of lines.
const history = reactive(
  usePagedList(async ({ page, size }) => {
    try {
      const data = await getRoomHistory(props.roomId, { page, size })
      summary.value = data.summary
      missing.value = false
      return data.entries
    } catch (e) {
      // The server answers 404 for a room the member was never in.
      if (e instanceof ApiError && [400, 404].includes(e.status)) missing.value = true
      throw e
    }
  }),
)

watch(
  () => props.roomId,
  () => {
    summary.value = null
    missing.value = false
    history.goTo(0)
  },
)

// Links to the devotion only make sense while the member is still in an active room.
const canOpen = computed(() => Boolean(summary.value?.current) && summary.value?.roomStatus === 'ACTIVE')

function streakText(count) {
  return count === 1 ? '1 devotion in a row' : `${count} devotions in a row`
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <RouterLink
      :to="{ name: 'member-history' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← History
    </RouterLink>

    <div v-if="missing" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room could not be found.</p>
      <p class="mt-1 text-sm text-muted">Go back to your history and choose one of your rooms.</p>
    </div>

    <template v-else>
      <header v-if="summary" class="space-y-2">
        <h1 class="text-2xl font-bold text-navy-900">Room history</h1>
        <div class="flex flex-wrap items-start justify-between gap-2">
          <RoomTitle
            :church-name="summary.churchName"
            :room-name="summary.roomName"
            :leader-name="summary.leaderName"
            :level="2"
          />
          <div class="flex flex-wrap gap-2">
            <StatusPill v-if="summary.roomStatus === 'ARCHIVED'" status="ARCHIVED" />
            <span v-if="!summary.current" class="rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
              No longer in this room
            </span>
          </div>
        </div>
        <p v-if="!summary.current" class="text-sm text-muted">
          You’re no longer in this room. Your history, streaks and EXP stay here.
        </p>
      </header>

      <template v-if="summary">
        <section class="grid gap-4 sm:grid-cols-2" aria-label="Streaks">
          <div class="rounded-[18px] border border-line bg-white p-4">
            <p class="text-xs font-bold uppercase tracking-[2px] text-muted">Current streak</p>
            <p class="mt-1 text-3xl font-extrabold text-navy-900">{{ summary.currentStreak }}</p>
            <p class="text-sm text-muted">{{ streakText(summary.currentStreak) }}</p>
          </div>
          <div class="rounded-[18px] border border-line bg-white p-4">
            <p class="text-xs font-bold uppercase tracking-[2px] text-muted">Best streak</p>
            <p class="mt-1 text-3xl font-extrabold text-navy-900">{{ summary.bestStreak }}</p>
            <p class="text-sm text-muted">{{ streakText(summary.bestStreak) }}</p>
          </div>
        </section>

        <details class="rounded-[18px] border border-line bg-white p-4">
          <summary class="flex min-h-11 cursor-pointer items-center font-semibold text-navy-900">How streaks work</summary>
          <div class="mt-2 space-y-2 text-sm text-muted">
            <p>A streak counts the devotions you shared in a row in this room.</p>
            <p>
              Days when you were waiting for someone else, days you were exempt, and days with no devotion only
              <strong class="text-navy-900">pause</strong> your streak. Today never breaks it either.
            </p>
            <p>Only a day when it was your turn and you didn’t share starts the count again. Your best streak always stays.</p>
          </div>
        </details>

        <section class="grid gap-4 sm:grid-cols-3" aria-label="Totals for this room">
          <PeriodCard title="This week" :totals="summary.thisWeek" />
          <PeriodCard title="This month" :totals="summary.thisMonth" />
          <PeriodCard title="All time" :totals="summary.allTime" />
        </section>
      </template>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Devotions">
        <ListState
          :loading="history.loading"
          :error="history.error"
          :count="history.items.length"
          empty-message="Nothing to show yet. Your history begins the day after you join a room, and each devotion appears here as it happens."
          @retry="history.reload"
        >
          <ul class="divide-y divide-line">
            <li v-for="entry in history.items" :key="entry.devotionId" class="flex flex-wrap items-center gap-3 px-4 py-4">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="text-sm text-muted">{{ formatDay(entry.date) }}</p>
                <p class="break-words font-semibold text-navy-900">
                  {{ entry.reference }}<template v-if="entry.title"> · {{ entry.title }}</template>
                </p>
                <p v-if="entry.status === 'SHARED'" class="text-sm text-muted">
                  +{{ entry.points }} EXP · shared {{ formatDateTime(entry.submittedAt) }}
                </p>
              </div>
              <HistoryStatusPill :status="entry.status" />
              <RouterLink
                v-if="canOpen"
                :to="{ name: 'member-devotion', params: { roomId, devotionId: entry.devotionId } }"
                :class="btnOutline"
              >
                Open<span class="sr-only"> the devotion for {{ formatDay(entry.date) }}</span>
              </RouterLink>
            </li>
          </ul>
        </ListState>

        <PaginationBar
          :page="history.page"
          :total-pages="history.totalPages"
          :total-elements="history.totalElements"
          :loading="history.loading"
          @change="history.goTo"
        />
      </section>
    </template>
  </div>
</template>
