<script setup>
// A round badge for a member's level. The drawings are simple and grow with the level:
// a seed, a sprout, a branch, a tree, a tree with fruit. Locked levels are drawn in grey.
defineProps({
  code: { type: String, required: true }, // SEED | SPROUT | BRANCH | TREE | FRUITFUL
  size: { type: Number, default: 28 },
  locked: { type: Boolean, default: false },
})
</script>

<template>
  <span
    :class="[
      'inline-flex shrink-0 items-center justify-center rounded-full',
      locked ? 'bg-track text-muted' : 'bg-navy-900 text-gold-500 ring-2 ring-gold-500',
    ]"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  >
    <svg viewBox="0 0 32 32" :width="size * 0.68" :height="size * 0.68" fill="currentColor" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
      <!-- Seed -->
      <g v-if="code === 'SEED'">
        <ellipse cx="16" cy="17" rx="5.5" ry="8" transform="rotate(-25 16 17)" stroke="none" />
        <path d="M13.5 20 C15 17 17 14.5 19 13" fill="none" stroke-width="1.6" class="text-navy-900" :style="locked ? 'color: #E3E8F4' : ''" />
      </g>
      <!-- Sprout -->
      <g v-else-if="code === 'SPROUT'">
        <path d="M16 27 V16" fill="none" stroke-width="2.4" />
        <path d="M16 18 C10 18 7 15 7 10 C13 10 16 13 16 18 Z" stroke="none" />
        <path d="M16 15 C16 10 19 6 25 6 C25 12 21 15 16 15 Z" stroke="none" />
      </g>
      <!-- Branch -->
      <g v-else-if="code === 'BRANCH'">
        <path d="M8 27 C11 20 15 16 23 8" fill="none" stroke-width="2.4" />
        <circle cx="12" cy="16" r="3" stroke="none" />
        <circle cx="19.5" cy="19" r="3" stroke="none" />
        <circle cx="21.5" cy="11" r="3" stroke="none" />
      </g>
      <!-- Tree -->
      <g v-else-if="code === 'TREE'">
        <circle cx="16" cy="13" r="8" stroke="none" />
        <rect x="14.2" y="19" width="3.6" height="9" rx="1" stroke="none" />
      </g>
      <!-- Fruitful: a tree with fruit -->
      <g v-else>
        <circle cx="16" cy="13" r="8.5" stroke="none" />
        <rect x="14.2" y="19.5" width="3.6" height="8.5" rx="1" stroke="none" />
        <circle cx="12.5" cy="11.5" r="1.9" stroke="none" :style="locked ? 'fill: #E3E8F4' : 'fill: #0E2250'" />
        <circle cx="19.5" cy="10.5" r="1.9" stroke="none" :style="locked ? 'fill: #E3E8F4' : 'fill: #0E2250'" />
        <circle cx="16" cy="16.5" r="1.9" stroke="none" :style="locked ? 'fill: #E3E8F4' : 'fill: #0E2250'" />
      </g>
    </svg>
  </span>
</template>
