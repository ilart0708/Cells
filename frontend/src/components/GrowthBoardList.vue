<script setup>
import GrowthLevelPill from './GrowthLevelPill.vue'

// entries: the backend's GrowthEntryResponse list. position is null for members with no EXP this month.
defineProps({ entries: { type: Array, required: true } })
</script>

<template>
  <ol aria-label="Growing Together this month" class="divide-y divide-line">
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
          {{ entry.pointsThisMonth }} EXP this month · {{ entry.sharedThisMonth }} shared · streak {{ entry.currentStreak }}
        </p>
      </div>

      <span v-if="entry.you" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">You</span>
      <GrowthLevelPill :level="entry.level" />
    </li>
  </ol>
</template>
