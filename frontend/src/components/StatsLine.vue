<script setup>
import { rateText, todayFraction } from '../utils/stats'

// One line of a monitoring list (a room, a church, a pastor or a leader).
// Slots: title, details (under the title), badges, action.
defineProps({
  today: { type: Object, required: true }, // TodayStats
  last7: { type: Object, required: true }, // PeriodStats
  last30: { type: Object, required: true }, // PeriodStats
})
</script>

<template>
  <li class="flex flex-wrap items-center gap-3 px-4 py-4">
    <div class="min-w-0 flex-1 space-y-1">
      <slot name="title" />
      <p v-if="$slots.details" class="text-sm text-muted"><slot name="details" /></p>
      <p class="text-sm text-muted">
        Last 7 days: {{ rateText(last7.ratePercent) }} · Last 30 days: {{ rateText(last30.ratePercent) }}
      </p>
    </div>

    <slot name="badges" />

    <div class="text-right">
      <template v-if="todayFraction(today)">
        <p class="text-2xl font-extrabold text-navy-900">
          {{ todayFraction(today) }}<span class="sr-only"> shared today</span>
        </p>
        <p class="text-xs text-muted" aria-hidden="true">shared today</p>
      </template>
      <p v-else class="text-sm text-muted">No devotion today</p>
    </div>

    <slot name="action" />
  </li>
</template>
