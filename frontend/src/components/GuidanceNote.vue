<script setup>
import { computed } from 'vue'
import { formatDateTime } from '../utils/format'
import { isEdited } from '../utils/guidance'

const props = defineProps({
  kind: { type: String, required: true }, // SUMMARY | REFLECTION
  note: { type: Object, required: true }, // { content, createdAt, updatedAt }
})

const title = computed(() => (props.kind === 'SUMMARY' ? 'Group summary' : 'Leader’s reflection'))
</script>

<template>
  <article class="space-y-2 rounded-[18px] border border-gold-line bg-gold-50 p-5">
    <h3 class="text-xs font-bold uppercase tracking-[2px] text-gold-700">{{ title }}</h3>
    <p class="text-sm text-muted">
      Written {{ formatDateTime(note.createdAt)
      }}<template v-if="isEdited(note)"> · Edited {{ formatDateTime(note.updatedAt) }}</template>
    </p>
    <!-- Plain text with line breaks kept, never through v-html -->
    <p class="whitespace-pre-line break-words text-sm text-navy-900">{{ note.content }}</p>
  </article>
</template>
