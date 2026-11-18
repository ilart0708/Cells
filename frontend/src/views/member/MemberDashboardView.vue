<script setup>
import { onMounted, ref } from 'vue'
import { listMyRooms } from '../../api/memberRooms'
import ListState from '../../components/ListState.vue'
import RoomCard from '../../components/RoomCard.vue'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const rooms = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rooms.value = await listMyRooms()
  } catch (e) {
    error.value = e?.message || 'Could not load your cell groups.'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const pages = [
  { name: 'Current Devotion', phase: 5 },
  { name: 'Bible Passage', phase: 5 },
  { name: 'Submit Understanding', phase: 6 },
  { name: 'Participation History', phase: 7 },
  { name: 'Experience Statistics', phase: 7 },
]
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
          <RoomCard v-for="room in rooms" :key="room.roomId" :room="room" />
        </div>
      </ListState>
    </section>

    <section class="rounded-[18px] border border-line bg-white p-4" aria-labelledby="next-title">
      <h2 id="next-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Coming next</h2>
      <ul class="mt-3 divide-y divide-line">
        <li v-for="page in pages" :key="page.name" class="flex items-center justify-between gap-3 py-3">
          <span class="text-sm font-semibold text-navy-900">{{ page.name }}</span>
          <span class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">Phase {{ page.phase }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>
