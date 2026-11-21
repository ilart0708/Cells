<script setup>
import { reactive, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getAdminMember, listMemberSubmissions } from '../../api/adminStats'
import { btnOutline } from '../../components/buttonClasses'
import GrowthLevelPill from '../../components/GrowthLevelPill.vue'
import ListState from '../../components/ListState.vue'
import PageState from '../../components/PageState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatTile from '../../components/StatTile.vue'
import StatusPill from '../../components/StatusPill.vue'
import TodayStatusPill from '../../components/TodayStatusPill.vue'
import { useLiveResource } from '../../composables/useLiveResource'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'
import { formatDate, formatDateTime } from '../../utils/format'

const props = defineProps({ memberId: { type: Number, required: true } })

const { state, data, refreshing, announcement, reload, refresh } = useLiveResource(() =>
  getAdminMember(props.memberId),
)

// What the member shared and when. The server never sends the text of an answer.
const history = reactive(
  usePagedList(({ page, size }) => listMemberSubmissions(props.memberId, { page, size })),
)

watch(
  () => props.memberId,
  () => {
    reload()
    history.goTo(0)
  },
)

function refreshAll() {
  refresh()
  history.reload()
}
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'admin-members' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Members
    </RouterLink>

    <PageState
      :state="state"
      loading-text="Loading the member…"
      not-found-title="This member could not be found."
      not-found-hint="Go back to the list of members."
      error-text="The member could not be loaded."
      @retry="reload"
    >
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-1">
          <h1 class="break-words text-2xl font-bold text-navy-900">{{ data.member.fullName }}</h1>
          <p class="break-words text-sm text-muted">{{ data.member.email }} · {{ data.member.churchName }}</p>
          <p class="text-sm text-muted">
            Member since {{ formatDate(data.member.memberSince) }} · Today is {{ formatDay(data.today) }}
          </p>
          <div class="flex flex-wrap gap-2 pt-1">
            <StatusPill v-if="data.member.accountStatus === 'DISABLED'" status="DISABLED" />
            <GrowthLevelPill :level="data.member.level" />
          </div>
        </div>
        <button type="button" :class="btnOutline" :disabled="refreshing" @click="refreshAll">
          {{ refreshing ? 'Refreshing…' : 'Refresh' }}
        </button>
      </header>

      <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-label="Totals">
        <StatTile label="Total EXP" :value="data.member.totalPoints" />
        <StatTile label="Level" :value="data.member.level.name" :hint="`From ${data.member.level.threshold} EXP`" />
        <StatTile label="Devotions shared" :value="data.member.devotionsShared" />
        <StatTile label="Rooms" :value="data.member.rooms" />
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="rooms-title">
        <div class="border-b border-line p-4">
          <h2 id="rooms-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Rooms today</h2>
        </div>

        <p v-if="data.member.roomsToday.length === 0" class="p-6 text-center text-sm text-muted">
          Not in an active room.
        </p>

        <ul v-else class="divide-y divide-line">
          <li v-for="room in data.member.roomsToday" :key="room.roomId" class="flex flex-wrap items-center gap-3 px-4 py-4">
            <div class="min-w-0 flex-1 space-y-1">
              <RoomTitle
                :church-name="room.churchName"
                :room-name="room.roomName"
                :leader-name="room.leaderName"
                :level="3"
              />
              <p class="text-sm text-muted">Participation number {{ room.participationNumber }}</p>
            </div>
            <TodayStatusPill :status="room.today" />
            <RouterLink :to="{ name: 'admin-room-stats', params: { roomId: room.roomId } }" :class="btnOutline">
              Room statistics<span class="sr-only"> of {{ room.roomName }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="history-title">
        <div class="border-b border-line p-4">
          <h2 id="history-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">What they shared</h2>
          <p class="mt-1 text-sm text-muted">
            When and which passage, newest first. What a member writes is private to their room and isn’t shown here.
          </p>
        </div>

        <ListState
          :loading="history.loading"
          :error="history.error"
          :count="history.items.length"
          empty-message="This member hasn’t shared anything yet."
          @retry="history.reload"
        >
          <ul class="divide-y divide-line">
            <li v-for="entry in history.items" :key="entry.submissionId" class="flex flex-wrap items-center gap-3 px-4 py-4">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="text-sm text-muted">{{ formatDay(entry.devotionDate) }}</p>
                <p class="break-words font-semibold text-navy-900">
                  {{ entry.reference }}<template v-if="entry.title"> · {{ entry.title }}</template>
                </p>
                <p class="text-sm text-muted">
                  {{ entry.churchName }} – {{ entry.roomName }} · shared {{ formatDateTime(entry.submittedAt) }}
                </p>
              </div>
              <span class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">+{{ entry.points }} EXP</span>
            </li>
          </ul>
        </ListState>

        <PaginationBar
          :page="history.page"
          :total-pages="history.totalPages"
          :total-elements="history.totalElements"
          :loading="history.loading"
          @change="history.goTo"
        />
      </section>
    </PageState>
  </div>
</template>
