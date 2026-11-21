<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getExperience } from '../../api/experience'
import { btnOutline } from '../../components/buttonClasses'
import PageState from '../../components/PageState.vue'
import ProgressBar from '../../components/ProgressBar.vue'
import { levelProgress } from '../../utils/experience'

const state = ref('loading') // loading | ready | error
const data = ref(null)

async function load() {
  state.value = 'loading'
  try {
    data.value = await getExperience()
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

onMounted(load)

const progress = computed(() => levelProgress(data.value))

function levelNote(level) {
  if (level.code === data.value.level.code) return 'You are here'
  if (data.value.totalPoints >= level.threshold) return 'Reached'
  if (data.value.nextLevel && level.code === data.value.nextLevel.code) return 'Next'
  return ''
}

function sharedText(count) {
  return count === 1 ? '1 devotion shared' : `${count} devotions shared`
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <header>
      <h1 class="text-2xl font-bold text-navy-900">Experience</h1>
      <p class="mt-1 text-sm text-muted">
        EXP shows that you took part. It never measures how strong anyone’s faith is, and it isn’t a competition.
      </p>
    </header>

    <PageState
      :state="state"
      loading-text="Loading your experience…"
      error-text="Your experience could not be loaded."
      @retry="load"
    >
      <section class="bg-room-card space-y-4 rounded-[18px] p-5 text-white">
        <div>
          <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your level</p>
          <h2 class="mt-1 text-3xl font-extrabold">{{ data.level.name }}</h2>
          <p class="mt-1 text-sm text-on-navy-soft">
            {{ data.totalPoints }} EXP · {{ sharedText(data.totalSubmissions) }}
          </p>
        </div>

        <div v-if="progress" class="rounded-xl bg-white p-4 text-navy-900">
          <ProgressBar
            :label="`Progress to ${data.nextLevel.name}`"
            :value="progress.value"
            :max="progress.max"
            :summary="`${progress.value} of ${progress.max} EXP towards ${data.nextLevel.name}`"
          />
          <p class="mt-2 text-sm text-muted">
            {{ data.pointsToNextLevel }} more EXP to reach {{ data.nextLevel.name }}. Every devotion you share earns EXP.
          </p>
        </div>
        <p v-else class="rounded-xl bg-white/10 px-4 py-3 text-sm text-on-navy">
          You have reached the highest level. Keep going, one devotion at a time.
        </p>
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="ladder-title">
        <div class="border-b border-line p-4">
          <h2 id="ladder-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">The levels</h2>
        </div>
        <ol aria-label="Levels" class="divide-y divide-line">
          <li
            v-for="level in data.levels"
            :key="level.code"
            :class="['flex flex-wrap items-center gap-3 px-4 py-3', level.code === data.level.code ? 'bg-gold-50' : '']"
          >
            <p class="min-w-0 flex-1 font-semibold text-navy-900">{{ level.name }}</p>
            <p class="text-sm text-muted">{{ level.threshold }} EXP</p>
            <span
              v-if="levelNote(level)"
              :class="[
                'rounded-full px-3 py-1 text-xs font-bold',
                level.code === data.level.code ? 'bg-gold-100 text-navy-900' : 'bg-track text-navy-900',
              ]"
            >
              {{ levelNote(level) }}
            </span>
          </li>
        </ol>
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="rooms-title">
        <div class="border-b border-line p-4">
          <h2 id="rooms-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">EXP by room</h2>
        </div>

        <p v-if="data.rooms.length === 0" class="p-6 text-center text-sm text-muted">
          Your EXP will appear here after you share your first understanding.
        </p>

        <ul v-else class="divide-y divide-line">
          <li v-for="room in data.rooms" :key="room.roomId" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="break-words font-semibold text-navy-900">{{ room.churchName }} – {{ room.roomName }}</p>
              <p class="text-sm text-muted">{{ room.points }} EXP · {{ sharedText(room.submissions) }}</p>
            </div>
            <RouterLink :to="{ name: 'member-history-room', params: { roomId: room.roomId } }" :class="btnOutline">
              History<span class="sr-only"> of {{ room.roomName }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <p class="text-sm text-muted">
        Your EXP stays with you, even if you leave a room or a room is archived.
      </p>
    </PageState>
  </div>
</template>
