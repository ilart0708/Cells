<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { getMyRoom } from '../../api/memberRooms'
import ParticipationBadge from '../../components/ParticipationBadge.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import { formatDate, memberLabel } from '../../utils/format'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const detail = ref(null)
const refreshing = ref(false)
let latestRequest = 0

const room = computed(() => detail.value?.room)
const order = computed(() => detail.value?.order ?? [])

async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (quiet) refreshing.value = true
  else state.value = 'loading'
  try {
    const data = await getMyRoom(props.roomId)
    if (id !== latestRequest) return
    detail.value = data
    state.value = 'ready'
  } catch (e) {
    if (id !== latestRequest) return
    // The server answers 404 when the room is archived or the member was removed from it.
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (!quiet) state.value = 'error'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

onMounted(() => load())
watch(() => props.roomId, () => load())
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <RouterLink
      :to="{ name: 'member-dashboard' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Dashboard
    </RouterLink>

    <p v-if="state === 'loading'" role="status" class="p-6 text-center text-sm text-muted">Loading your room…</p>

    <div v-else-if="state === 'notfound'" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room could not be found.</p>
      <p class="mt-1 text-sm text-muted">
        You may no longer be in this room, or it may have been archived. Your dashboard lists the rooms you belong to.
      </p>
    </div>

    <div v-else-if="state === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">Your room could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="load()">Try again</button>
    </div>

    <template v-else>
      <section class="bg-room-card rounded-[18px] p-5 text-white">
        <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your room</p>
        <div class="mt-2">
          <RoomTitle
            :church-name="room.churchName"
            :room-name="room.roomName"
            :leader-name="room.leaderName" :pastor-name="room.pastorName" :church-verified="room.churchVerified"
            :level="1"
            tone="dark"
          />
        </div>
        <p class="mt-3 text-sm text-on-navy-soft">
          {{ memberLabel(room.memberCount) }} · Joined {{ formatDate(room.joinedAt) }}
        </p>
        <p class="mt-3">
          <span class="inline-flex items-center rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">
            Your number: {{ room.myParticipationNumber }}
          </span>
        </p>
     <div class="mt-4 flex flex-wrap gap-2">
       <RouterLink :to="{ name: 'member-devotion-current', params: { roomId: room.roomId } }" :class="btnPrimary">
         Today’s devotion
       </RouterLink>
       <RouterLink :to="{ name: 'member-devotions', params: { roomId: room.roomId } }" :class="btnOutline">
         Past devotions
       </RouterLink>
        <RouterLink :to="{ name: 'member-board', params: { roomId: room.roomId } }" :class="btnOutline">
          Growing Together
        </RouterLink>
        <RouterLink :to="{ name: 'member-history-room', params: { roomId: room.roomId } }" :class="btnOutline">
          My history
        </RouterLink>
     </div>
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="order-title">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line p-4">
          <div class="min-w-0 flex-1">
            <h2 id="order-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Participation order</h2>
            <p class="mt-1 text-sm text-muted">Members take their turn in this order. Your leader can change it.</p>
          </div>
          <button type="button" :class="btnOutline" :disabled="refreshing" @click="load({ quiet: true })">
            {{ refreshing ? 'Refreshing…' : 'Refresh' }}
          </button>
        </div>

        <ol aria-label="Participation order" class="divide-y divide-line">
          <li
            v-for="entry in order"
            :key="entry.participationNumber"
            :class="['flex items-center gap-3 px-4 py-3', entry.you ? 'bg-gold-50' : '']"
          >
            <ParticipationBadge :number="entry.participationNumber" />
            <p class="min-w-0 flex-1 truncate font-semibold text-navy-900">{{ entry.fullName }}</p>
            <span v-if="entry.you" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">You</span>
          </li>
        </ol>
      </section>
    </template>
  </div>
</template>
