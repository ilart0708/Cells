<script setup>
import { computed, ref, useId, watch } from 'vue'
import { addHighlight, MAX_HIGHLIGHTS, segments } from '../utils/highlights'
import { btnOutline, btnPrimary } from './buttonClasses'

const MAX_LENGTH = 2000
const MAX_TAKEAWAY = 200

const props = defineProps({
  error: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

defineEmits(['submit'])

const model = defineModel({ type: String, default: '' })
const takeaway = defineModel('takeaway', { type: String, default: '' })
const highlights = defineModel('highlights', { type: Array, default: () => [] })
const id = useId()
const textarea = ref(null)
const markNote = ref('')

// Marked passages are positions in the text, so any change to the text clears them.
watch(model, () => {
  if (highlights.value.length > 0) {
    highlights.value = []
    markNote.value = 'Your text changed, so the highlights were cleared. Mark them again when you are done writing.'
  }
})

function markSelection() {
  const box = textarea.value
  if (!box) return
  const result = addHighlight(model.value, highlights.value, box.selectionStart, box.selectionEnd)
  if (result.error) {
    markNote.value = result.error
    return
  }
  highlights.value = result.ranges
  markNote.value = ''
}

function removeHighlight(index) {
  highlights.value = highlights.value.filter((_, i) => i !== index)
  markNote.value = ''
}

const pieces = computed(() => segments(model.value, highlights.value))
const describedBy = computed(() => (props.error ? `${id}-hint ${id}-error` : `${id}-hint`))
</script>

<template>
  <form class="space-y-4 rounded-[18px] border border-gold-line bg-white p-5" novalidate @submit.prevent="$emit('submit')">
    <div>
      <p class="text-xs font-bold uppercase tracking-[2px] text-gold-700">Your turn</p>
      <h2 class="mt-1 text-lg font-bold text-navy-900">Share your understanding</h2>
    </div>

    <div class="text-sm text-muted">
      <p>Write in your own words. You might think about:</p>
      <ul class="mt-1 list-disc space-y-1 pl-5">
        <li>What does this passage teach you?</li>
        <li>What lessons did you learn from it?</li>
        <li>How can you apply it in your daily life?</li>
      </ul>
    </div>

    <div>
      <label :for="id" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
        Your understanding
      </label>
      <textarea
        :id="id"
        ref="textarea"
        v-model="model"
        rows="8"
        :maxlength="MAX_LENGTH"
        :aria-invalid="error ? 'true' : 'false'"
        :aria-describedby="describedBy"
        class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
      ></textarea>
      <p :id="`${id}-hint`" class="mt-1 text-sm text-muted">
        {{ model.length }} / {{ MAX_LENGTH }}. Once you share it, your answer can’t be changed or deleted.
      </p>
      <p v-if="error" :id="`${id}-error`" class="mt-1 text-sm text-red-700">{{ error }}</p>
    </div>

    <section aria-labelledby="highlight-heading" class="space-y-2 rounded-xl border border-line bg-app-bg p-4">
      <h3 id="highlight-heading" class="text-[13px] font-bold uppercase tracking-[1px] text-muted">
        Highlight key sentences <span class="font-normal normal-case tracking-normal">(optional)</span>
      </h3>
      <p class="text-sm text-muted">
        Select the words that matter most in your answer, then tap Highlight. Up to {{ MAX_HIGHLIGHTS }}.
      </p>
      <button type="button" :class="btnOutline" @click="markSelection">Highlight selected text</button>
      <p v-if="markNote" role="status" class="text-sm font-semibold text-navy-900">{{ markNote }}</p>
      <template v-if="highlights.length > 0">
        <p class="whitespace-pre-line break-words rounded-xl border border-line bg-white p-3 text-sm text-navy-900" aria-label="Preview with your highlights">
          <template v-for="(piece, index) in pieces" :key="index"><mark
              v-if="piece.marked"
              class="rounded bg-gold-100 px-0.5 text-navy-900 underline decoration-gold-500 decoration-2 underline-offset-2"
            >{{ piece.text }}</mark><template v-else>{{ piece.text }}</template></template>
        </p>
        <ul class="space-y-1">
          <li v-for="(range, index) in highlights" :key="`${range.start}-${range.end}`" class="flex items-center gap-2 text-sm text-navy-900">
            <span class="min-w-0 flex-1 truncate">“{{ model.slice(range.start, range.end) }}”</span>
            <button type="button" :class="btnOutline" @click="removeHighlight(index)">
              Remove<span class="sr-only"> highlight {{ index + 1 }}</span>
            </button>
          </li>
        </ul>
      </template>
    </section>

    <div>
      <label :for="`${id}-takeaway`" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
        Key takeaway <span class="font-normal normal-case tracking-normal">(optional)</span>
      </label>
      <input
        :id="`${id}-takeaway`"
        v-model="takeaway"
        type="text"
        :maxlength="MAX_TAKEAWAY"
        :aria-describedby="`${id}-takeaway-hint`"
        class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
      />
      <p :id="`${id}-takeaway-hint`" class="mt-1 text-sm text-muted">
        One line you want everyone to remember. {{ takeaway.length }} / {{ MAX_TAKEAWAY }}.
      </p>
    </div>

    <div class="flex justify-end">
      <button type="submit" :class="btnPrimary" :disabled="saving">
        {{ saving ? 'Sharing…' : 'Share my understanding' }}
      </button>
    </div>
  </form>
</template>
