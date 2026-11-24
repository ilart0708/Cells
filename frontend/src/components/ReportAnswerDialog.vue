<script setup>
import { ref } from 'vue'
import { reportAnswer } from '../api/reports'
import { ApiError } from '../api/http'
import { REPORT_REASONS } from '../constants/reportReasons'
import BaseDialog from './BaseDialog.vue'
import { btnOutline, btnSecondary } from './buttonClasses'

const props = defineProps({
  roomId: { type: Number, required: true },
  submissionId: { type: Number, default: null },
})
const emit = defineEmits(['reported'])
const open = defineModel('open', { type: Boolean, default: false })

const reason = ref('OFFENSIVE')
const note = ref('')
const busy = ref(false)
const error = ref('')

async function send() {
  if (busy.value || props.submissionId === null) return
  busy.value = true
  error.value = ''
  try {
    await reportAnswer(props.roomId, props.submissionId, reason.value, note.value.trim())
    emit('reported', props.submissionId)
    open.value = false
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <BaseDialog v-model:open="open" title="Report this answer">
    <form class="space-y-4" @submit.prevent="send">
      <p class="text-sm text-muted">
        Your leader will review it. Your name is not shown to the leader or to the person who wrote it.
      </p>
      <fieldset class="space-y-1">
        <legend class="text-[13px] font-bold uppercase tracking-[1px] text-muted">Why are you reporting it?</legend>
        <label v-for="item in REPORT_REASONS" :key="item.value" class="flex min-h-11 items-center gap-3 text-sm text-navy-900">
          <input v-model="reason" type="radio" name="report-reason" :value="item.value" class="size-4 accent-navy-900" />
          {{ item.label }}
        </label>
      </fieldset>
      <div>
        <label for="report-note" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
          More detail (optional)
        </label>
        <textarea
          id="report-note"
          v-model="note"
          rows="3"
          maxlength="500"
          class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
        ></textarea>
      </div>
      <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{{ error }}</p>
      <div class="flex justify-end gap-3">
        <button type="button" :class="btnOutline" @click="open = false">Cancel</button>
        <button type="submit" :class="btnSecondary" :disabled="busy">{{ busy ? 'Sending…' : 'Send report' }}</button>
      </div>
    </form>
  </BaseDialog>
</template>
