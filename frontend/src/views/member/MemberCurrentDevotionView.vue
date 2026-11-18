<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { getTodayDevotion } from '../../api/memberDevotions'
import { getMyRoom } from '../../api/memberRooms'
import { btnOutline } from '../../components/buttonClasses'
import DevotionCard from '../../components/DevotionCard.vue'
import PageState from '../../components/PageState.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import { formatDay } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const today = ref('')
const devotion = ref(null)
const refreshing = ref(false)
const announcement = ref('') // read out by screen readers only
let latestRequest = 0

async function load({ quiet = false } = {}) {
  const id = ++latestRequest
  if (quiet) refreshing.value = true
  else state.value = 'loading'

  try {
    const [roomData, todayData] = await Promise.all([getMyRoom(props.roomId), getTodayDevotion(props.roomId)])
    if (id !== latestRequest) return // a newer request replaced this one
    room.value = roomData.room
    today.value = todayData.today
    devotion.value = todayData.devotion
    state.value = 'ready'
    if (quiet) {
      announcement.value = todayData.devotion
        ? 'Updated. Today’s devotion is shown.'
        : 'Updated. No devotion has been posted yet today.'
    }
  } catch (e) {
    if (id !== latestRequest) return
    // The server answers 404 when the room is archived or the member was removed from it.
    if (e instanceof ApiError && [400, 404].includes(e.status)) state.value = 'notfound'
    else if (!quiet) state.value = 'error'
    else announcement.value = 'Could not refresh. Please try again.'
  } finally {
    if (id === latestRequest) refreshing.value = false
  }
}

onMounted(() => load())
watch(() => props.roomId, () => load())
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'member-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Your room
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading today’s devotion…"
      not-found-title="This room could not be found."
      not-found-hint="You may no longer be in this room, or it may have been archived. Your dashboard lists the rooms you belong to."
      error-text="Today’s devotion could not be loaded."
      @retry="load()"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Today’s devotion</h1>
          <RoomTitle
            :church-name="room.churchName"
            :room-name="room.roomName"
            :leader-name="room.leaderName"
            :level="2"
          />
          <p class="text-sm text-muted">{{ formatDay(today) }}</p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="load({ quiet: true })">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <DevotionCard
        v-if="devotion"
        :reference="devotion.reference"
        :title="devotion.title ?? ''"
        :level="devotion.learningLevel"
        :instructions="devotion.instructions ?? ''"
        :date="devotion.date"
      />

      <section v-else class="rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">No devotion has been posted for today yet.</p>
        <p class="mt-1 text-sm text-muted">
          Your leader hasn’t posted today’s passage. Check back later, or press Refresh.
        </p>
      </section>

      <p v-if="devotion" class="text-sm text-muted">Sharing your understanding of this passage will open here soon.</p>

      <RouterLink :to="{ name: 'member-devotions', params: { roomId } }" :class="btnOutline">
        Past devotions
      </RouterLink>
    </PageState>
  </div>
</template>
