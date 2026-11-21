<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  totals: { type: Object, required: true }, // the backend's PeriodTotalsResponse
})

const rateText = computed(() =>
  props.totals.ratePercent == null
    ? 'Nothing to count yet.'
    : `${props.totals.ratePercent}% of the days it was your turn.`,
)
</script>

<template>
  <section class="space-y-3 rounded-[18px] border border-line bg-white p-4">
    <div>
      <h3 class="text-xs font-bold uppercase tracking-[2px] text-muted">{{ title }}</h3>
      <p v-if="subtitle" class="text-sm text-muted">{{ subtitle }}</p>
    </div>

    <p class="text-3xl font-extrabold text-navy-900">
      {{ totals.shared }}<span class="ml-2 text-sm font-semibold text-muted">shared</span>
    </p>
    <p class="text-sm font-semibold text-navy-900">{{ totals.points }} EXP</p>
    <p class="text-sm text-muted">{{ rateText }}</p>

    <dl class="space-y-1 border-t border-line pt-3 text-sm">
      <div class="flex justify-between gap-3">
        <dt class="text-muted">Didn’t share</dt>
        <dd class="font-semibold text-navy-900">{{ totals.missed }}</dd>
      </div>
      <div class="flex justify-between gap-3">
        <dt class="text-muted">Waited on others</dt>
        <dd class="font-semibold text-navy-900">{{ totals.blocked }}</dd>
      </div>
      <div class="flex justify-between gap-3">
        <dt class="text-muted">Exempt</dt>
        <dd class="font-semibold text-navy-900">{{ totals.exempt }}</dd>
      </div>
      <div v-if="totals.pending > 0" class="flex justify-between gap-3">
        <dt class="text-muted">Open today</dt>
        <dd class="font-semibold text-navy-900">{{ totals.pending }}</dd>
      </div>
    </dl>
  </section>
</template>
