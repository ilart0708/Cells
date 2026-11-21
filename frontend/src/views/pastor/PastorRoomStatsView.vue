<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getRoomStats } from '../../api/pastorStats'
import { btnOutline } from '../../components/buttonClasses'
import LevelPill from '../../components/LevelPill.vue'
import PageState from '../../components/PageState.vue'
import PeriodPanel from '../../components/PeriodPanel.vue'
import ProgressBar from '../../components/ProgressBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatsHelp from '../../components/StatsHelp.vue'
import TodayPanel from '../../components/TodayPanel.vue'
import { useLiveResource } from '../../composables/useLiveResource'
import { formatDay } from '../../utils/dates'
import { plural } from '../../utils/stats'

const props = defineProps({ roomId: { type: Number, required: true } })

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(() => getRoomStats(props.roomId))

watch(() => props.roomId, () => reload())
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      v-if="state === 'ready'"
      :to="{ name: 'pastor-leader-stats', params: { leaderId: data.room.leaderId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← {{ data.room.leaderName }}
    </RouterLink>
    <RouterLink
      v-else
      :to="{ name: 'pastor-dashboard' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Dashboard
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading statistics…"
      not-found-title="This room could not be found."
      not-found-hint="Go back to your dashboard and choose one of your leaders’ rooms."
      error-text="The statistics could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Room statistics</h1>
          <RoomTitle
            :church-name="data.room.churchName"
            :room-name="data.room.roomName"
            :leader-name="data.room.leaderName"
            :level="2"
          />
          <p class="text-sm text-muted">
            {{ formatDay(data.periods.today) }} · {{ plural(data.room.members, 'member') }}<template
              v-if="data.room.disabledMembers > 0"
            > · {{ plural(data.room.disabledMembers, 'disabled account') }} not counted</template>
          </p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section
        v-if="data.todayDevotion"
        class="space-y-2 rounded-[18px] border border-line bg-white p-5"
        aria-labelledby="devotion-title"
      >
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 id="devotion-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Today’s devotion</h2>
          <LevelPill :level="data.todayDevotion.learningLevel" />
        </div>
        <p class="break-words text-lg font-bold text-navy-900">
          {{ data.todayDevotion.reference }}<template v-if="data.todayDevotion.title"> · {{ data.todayDevotion.title }}</template>
        </p>
      </section>

      <TodayPanel :today="data.room.today" :rooms="1" />

      <div class="grid gap-4 md:grid-cols-2">
        <PeriodPanel
          title="Last 7 days"
          :subtitle="`${formatDay(data.periods.last7From)} to ${formatDay(data.periods.through)}`"
          :stats="data.room.last7"
        />
        <PeriodPanel
          title="Last 30 days"
          :subtitle="`${formatDay(data.periods.last30From)} to ${formatDay(data.periods.through)}`"
          :stats="data.room.last30"
        />
      </div>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="days-title">
        <div class="border-b border-line p-4">
          <h2 id="days-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Day by day</h2>
          <p class="mt-1 text-sm text-muted">The last 30 completed days that had a devotion, newest first.</p>
        </div>

        <p v-if="data.days.length === 0" class="p-6 text-center text-sm text-muted">
          No completed days with a devotion yet.
        </p>

        <ul v-else class="divide-y divide-line">
          <li v-for="day in data.days" :key="day.date" class="space-y-2 px-4 py-4">
            <p class="text-sm text-muted">{{ formatDay(day.date) }}</p>
            <p class="break-words font-semibold text-navy-900">
              {{ day.reference }}<template v-if="day.title"> · {{ day.title }}</template>
            </p>
            <ProgressBar
              label="Shared"
              :value="day.shared"
              :max="day.expected"
              :summary="`${day.shared} of ${day.expected} expected members shared on ${formatDay(day.date)}`"
            />
            <p class="text-sm text-muted">
              {{ day.ratePercent == null ? 'Everyone was exempt, so nobody was expected.' : `${day.ratePercent}% participation` }}
            </p>
          </li>
        </ul>
      </section>

      <StatsHelp />
    </PageState>
  </div>
</template>
