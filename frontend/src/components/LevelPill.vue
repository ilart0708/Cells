<script setup>
import { computed } from 'vue'
import { LEVEL_LABEL } from '../constants/devotion'

const props = defineProps({ level: { type: String, required: true } })

const label = computed(() => LEVEL_LABEL[props.level] ?? props.level)
</script>

<template>
  <span class="inline-flex items-center rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">
    {{ label }}
  </span>
</template>
