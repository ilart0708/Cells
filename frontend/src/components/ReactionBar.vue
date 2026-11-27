<script setup>
import { computed, ref, watch } from 'vue'
import { ApiError } from '../api/http'
import { clearReaction, reactToAnswer } from '../api/reactions'
import { REACTIONS } from '../constants/reactions'

const props = defineProps({
  role: { type: String, required: true }, // 'member' | 'leader'
  roomId: { type: Number, required: true },
  submissionId: { type: Number, required: true },
  summary: { type: Object, default: null }, // { counts, mine } from the answers list
  // My own answer: counts are shown but I cannot react to it.
  readonly: { type: Boolean, default: false },
})

const current = ref(props.summary ?? { counts: {}, mine: null })
const busy = ref(false)
const message = ref('')

// A reload of the list brings fresh counts.
watch(() => props.summary, (value) => {
  if (value) current.value = value
})

const total = computed(() => REACTIONS.reduce((sum, r) => sum + (current.value.counts?.[r.type] ?? 0), 0))
const countOf = (type) => current.value.counts?.[type] ?? 0

async function choose(type) {
  if (busy.value) return
  busy.value = true
  message.value = ''
  try {
    // Tapping my own choice again takes it back.
    current.value = current.value.mine === type
      ? await clearReaction(props.role, props.roomId, props.submissionId)
      : await reactToAnswer(props.role, props.roomId, props.submissionId, type)
  } catch (e) {
    message.value = e instanceof ApiError ? e.message : 'The reaction could not be saved. Please try again.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div v-if="!readonly || total > 0">
    <ul v-if="readonly" class="flex flex-wrap gap-2" aria-label="Reactions to your answer">
      <template v-for="r in REACTIONS" :key="r.type">
        <li v-if="countOf(r.type) > 0" class="inline-flex min-h-8 items-center gap-1.5 rounded-full border border-line bg-white px-3 text-sm font-semibold text-navy-900">
          <span aria-hidden="true">{{ r.icon }}</span>{{ r.label }} · {{ countOf(r.type) }}
        </li>
      </template>
    </ul>
    <div v-else role="group" aria-label="React to this answer" class="flex flex-wrap gap-2">
      <button
        v-for="r in REACTIONS"
        :key="r.type"
        type="button"
        :aria-pressed="current.mine === r.type"
        :disabled="busy"
        :class="[
          'inline-flex min-h-11 items-center gap-1.5 rounded-full border px-4 text-sm font-semibold text-navy-900 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900 disabled:opacity-60',
          current.mine === r.type ? 'border-gold-500 bg-gold-100' : 'border-line bg-white hover:bg-app-bg',
        ]"
        @click="choose(r.type)"
      >
        <span aria-hidden="true">{{ r.icon }}</span>{{ r.label }}<template v-if="countOf(r.type) > 0"> · {{ countOf(r.type) }}</template>
      </button>
    </div>
    <p v-if="message" role="alert" class="mt-2 text-sm font-semibold text-red-700">{{ message }}</p>
  </div>
</template>
