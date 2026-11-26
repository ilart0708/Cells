<script setup>
import { computed } from 'vue'

const props = defineProps({
  churchName: { type: String, required: true },
  roomName: { type: String, required: true },
  leaderName: { type: String, required: true },
  level: { type: Number, default: 3 }, // which heading element to use: 1 to 6
  tone: { type: String, default: 'light' }, // 'light' on white cards, 'dark' on the navy room card
  // Filled only for a church a leader registered by themselves. The pastor is typed text, not an account.
  pastorName: { type: String, default: '' },
  churchVerified: { type: Boolean, default: true },
})

const heading = computed(() => `h${props.level}`)
const dark = computed(() => props.tone === 'dark')
</script>

<template>
  <div class="min-w-0">
    <component
      :is="heading"
      :class="[
        'break-words font-bold',
        level === 1 ? 'text-2xl' : 'text-base',
        dark ? 'text-white' : 'text-navy-900',
      ]"
    >
      {{ churchName }} – {{ roomName }}
    </component>
    <p :class="['text-sm', dark ? 'text-on-navy-soft' : 'text-muted']">Led by {{ leaderName }}</p>
    <p
      v-if="pastorName && !churchVerified"
      :class="['text-sm', dark ? 'text-on-navy-soft' : 'text-muted']"
    >
      Pastor: {{ pastorName }} · not verified
    </p>
  </div>
</template>
