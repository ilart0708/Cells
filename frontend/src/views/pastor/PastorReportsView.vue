<script setup>
import { RouterLink } from 'vue-router'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import PageState from '../../components/PageState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import ReportTable from '../../components/ReportTable.vue'
import StatsHelp from '../../components/StatsHelp.vue'
import { usePastorDashboard } from '../../composables/usePastorDashboard'
import { formatDay } from '../../utils/dates'

const { state, data, refreshing, announcement, reload, refresh, goTo } = usePastorDashboard(20)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <PageState
      :state="state"
      loading-text="Loading the reports…"
      error-text="The reports could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 class="text-2xl font-bold text-navy-900">Participation reports</h1>
          <p class="mt-1 text-sm text-muted">
            The last 7 and 30 completed days, by leader. Counts only: no member is named.
          </p>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section v-if="data.totals.leaders === 0" class="rounded-[18px] border border-line bg-white p-6 text-center">
        <p class="font-semibold text-navy-900">You have no leaders yet.</p>
        <p class="mt-1 text-sm text-muted">Invite your first leader, and their reports will appear here.</p>
        <RouterLink :to="{ name: 'pastor-leaders' }" :class="[btnPrimary, 'mt-4']">My Leaders</RouterLink>
      </section>

      <template v-else>
        <ReportTable
          title="Last 7 days"
          :subtitle="`${formatDay(data.periods.last7From)} to ${formatDay(data.periods.through)}`"
          period="last7"
          :leaders="data.leaders.content"
          :totals="data.totals"
        />
        <ReportTable
          title="Last 30 days"
          :subtitle="`${formatDay(data.periods.last30From)} to ${formatDay(data.periods.through)}`"
          period="last30"
          :leaders="data.leaders.content"
          :totals="data.totals"
        />

        <div class="overflow-hidden rounded-[18px] border border-line bg-white">
          <PaginationBar
            :page="data.leaders.page"
            :total-pages="data.leaders.totalPages"
            :total-elements="data.leaders.totalElements"
            :loading="refreshing"
            @change="goTo"
          />
        </div>

        <StatsHelp />
      </template>
    </PageState>
  </div>
</template>
