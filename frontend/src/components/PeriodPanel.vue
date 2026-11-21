<script setup>
import { computed } from 'vue'
import ProgressBar from './ProgressBar.vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  stats: { type: Object, required: true }, // the backend's PeriodStats
  unit: { type: String, default: 'day' }, // 'day' for one room, 'room-day' when several rooms are added up
})

const postedText = computed(() => {
  const { devotionDays, possibleDays } = props.stats
  if (possibleDays === 0) return 'No completed days to count yet.'
  const word = possibleDays === 1 ? props.unit : `${props.unit}s`
  return `A devotion was posted on ${devotionDays} of ${possibleDays} ${word}.`
})
</script>

<template>
  <section class="space-y-3 rounded-[18px] border border-line bg-white p-5">
    <div>
      <h2 class="text-xs font-bold uppercase tracking-[2px] text-muted">{{ title }}</h2>
      <p v-if="subtitle" class="text-sm text-muted">{{ subtitle }}</p>
    </div>

    <template v-if="stats.expected > 0">
      <p class="text-3xl font-extrabold text-navy-900">
        {{ stats.ratePercent }}%<span class="ml-2 text-sm font-semibold text-muted">participation</span>
      </p>
      <ProgressBar
        label="Shared"
        :value="stats.shared"
        :max="stats.expected"
        :summary="`${stats.shared} of ${stats.expected} expected shares in ${title.toLowerCase()}`"
      />
    </template>
    <p v-else class="text-sm text-muted">Nothing to count yet.</p>

    <p class="text-sm text-muted">{{ postedText }}</p>
  </section>
</template>
