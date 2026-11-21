<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getExperience } from '../../api/experience'
import { listMyRooms } from '../../api/memberRooms'
import { getParticipationBoard } from '../../api/participation'
import { btnOutline } from '../../components/buttonClasses'
import ListState from '../../components/ListState.vue'
import ProgressBar from '../../components/ProgressBar.vue'
import RoomCard from '../../components/RoomCard.vue'
import { useAuthStore } from '../../stores/auth'
import { levelProgress } from '../../utils/experience'

const auth = useAuthStore()

const rooms = ref([])
const loading = ref(true)
const error = ref('')

// roomId -> today's devotion (null: none posted) and my status. A missing key means "not known",
// so the card says nothing.
const todayByRoom = reactive({})
const statusByRoom = reactive({})

// My growth. If this fails the card is simply not shown: it is a nice extra, never an error.
const growth = ref(null)
const progress = computed(() => levelProgress(growth.value))

async function loadToday(list) {
  await Promise.allSettled(
    list.map(async (room) => {
      const data = await getParticipationBoard(room.roomId)
      todayByRoom[room.roomId] = data.devotion
      statusByRoom[room.roomId] = data.myStatus
    }),
  )
}

async function loadGrowth() {
  try {
    growth.value = await getExperience()
  } catch {
    growth.value = null
  }
}

async function load() {
  loading.value = true
  error.value = ''
  Object.keys(todayByRoom).forEach((key) => delete todayByRoom[key])
  Object.keys(statusByRoom).forEach((key) => delete statusByRoom[key])
  try {
    rooms.value = await listMyRooms()
    loadToday(rooms.value) // not awaited: the cards show up first, the devotion lines follow
    loadGrowth()
  } catch (e) {
    error.value = e?.message || 'Could not load your cell groups.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <section class="rounded-[18px] border border-line bg-white p-5">
      <p class="text-xs font-bold uppercase tracking-[2px] text-muted">Your area</p>
      <h1 class="mt-1 text-2xl font-bold text-navy-900">Welcome, {{ auth.user.fullName }}</h1>
      <p class="mt-1 text-sm text-muted">{{ auth.user.email }}</p>
    </section>

    <section aria-labelledby="rooms-title" class="space-y-3">
      <h2 id="rooms-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Your cell groups</h2>

      <ListState
        :loading="loading"
        :error="error"
        :count="rooms.length"
        empty-message="You are not in a cell group yet. Your leader will add you, or send you an invitation link."
        @retry="load"
      >
        <div class="space-y-4">
          <RoomCard
            v-for="room in rooms"
            :key="room.roomId"
            :room="room"
            :today-devotion="todayByRoom[room.roomId]"
            :my-status="statusByRoom[room.roomId]"
          />
        </div>
      </ListState>
    </section>

    <section v-if="growth" class="space-y-3 rounded-[18px] border border-line bg-white p-5" aria-labelledby="growth-title">
      <h2 id="growth-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Your growth</h2>
      <p class="text-lg font-bold text-navy-900">
        {{ growth.level.name }}<span class="ml-2 text-sm font-semibold text-muted">{{ growth.totalPoints }} EXP</span>
      </p>
      <ProgressBar
        v-if="progress"
        :label="`Progress to ${growth.nextLevel.name}`"
        :value="progress.value"
        :max="progress.max"
        :summary="`${progress.value} of ${progress.max} EXP towards ${growth.nextLevel.name}`"
      />
      <div class="flex flex-wrap gap-2">
        <RouterLink :to="{ name: 'member-experience' }" :class="btnOutline">Experience</RouterLink>
        <RouterLink :to="{ name: 'member-history' }" :class="btnOutline">Participation history</RouterLink>
      </div>
    </section>
  </div>
</template>
