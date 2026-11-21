<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getLeaderStats } from '../../api/pastorStats'
import { btnOutline } from '../../components/buttonClasses'
import PageState from '../../components/PageState.vue'
import PeriodPanel from '../../components/PeriodPanel.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatsHelp from '../../components/StatsHelp.vue'
import StatTile from '../../components/StatTile.vue'
import StatusPill from '../../components/StatusPill.vue'
import TodayPanel from '../../components/TodayPanel.vue'
import { useLiveResource } from '../../composables/useLiveResource'
import { formatDay } from '../../utils/dates'
import { plural, rateText, todayFraction } from '../../utils/stats'

const props = defineProps({ leaderId: { type: Number, required: true } })

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(() =>
  getLeaderStats(props.leaderId),
)

watch(() => props.leaderId, () => reload())
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'pastor-dashboard' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Dashboard
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading statistics…"
      not-found-title="This leader could not be found."
      not-found-hint="Go back to your dashboard and choose one of your leaders."
      error-text="The statistics could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-1">
          <h1 class="break-words text-2xl font-bold text-navy-900">{{ data.leader.fullName }}</h1>
          <p class="text-sm text-muted">{{ data.leader.churchName }} · {{ formatDay(data.periods.today) }}</p>
          <StatusPill v-if="data.leader.accountStatus === 'DISABLED'" status="DISABLED" />
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section class="grid gap-4 sm:grid-cols-2" aria-label="Totals">
        <StatTile label="Rooms" :value="data.leader.rooms" />
        <StatTile
          label="Members"
          :value="data.leader.members"
          :hint="data.leader.disabledMembers > 0 ? `${plural(data.leader.disabledMembers, 'disabled account')} not counted` : ''"
        />
      </section>

      <TodayPanel :today="data.leader.today" :rooms="data.leader.rooms" />

      <div class="grid gap-4 md:grid-cols-2">
        <PeriodPanel
          title="Last 7 days"
          :subtitle="`${formatDay(data.periods.last7From)} to ${formatDay(data.periods.through)}`"
          :stats="data.leader.last7"
          :unit="data.leader.rooms === 1 ? 'day' : 'room-day'"
        />
        <PeriodPanel
          title="Last 30 days"
          :subtitle="`${formatDay(data.periods.last30From)} to ${formatDay(data.periods.through)}`"
          :stats="data.leader.last30"
          :unit="data.leader.rooms === 1 ? 'day' : 'room-day'"
        />
      </div>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="rooms-title">
        <div class="border-b border-line p-4">
          <h2 id="rooms-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Rooms</h2>
        </div>

        <p v-if="data.rooms.length === 0" class="p-6 text-center text-sm text-muted">
          This leader has no active rooms yet.
        </p>

        <ul v-else class="divide-y divide-line">
          <li v-for="room in data.rooms" :key="room.roomId" class="flex flex-wrap items-center gap-3 px-4 py-4">
            <div class="min-w-0 flex-1 space-y-1">
              <RoomTitle
                :church-name="room.churchName"
                :room-name="room.roomName"
                :leader-name="room.leaderName"
                :level="3"
              />
              <p class="text-sm text-muted">
                {{ plural(room.members, 'member') }}<template
                  v-if="room.disabledMembers > 0"
                > · {{ room.disabledMembers }} disabled</template> · Last 7 days: {{ rateText(room.last7.ratePercent) }}
              </p>
            </div>

            <div class="text-right">
              <template v-if="todayFraction(room.today)">
                <p class="text-2xl font-extrabold text-navy-900">
                  {{ todayFraction(room.today) }}<span class="sr-only"> shared today</span>
                </p>
                <p class="text-xs text-muted" aria-hidden="true">shared today</p>
              </template>
              <p v-else class="text-sm text-muted">No devotion today</p>
            </div>

            <RouterLink :to="{ name: 'pastor-room-stats', params: { roomId: room.roomId } }" :class="btnOutline">
              Room statistics<span class="sr-only"> of {{ room.roomName }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <StatsHelp />
    </PageState>
  </div>
</template>
