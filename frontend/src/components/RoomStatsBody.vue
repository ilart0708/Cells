<script setup>
import { formatDay } from '../utils/dates'
import LevelPill from './LevelPill.vue'
import PeriodPanel from './PeriodPanel.vue'
import ProgressBar from './ProgressBar.vue'
import StatsHelp from './StatsHelp.vue'
import TodayPanel from './TodayPanel.vue'

defineProps({
  data: { type: Object, required: true }, // RoomStatsDetailResponse
  audience: { type: String, default: 'admin' },
})
</script>

<template>
  <div class="space-y-6">
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

    <StatsHelp :audience="audience" />
  </div>
</template>
