<script setup>
import { computed } from 'vue'
import { segments } from '../utils/highlights'

const props = defineProps({
  content: { type: String, default: '' },
  takeaway: { type: String, default: '' },
  highlights: { type: Array, default: () => [] },
  removed: { type: Boolean, default: false },
})

const pieces = computed(() => (props.removed ? [{ text: props.content, marked: false }] : segments(props.content, props.highlights)))
</script>

<template>
  <div class="space-y-2">
    <div
      v-if="takeaway && !removed"
      class="rounded-[14px] border border-gold-line bg-gold-50 px-4 py-3"
    >
      <p class="text-xs font-bold uppercase tracking-[1px] text-gold-700">Key takeaway</p>
      <p class="mt-1 break-words text-sm font-semibold text-navy-900">{{ takeaway }}</p>
    </div>
    <p :class="['whitespace-pre-line break-words text-sm', removed ? 'italic text-muted' : 'text-navy-900']">
      <template v-for="(piece, index) in pieces" :key="index"><mark
          v-if="piece.marked"
          class="rounded bg-gold-100 px-0.5 text-navy-900 underline decoration-gold-500 decoration-2 underline-offset-2"
        >{{ piece.text }}</mark><template v-else>{{ piece.text }}</template></template>
    </p>
  </div>
</template>
