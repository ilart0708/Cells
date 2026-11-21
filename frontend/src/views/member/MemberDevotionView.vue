<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { getMemberDevotion } from '../../api/memberDevotions'
import { getMyRoom } from '../../api/memberRooms'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import SubmissionsPanel from '../../components/SubmissionsPanel.vue'
import DevotionCard from '../../components/DevotionCard.vue'
import PageState from '../../components/PageState.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import GuidancePanel from '../../components/GuidancePanel.vue'

const props = defineProps({
  roomId: { type: Number, required: true },
  devotionId: { type: Number, required: true },
})

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const devotion = ref(null)

async function load() {
  state.value = 'loading'
  try {
    const [roomData, devotionData] = await Promise.all([
      getMyRoom(props.roomId),
      getMemberDevotion(props.roomId, props.devotionId),
    ])
    room.value = roomData.room
    devotion.value = devotionData
    state.value = 'ready'
  } catch (e) {
    // 404 also covers devotions that are not available to members: future, cancelled, or another room's.
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

onMounted(load)
watch(() => [props.roomId, props.devotionId], load)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <RouterLink
      :to="{ name: 'member-devotions', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Devotions
    </RouterLink>

    <PageState
      :state="state"
      not-found-title="This devotion could not be found."
      not-found-hint="It may not be available to you. Go back to the list of devotions."
      error-text="The devotion could not be loaded."
      @retry="load"
    >
      <header class="space-y-2">
        <h1 class="text-2xl font-bold text-navy-900">Bible passage</h1>
        <RoomTitle
          :church-name="room.churchName"
          :room-name="room.roomName"
          :leader-name="room.leaderName"
          :level="2"
        />
      </header>

      <DevotionCard
        :reference="devotion.reference"
        :title="devotion.title ?? ''"
        :level="devotion.learningLevel"
        :instructions="devotion.instructions ?? ''"
        :date="devotion.date"
      />

      <SubmissionsPanel :room-id="roomId" :devotion-id="devotionId">
        <template #locked>
          <RouterLink :to="{ name: 'member-devotion-current', params: { roomId } }" :class="btnPrimary">
            Go to today’s devotion
          </RouterLink>
        </template>
      </SubmissionsPanel>
      <GuidancePanel :room-id="roomId" :devotion-id="devotionId" />

      <RouterLink :to="{ name: 'member-devotions', params: { roomId } }" :class="btnOutline">
        All devotions
      </RouterLink>
    </PageState>
  </div>
</template>
