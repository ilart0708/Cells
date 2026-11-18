<script setup>
import { computed } from 'vue'
import { formatDay } from '../utils/dates'
import { readOnlineUrl } from '../utils/bible'
import { btnOutline } from './buttonClasses'
import LevelPill from './LevelPill.vue'

const props = defineProps({
  reference: { type: String, default: '' },
  title: { type: String, default: '' },
  level: { type: String, default: '' },
  instructions: { type: String, default: '' },
  date: { type: String, default: '' },
  showReadLink: { type: Boolean, default: true },
})

const hasPassage = computed(() => props.reference.trim() !== '')
</script>

<template>
  <article class="space-y-4 rounded-[18px] border border-line bg-white p-5">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <p v-if="date" class="text-xs font-bold uppercase tracking-[2px] text-muted">{{ formatDay(date) }}</p>
      <LevelPill v-if="level" :level="level" />
    </div>

    <h3 v-if="title" class="break-words text-xl font-bold text-navy-900">{{ title }}</h3>

    <!-- Scripture panel from the design guide: gold-50 background, gold border, reference in bold -->
    <div class="rounded-2xl border border-gold-line bg-gold-50 p-4">
      <p class="text-xs font-bold uppercase tracking-[2px] text-gold-700">Bible passage</p>
      <p class="mt-1 break-words text-lg font-bold text-navy-900">
        {{ hasPassage ? reference : 'Choose a book and chapter' }}
      </p>
      <a
        v-if="showReadLink && hasPassage"
        :href="readOnlineUrl(reference)"
        target="_blank"
        rel="noopener noreferrer"
        :class="[btnOutline, 'mt-3']"
      >
        Read it online<span class="sr-only"> (opens in a new tab)</span>
      </a>
    </div>

    <div v-if="instructions">
      <p class="text-[13px] font-bold uppercase tracking-[1px] text-muted">Instructions</p>
      <p class="mt-1 whitespace-pre-line break-words text-sm text-navy-900">{{ instructions }}</p>
    </div>
  </article>
</template>
