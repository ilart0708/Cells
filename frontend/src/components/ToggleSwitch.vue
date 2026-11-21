
<script setup>
// A reusable accessible switch.
// Screen readers receive the checked state through aria-checked.
defineProps({
  checked: { type: Boolean, required: true },
  label: { type: String, required: true },
  disabled: { type: Boolean, default: false },
})

defineEmits(['toggle'])
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="checked"
    :aria-label="label"
    :disabled="disabled"
    class="flex w-full min-w-0 min-h-11 items-center gap-3 rounded-xl px-2 py-2 text-left
           focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900
           disabled:cursor-not-allowed disabled:opacity-60"
    @click="$emit('toggle')"
  >
    <!-- Switch track -->
    <span
      aria-hidden="true"
      :class="[
        'relative block h-6 w-11 shrink-0 rounded-full transition-colors',
        checked ? 'bg-navy-900' : 'bg-line-input'
      ]"
    >
      <!-- Switch knob -->
      <span
        :class="[
          'absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition-transform motion-reduce:transition-none',
          checked ? 'translate-x-5' : 'translate-x-0'
        ]"
      ></span>
    </span>

    <!-- Label -->
    <span class="min-w-0 flex-1 break-words text-sm font-semibold text-navy-900">
      {{ label }}
    </span>

    <!-- State -->
    <span
      aria-hidden="true"
      class="shrink-0 whitespace-nowrap text-xs font-semibold text-muted"
    >
      {{ checked ? 'On' : 'Off' }}
    </span>
  </button>
</template>
