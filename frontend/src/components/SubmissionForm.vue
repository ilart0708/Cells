<script setup>
import { computed, useId } from 'vue'
import { btnPrimary } from './buttonClasses'

const MAX_LENGTH = 2000

const props = defineProps({
  error: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

defineEmits(['submit'])

const model = defineModel({ type: String, default: '' })
const id = useId()
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

    <div class="flex justify-end">
      <button type="submit" :class="btnPrimary" :disabled="saving">
        {{ saving ? 'Sharing…' : 'Share my understanding' }}
      </button>
    </div>
  </form>
</template>
