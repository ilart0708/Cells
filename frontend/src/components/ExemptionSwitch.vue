
<script setup>
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
    class="inline-flex w-fit min-w-max flex-nowrap shrink-0 items-center gap-3 rounded-xl px-2 py-2
           focus-visible:outline-2 focus-visible:outline-offset-2
           focus-visible:outline-navy-900
           disabled:cursor-not-allowed disabled:opacity-60"
    @click="$emit('toggle')"
  >
    <!-- Toggle track -->
    <span
      aria-hidden="true"
      :class="[
        'relative block h-6 w-11 shrink-0 rounded-full transition-colors',
        checked ? 'bg-navy-900' : 'bg-line-input'
      ]"
    >
      <!-- Toggle knob -->
      <span
        :class="[
          'absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition-transform motion-reduce:transition-none',
          checked ? 'translate-x-5' : 'translate-x-0'
        ]"
      ></span>
    </span>

    <!-- Label -->
    <span class="whitespace-nowrap text-left text-sm font-semibold text-navy-900">
      Exempt for Today:
      <span aria-hidden="true">{{ checked ? 'On' : 'Off' }}</span>
    </span>
  </button>
</template>
