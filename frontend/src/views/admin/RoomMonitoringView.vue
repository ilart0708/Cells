<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { listRoomStats } from '../../api/adminStats'
import { btnOutline } from '../../components/buttonClasses'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatsLine from '../../components/StatsLine.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'
import { plural } from '../../utils/stats'

// A church line on the analytics page links here with the church name already searched.
const route = useRoute()
const initialQuery = typeof route.query.q === 'string' ? route.query.q : ''

const periods = ref(null)

// The endpoint returns the days it covers together with one page of rooms.
const rooms = reactive(
  usePagedList(
    async (params) => {
      const data = await listRoomStats(params)
      periods.value = data.periods
      return data.items
    },
    { initialQuery },
  ),
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-2xl font-bold text-navy-900">Room monitoring</h1>
        <p class="mt-1 text-sm text-muted">
          Every active room, alphabetical by church and room. Counts only.
          <template v-if="periods">
            Today is {{ formatDay(periods.today) }}; the last 7 days are {{ formatDay(periods.last7From) }} to
            {{ formatDay(periods.through) }}.
          </template>
        </p>
      </div>
      <button type="button" :class="btnOutline" :disabled="rooms.loading" @click="rooms.reload">
        {{ rooms.loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </header>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Rooms">
      <div class="border-b border-line p-4">
        <TextField v-model="rooms.query" label="Search rooms" type="search" hint="Search by room, church or leader." />
      </div>

      <ListState
        :loading="rooms.loading"
        :error="rooms.error"
        :count="rooms.items.length"
        :empty-message="rooms.query ? 'No rooms match your search.' : 'There are no active rooms yet.'"
        @retry="rooms.reload"
      >
        <ul class="divide-y divide-line">
          <StatsLine
            v-for="room in rooms.items"
            :key="room.roomId"
            :today="room.today"
            :last7="room.last7"
            :last30="room.last30"
          >
            <template #title>
              <RoomTitle
                :church-name="room.churchName"
                :room-name="room.roomName"
                :leader-name="room.leaderName"
                :level="3"
              />
            </template>
            <template #details>
              {{ plural(room.members, 'member') }}<template v-if="room.disabledMembers > 0"> · {{ room.disabledMembers }} disabled</template>
            </template>
            <template #action>
              <RouterLink :to="{ name: 'admin-room-stats', params: { roomId: room.roomId } }" :class="btnOutline">
                Room statistics<span class="sr-only"> of {{ room.roomName }}</span>
              </RouterLink>
            </template>
          </StatsLine>
        </ul>
      </ListState>

      <PaginationBar
        :page="rooms.page"
        :total-pages="rooms.totalPages"
        :total-elements="rooms.totalElements"
        :loading="rooms.loading"
        @change="rooms.goTo"
      />
    </section>
  </div>
</template>
