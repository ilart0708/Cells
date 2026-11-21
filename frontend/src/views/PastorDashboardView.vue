<script setup>
import { RouterLink } from 'vue-router'
import { btnOutline, btnPrimary } from '../components/buttonClasses'
import PageState from '../components/PageState.vue'
import PaginationBar from '../components/PaginationBar.vue'
import PeriodPanel from '../components/PeriodPanel.vue'
import StatsHelp from '../components/StatsHelp.vue'
import StatTile from '../components/StatTile.vue'
import StatusPill from '../components/StatusPill.vue'
import TodayPanel from '../components/TodayPanel.vue'
import { usePastorDashboard } from '../composables/usePastorDashboard'
import { formatDay } from '../utils/dates'
import { plural, rateText, todayFraction } from '../utils/stats'

const { state, data, refreshing, announcement, reload, refresh, goTo } = usePastorDashboard(20)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <PageState
      :state="state"
      loading-text="Loading your dashboard…"
      error-text="Your dashboard could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 class="text-2xl font-bold text-navy-900">Pastor dashboard</h1>
          <p class="mt-1 text-sm text-muted">
            {{ formatDay(data.periods.today) }} · participation across your leaders’ rooms. Counts only: no member is named.
          </p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section v-if="data.totals.leaders === 0" class="rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">You have no leaders yet.</p>
        <p class="mt-1 text-sm text-muted">Invite your first leader, and their rooms will appear here.</p>
        <RouterLink :to="{ name: 'pastor-leaders' }" :class="[btnPrimary, 'mt-4']">My Leaders</RouterLink>
      </section>

      <template v-else>
        <section class="grid gap-4 sm:grid-cols-3" aria-label="Totals">
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

        <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="leaders-title">
          <div class="border-b border-line p-4">
            <h2 id="leaders-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Your leaders</h2>
            <p class="mt-1 text-sm text-muted">
              Today’s figure is members who have shared out of those expected. The list is alphabetical.
            </p>
          </div>

          <p v-if="data.leaders.content.length === 0" class="p-6 text-center text-sm text-muted">
            No leaders on this page.
          </p>

          <ul v-else class="divide-y divide-line">
            <li v-for="leader in data.leaders.content" :key="leader.leaderId" class="flex flex-wrap items-center gap-3 px-4 py-4">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="truncate font-semibold text-navy-900">{{ leader.fullName }}</p>
                <p class="text-sm text-muted">
                  {{ plural(leader.rooms, 'room') }} · {{ plural(leader.members, 'member') }}<template
                    v-if="leader.disabledMembers > 0"
                  > · {{ leader.disabledMembers }} disabled</template>
                </p>
                <p class="text-sm text-muted">Last 7 days: {{ rateText(leader.last7.ratePercent) }}</p>
              </div>

              <StatusPill v-if="leader.accountStatus === 'DISABLED'" status="DISABLED" />

              <div class="text-right">
                <template v-if="todayFraction(leader.today)">
                  <p class="text-2xl font-extrabold text-navy-900">
                    {{ todayFraction(leader.today) }}<span class="sr-only"> shared today</span>
                  </p>
                  <p class="text-xs text-muted" aria-hidden="true">shared today</p>
                </template>
                <p v-else class="text-sm text-muted">No devotion today</p>
              </div>

              <RouterLink :to="{ name: 'pastor-leader-stats', params: { leaderId: leader.leaderId } }" :class="btnOutline">
                Statistics<span class="sr-only"> of {{ leader.fullName }}</span>
              </RouterLink>
            </li>
          </ul>

          <PaginationBar
            :page="data.leaders.page"
            :total-pages="data.leaders.totalPages"
            :total-elements="data.leaders.totalElements"
            :loading="refreshing"
            @change="goTo"
          />
        </section>

        <div class="flex flex-wrap gap-2">
          <RouterLink :to="{ name: 'pastor-reports' }" :class="btnOutline">Participation reports</RouterLink>
        </div>

        <StatsHelp />
      </template>
    </PageState>
  </div>
</template>
