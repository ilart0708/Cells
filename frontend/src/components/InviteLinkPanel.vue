<script setup>
import { ref } from 'vue'
import { formatDate } from '../utils/format'
import { btnPrimary, btnSecondary } from './buttonClasses'

const props = defineProps({
  url: { type: String, required: true },
  email: { type: String, required: true },
  expiresAt: { type: String, required: true },
})

defineEmits(['done'])

const field = ref(null)
const message = ref('')

async function copy() {
  try {
    await navigator.clipboard.writeText(props.url)
    message.value = 'Link copied.'
  } catch {
    field.value?.select()
    message.value = 'Press Ctrl+C to copy the selected link.'
  }
}
</script>

<template>
  <div class="space-y-4">
    <p class="text-sm text-muted">
      Send this link to <strong class="text-navy-900">{{ email }}</strong
      >. It works once and expires on {{ formatDate(expiresAt) }}. For security it is shown only now,
      so copy it before you close this window.
    </p>

    <div>
      <label for="invite-link" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
        Invitation link
      </label>
      <input
        id="invite-link"
        ref="field"
        type="text"
        readonly
        :value="url"
        class="mt-2 block w-full rounded-xl border border-line bg-app-bg px-4 py-3.5 text-sm text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
        @focus="$event.target.select()"
      />
    </div>

    <p v-if="message" role="status" class="text-sm font-semibold text-navy-900">{{ message }}</p>

    <div class="flex justify-end gap-3">
      <button type="button" :class="btnSecondary" @click="copy">Copy link</button>
      <button type="button" :class="btnPrimary" @click="$emit('done')">Done</button>
    </div>
  </div>
</template>
