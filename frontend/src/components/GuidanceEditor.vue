<script setup>
import { computed, ref, useId, watch } from 'vue'
import { ApiError } from '../api/http'
import { formatDateTime } from '../utils/format'
import { isEdited } from '../utils/guidance'
import { btnPrimary } from './buttonClasses'

const MAX_LENGTH = 4000

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  note: { type: Object, default: null }, // the saved note, or null until the leader writes one
  disabled: { type: Boolean, default: false }, // read-only (archived room, or nobody has answered yet)
  save: { type: Function, required: true }, // (content) => Promise<note>
})

const emit = defineEmits(['saved', 'changed'])
const dirty = defineModel('dirty', { type: Boolean, default: false })

const id = useId()
const draft = ref(props.note?.content ?? '')
const saving = ref(false)
const error = ref('')
const notice = ref('')

const isDirty = computed(() => draft.value.trim() !== (props.note?.content ?? '').trim())
watch(isDirty, (value) => (dirty.value = value), { immediate: true })

// A refreshed note replaces the text only when the leader has no unsaved changes of their own.
watch(
  () => props.note,
  (note) => {
    if (!isDirty.value) draft.value = note?.content ?? ''
  },
)

watch(draft, () => {
  notice.value = ''
})

const describedBy = computed(() => (error.value ? `${id}-hint ${id}-error` : `${id}-hint`))

async function submit() {
  if (saving.value || props.disabled) return
  error.value = ''
  notice.value = ''

  const text = draft.value.trim()
  if (!text) {
    error.value = 'Please write something first.'
    return
  }

  saving.value = true
  try {
    const saved = await props.save(text)
    notice.value = props.note ? `${props.title} updated.` : `${props.title} saved.`
    emit('saved', saved)
  } catch (e) {
    if (e instanceof ApiError) {
      error.value = e.fieldErrors.content || e.message
      // The room or the devotion changed under us: let the page show what is true now.
      if ([404, 422].includes(e.status)) emit('changed')
    } else {
      error.value = 'Something went wrong. Please try again.'
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="space-y-3 rounded-[18px] border border-line bg-white p-5" novalidate @submit.prevent="submit">
    <div>
      <label :for="id" class="block text-base font-bold text-navy-900">{{ title }}</label>
      <p :id="`${id}-hint`" class="mt-1 text-sm text-muted">{{ description }}</p>
    </div>

    <textarea
      :id="id"
      v-model="draft"
      rows="8"
      :maxlength="MAX_LENGTH"
      :readonly="disabled"
      :aria-invalid="error ? 'true' : 'false'"
      :aria-describedby="describedBy"
      :class="[
        'block w-full rounded-xl border border-line-input px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500',
        disabled ? 'bg-app-bg text-muted' : 'bg-white',
      ]"
    ></textarea>

    <p v-if="note" class="text-sm text-muted">
      Written {{ formatDateTime(note.createdAt)
      }}<template v-if="isEdited(note)"> · Edited {{ formatDateTime(note.updatedAt) }}</template>
    </p>
    <p class="text-sm text-muted">{{ draft.length }} / {{ MAX_LENGTH }}</p>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>
    <p v-if="error" :id="`${id}-error`" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
      {{ error }}
    </p>

    <div v-if="!disabled" class="flex justify-end">
      <button type="submit" :class="btnPrimary" :disabled="saving || !isDirty">
        {{ saving ? 'Saving…' : note ? 'Save changes' : 'Save' }}
      </button>
    </div>
  </form>
</template>
