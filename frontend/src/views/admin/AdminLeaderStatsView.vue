<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getAdminLeaderStats } from '../../api/adminStats'
import { btnOutline } from '../../components/buttonClasses'
import LeaderStatsBody from '../../components/LeaderStatsBody.vue'
import PageState from '../../components/PageState.vue'
import StatusPill from '../../components/StatusPill.vue'
import { useLiveResource } from '../../composables/useLiveResource'
import { formatDay } from '../../utils/dates'

const props = defineProps({ leaderId: { type: Number, required: true } })

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(() =>
  getAdminLeaderStats(props.leaderId),
)

watch(() => props.leaderId, () => reload())
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'admin-analytics' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Analytics
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading statistics…"
      not-found-title="This leader could not be found."
      not-found-hint="Go back to the analytics and choose a leader."
      error-text="The statistics could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-1">
          <h1 class="break-words text-2xl font-bold text-navy-900">{{ data.leader.fullName }}</h1>
          <p class="text-sm text-muted">{{ data.leader.email }} · {{ data.leader.churchName }}</p>
          <p class="text-sm text-muted">{{ formatDay(data.periods.today) }}</p>
          <StatusPill v-if="data.leader.accountStatus === 'DISABLED'" status="DISABLED" />
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refresh">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <LeaderStatsBody :data="data" room-route-name="admin-room-stats" audience="admin" />
    </PageState>
  </div>
</template>
