<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { listChurchStats, listLeaderStats, listPastorStats } from '../../api/adminStats'
import { btnOutline, btnSecondary } from '../../components/buttonClasses'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import StatsHelp from '../../components/StatsHelp.vue'
import StatsLine from '../../components/StatsLine.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'
import { plural } from '../../utils/stats'

const VIEWS = [
  { value: 'church', label: 'By church' },
  { value: 'pastor', label: 'By pastor' },
  { value: 'leader', label: 'By leader' },
]
const FETCHERS = { church: listChurchStats, pastor: listPastorStats, leader: listLeaderStats }
const NOUNS = { church: 'churches', pastor: 'pastors', leader: 'leaders' }

const view = ref('church')
const periods = ref(null)

// Each endpoint returns the days it covers together with one page of lines.
const list = reactive(
  usePagedList(async (params) => {
    const data = await FETCHERS[view.value](params)
    periods.value = data.periods
    return data.items
  }),
)

// The three kinds of line have different fields, so clear the old ones while the new view loads.
watch(view, () => {
  list.items = []
  list.goTo(0)
})

const emptyMessage = computed(() =>
  list.query ? `No ${NOUNS[view.value]} match your search.` : `There are no ${NOUNS[view.value]} yet.`,
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-2xl font-bold text-navy-900">Participation analytics</h1>
        <p class="mt-1 text-sm text-muted">
          Today and the last 7 and 30 completed days, alphabetical. Counts only.
          <template v-if="periods">
            Last 7 days: {{ formatDay(periods.last7From) }} to {{ formatDay(periods.through) }}.
          </template>
        </p>
      </div>
      <button type="button" :class="btnOutline" :disabled="list.loading" @click="list.reload">
        {{ list.loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </header>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Analytics">
      <div class="space-y-4 border-b border-line p-4">
        <div role="group" aria-label="Group the figures" class="flex flex-wrap gap-2">
          <button
            v-for="option in VIEWS"
            :key="option.value"
            type="button"
            :aria-pressed="view === option.value"
            :class="view === option.value ? btnSecondary : btnOutline"
            @click="view = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <TextField v-model="list.query" :label="`Search ${NOUNS[view]}`" type="search" />
      </div>

      <ListState
        :loading="list.loading"
        :error="list.error"
        :count="list.items.length"
        :empty-message="emptyMessage"
        @retry="list.reload"
      >
        <ul class="divide-y divide-line">
          <template v-if="view === 'church'">
            <StatsLine
              v-for="church in list.items"
              :key="church.churchId"
              :today="church.today"
              :last7="church.last7"
              :last30="church.last30"
            >
              <template #title>
                <p class="break-words font-semibold text-navy-900">{{ church.name }}</p>
              </template>
              <template #details>
                {{ plural(church.pastors, 'pastor') }} · {{ plural(church.leaders, 'leader') }} ·
                {{ plural(church.rooms, 'room') }} · {{ plural(church.members, 'member') }}<template
                  v-if="church.disabledMembers > 0"
                > ({{ church.disabledMembers }} disabled)</template>
              </template>
              <template #action>
                <RouterLink :to="{ name: 'admin-rooms', query: { q: church.name } }" :class="btnOutline">
                  View rooms<span class="sr-only"> of {{ church.name }}</span>
                </RouterLink>
              </template>
            </StatsLine>
          </template>

          <template v-else-if="view === 'pastor'">
            <StatsLine
              v-for="pastor in list.items"
              :key="pastor.pastorId"
              :today="pastor.today"
              :last7="pastor.last7"
              :last30="pastor.last30"
            >
              <template #title>
                <p class="break-words font-semibold text-navy-900">{{ pastor.fullName }}</p>
              </template>
              <template #details>
                {{ pastor.email }} · {{ pastor.churchName }} · {{ plural(pastor.leaders, 'leader') }} ·
                {{ plural(pastor.rooms, 'room') }} · {{ plural(pastor.members, 'member') }}<template
                  v-if="pastor.disabledMembers > 0"
                > ({{ pastor.disabledMembers }} disabled)</template>
              </template>
              <template #badges>
                <StatusPill v-if="pastor.accountStatus === 'DISABLED'" status="DISABLED" />
              </template>
            </StatsLine>
          </template>

          <template v-else>
            <StatsLine
              v-for="leader in list.items"
              :key="leader.leaderId"
              :today="leader.today"
              :last7="leader.last7"
              :last30="leader.last30"
            >
              <template #title>
                <p class="break-words font-semibold text-navy-900">{{ leader.fullName }}</p>
              </template>
              <template #details>
                {{ leader.email }} · {{ leader.churchName }} · {{ plural(leader.rooms, 'room') }} ·
                {{ plural(leader.members, 'member') }}<template
                  v-if="leader.disabledMembers > 0"
                > ({{ leader.disabledMembers }} disabled)</template>
              </template>
              <template #badges>
                <StatusPill v-if="leader.accountStatus === 'DISABLED'" status="DISABLED" />
              </template>
              <template #action>
                <RouterLink :to="{ name: 'admin-leader-stats', params: { leaderId: leader.leaderId } }" :class="btnOutline">
                  Statistics<span class="sr-only"> of {{ leader.fullName }}</span>
                </RouterLink>
              </template>
            </StatsLine>
          </template>
        </ul>
      </ListState>

      <PaginationBar
        :page="list.page"
        :total-pages="list.totalPages"
        :total-elements="list.totalElements"
        :loading="list.loading"
        @change="list.goTo"
      />
    </section>

    <StatsHelp audience="admin" />
  </div>
</template>
