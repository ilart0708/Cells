<script setup>
import { ref, useId, watch } from 'vue'

defineProps({ title: { type: String, required: true } })

const open = defineModel('open', { type: Boolean, default: false })
const dialog = ref(null)
const titleId = useId()

// Native <dialog>: focus trap, Escape key and focus return come from the browser.
watch(
  open,
  (value) => {
    const el = dialog.value
    if (!el) return
    if (value && !el.open) el.showModal()
    if (!value && el.open) el.close()
  },
  { immediate: true, flush: 'post' },
)
</script>

<template>
  <dialog
    ref="dialog"
    :aria-labelledby="titleId"
    class="m-auto max-h-[90vh] w-[calc(100%-2rem)] max-w-md overflow-y-auto rounded-[18px] border border-line bg-white p-0 text-navy-900 shadow-[0_40px_90px_rgba(0,0,0,.55)] backdrop:bg-navy-950/60"
    @close="open = false"
  >
    <!-- Rendered only while open, so every opening starts from a fresh form. -->
    <div v-if="open" class="p-6">
      <h2 :id="titleId" class="text-xl font-bold text-navy-900">{{ title }}</h2>
      <div class="mt-4">
        <slot />
      </div>
    </div>
  </dialog>
</template>
