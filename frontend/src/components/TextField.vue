<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  autocomplete: { type: String, default: undefined },
  maxlength: { type: [Number, String], default: undefined },
  min: { type: String, default: undefined },
  max: { type: String, default: undefined },
  inputmode: { type: String, default: undefined },
})

const model = defineModel({ type: String, default: '' })
const id = useId()

const describedBy = computed(
  () => [props.hint && `${id}-hint`, props.error && `${id}-error`].filter(Boolean).join(' ') || undefined,
)
</script>

<template>
  <div>
    <label :for="id" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
      {{ label }}
    </label>
    <input
      :id="id"
      v-model="model"
      :type="type"
      :readonly="readonly"
      :required="required"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :min="min"
      :max="max"
      :inputmode="inputmode"
      :aria-invalid="error ? 'true' : 'false'"
      :aria-describedby="describedBy"
      :class="[
        'mt-2 block w-full rounded-xl border px-4 py-3.5 text-base outline-none',
        readonly
          ? 'border-line bg-app-bg text-muted'
          : 'border-line-input bg-white text-navy-900 focus:border-gold-500 focus:ring-1 focus:ring-gold-500',
      ]"
    />
    <p v-if="hint" :id="`${id}-hint`" class="mt-1 text-sm text-muted">{{ hint }}</p>
    <p v-if="error" :id="`${id}-error`" class="mt-1 text-sm text-red-700">{{ error }}</p>
  </div>
</template>
