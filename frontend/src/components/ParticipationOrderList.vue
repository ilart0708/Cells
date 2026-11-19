<script setup>
import ParticipationBadge from './ParticipationBadge.vue'
import ParticipationPill from './ParticipationPill.vue'

// order: the backend's BoardEntryResponse list (number, name, you, status). status is null when
// nothing is posted today, and then no pill is shown.
defineProps({ order: { type: Array, required: true } })
</script>

<template>
  <ol aria-label="Participation order" class="divide-y divide-line">
    <li
      v-for="entry in order"
      :key="entry.participationNumber"
      :class="['flex flex-wrap items-center gap-3 px-4 py-3', entry.you ? 'bg-gold-50' : '']"
    >
      <ParticipationBadge :number="entry.participationNumber" />
      <p class="min-w-0 flex-1 truncate font-semibold text-navy-900">{{ entry.fullName }}</p>
      <span v-if="entry.you" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">You</span>
      <ParticipationPill v-if="entry.status" :status="entry.status" :you="entry.you" />
    </li>
  </ol>
</template>
