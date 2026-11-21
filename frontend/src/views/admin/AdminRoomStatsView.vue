<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getAdminRoomStats } from '../../api/adminStats'
import { btnOutline } from '../../components/buttonClasses'
import PageState from '../../components/PageState.vue'
import RoomStatsBody from '../../components/RoomStatsBody.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import { useLiveResource } from '../../composables/useLiveResource'
import { formatDay } from '../../utils/dates'
import { plural } from '../../utils/stats'

const props = defineProps({ roomId: { type: Number, required: true } })

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(() =>
  getAdminRoomStats(props.roomId),
)

watch(() => props.roomId, () => reload())
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'admin-rooms' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Rooms
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading statistics…"
      not-found-title="This room could not be found."
      not-found-hint="It may have been archived. Go back to the list of rooms."
      error-text="The statistics could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Room statistics</h1>
          <RoomTitle
            :church-name="data.room.churchName"
            :room-name="data.room.roomName"
            :leader-name="data.room.leaderName"
            :level="2"
          />
          <p class="text-sm text-muted">
            {{ formatDay(data.periods.today) }} · {{ plural(data.room.members, 'member') }}<template
              v-if="data.room.disabledMembers > 0"
            > · {{ plural(data.room.disabledMembers, 'disabled account') }} not counted</template>
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <RouterLink :to="{ name: 'admin-leader-stats', params: { leaderId: data.room.leaderId } }" :class="btnOutline">
            Leader statistics
          </RouterLink>
          <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
            {{ refreshing ? 'Refreshing…' : 'Refresh' }}
          </button>
        </div>
      </header>

      <RoomStatsBody :data="data" audience="admin" />
    </PageState>
  </div>
</template>
