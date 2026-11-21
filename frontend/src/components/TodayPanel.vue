<script setup>
import { computed } from 'vue'
import ProgressBar from './ProgressBar.vue'

const props = defineProps({
  today: { type: Object, required: true }, // the backend's TodayStats
  rooms: { type: Number, required: true }, // how many rooms the figures cover
})

const roomsWithout = computed(() => props.rooms - props.today.roomsWithDevotion)
</script>

<template>
  <section class="space-y-3 rounded-[18px] border border-line bg-white p-5" aria-labelledby="today-title">
    <h2 id="today-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Today</h2>

    <p v-if="rooms === 0" class="text-sm text-muted">There are no active rooms yet.</p>
    <p v-else-if="today.roomsWithDevotion === 0" class="text-sm text-muted">
      {{ rooms === 1 ? 'No devotion has been posted in this room today.' : 'No devotion has been posted in these rooms today.' }}
    </p>
    <template v-else>
      <ProgressBar
        label="Shared today"
        :value="today.shared"
        :max="today.expected"
        :summary="`${today.shared} of ${today.expected} expected members shared today`"
      />
      <p class="text-sm text-muted">
        {{ today.shared }} shared · {{ today.stillToShare }} still to share<template v-if="today.exempt > 0"> · {{ today.exempt }} exempt</template>
      </p>
      <p v-if="roomsWithout > 0" class="text-sm text-muted">
        {{ roomsWithout }} of {{ rooms }} rooms {{ roomsWithout === 1 ? 'has' : 'have' }} no devotion today and
        {{ roomsWithout === 1 ? 'isn’t' : 'aren’t' }} counted.
      </p>
      <p class="text-sm text-muted">“Still to share” isn’t a miss: the day isn’t over yet.</p>
    </template>
  </section>
</template>
