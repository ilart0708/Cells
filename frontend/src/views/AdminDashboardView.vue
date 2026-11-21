<script setup>
import { RouterLink } from 'vue-router'
import { getAdminDashboard } from '../api/adminStats'
import { btnOutline, btnPrimary } from '../components/buttonClasses'
import PageState from '../components/PageState.vue'
import PeriodPanel from '../components/PeriodPanel.vue'
import StatsHelp from '../components/StatsHelp.vue'
import StatTile from '../components/StatTile.vue'
import TodayPanel from '../components/TodayPanel.vue'
import { useLiveResource } from '../composables/useLiveResource'
import { formatDay } from '../utils/dates'
import { plural } from '../utils/stats'

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(getAdminDashboard)
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <PageState
      :state="state"
      loading-text="Loading the dashboard…"
      error-text="The dashboard could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 class="text-2xl font-bold text-navy-900">Administrator dashboard</h1>
          <p class="mt-1 text-sm text-muted">
            {{ formatDay(data.periods.today) }} · participation across the whole platform. Counts only.
          </p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-5" aria-label="Totals">
        <StatTile label="Churches" :value="data.churches" />
        <StatTile
          label="Pastors"
          :value="data.pastors"
          :hint="data.disabledPastors > 0 ? `${data.disabledPastors} disabled` : ''"
        />
        <StatTile
          label="Leaders"
          :value="data.totals.leaders"
          :hint="data.totals.disabledLeaders > 0 ? `${data.totals.disabledLeaders} disabled` : ''"
        />
        <StatTile label="Rooms" :value="data.totals.rooms" />
        <StatTile
          label="Members"
          :value="data.totals.members"
          :hint="data.totals.disabledMembers > 0 ? `${plural(data.totals.disabledMembers, 'disabled account')} not counted` : ''"
        />
      </section>

      <TodayPanel :today="data.totals.today" :rooms="data.totals.rooms" />

      <div class="grid gap-4 md:grid-cols-2">
        <PeriodPanel
          title="Last 7 days"
          :subtitle="`${formatDay(data.periods.last7From)} to ${formatDay(data.periods.through)}`"
          :stats="data.totals.last7"
          unit="room-day"
        />
        <PeriodPanel
          title="Last 30 days"
          :subtitle="`${formatDay(data.periods.last30From)} to ${formatDay(data.periods.through)}`"
          :stats="data.totals.last30"
          unit="room-day"
        />
      </div>

      <div class="flex flex-wrap gap-2">
        <RouterLink :to="{ name: 'admin-analytics' }" :class="btnPrimary">Participation analytics</RouterLink>
        <RouterLink :to="{ name: 'admin-rooms' }" :class="btnOutline">Room monitoring</RouterLink>
        <RouterLink :to="{ name: 'admin-members' }" :class="btnOutline">Member management</RouterLink>
      </div>

      <StatsHelp audience="admin" />
    </PageState>
  </div>
</template>
