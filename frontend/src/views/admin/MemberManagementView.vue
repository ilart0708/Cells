<script setup>
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listAdminMembers } from '../../api/adminStats'
import { btnOutline } from '../../components/buttonClasses'
import GrowthLevelPill from '../../components/GrowthLevelPill.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import TodayStatusPill from '../../components/TodayStatusPill.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'
import { plural } from '../../utils/stats'

const today = ref('')

// The endpoint returns the day it refers to together with one page of members.
const members = reactive(
  usePagedList(async (params) => {
    const data = await listAdminMembers(params)
    today.value = data.today
    return data.members
  }),
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-2xl font-bold text-navy-900">Member management</h1>
        <p class="mt-1 text-sm text-muted">
          Members are listed alphabetically, with their participation records. You see when someone shared, never
          the text of their answer.
          <template v-if="today">Today’s status is for {{ formatDay(today) }}.</template>
        </p>
      </div>
      <button type="button" :class="btnOutline" :disabled="members.loading" @click="members.reload">
        {{ members.loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </header>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Members">
      <div class="border-b border-line p-4">
        <TextField v-model="members.query" label="Search members" type="search" hint="Search by name, email or church." />
      </div>

      <ListState
        :loading="members.loading"
        :error="members.error"
        :count="members.items.length"
        :empty-message="members.query ? 'No members match your search.' : 'There are no members yet.'"
        @retry="members.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="member in members.items" :key="member.memberId" class="space-y-3 px-4 py-4">
            <div class="flex flex-wrap items-start gap-3">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="break-words font-semibold text-navy-900">{{ member.fullName }}</p>
                <p class="break-words text-sm text-muted">{{ member.email }} · {{ member.churchName }}</p>
                <p class="text-sm text-muted">
                  {{ plural(member.rooms, 'room') }} · {{ member.devotionsShared }} shared · {{ member.totalPoints }} EXP
                </p>
              </div>
              <StatusPill v-if="member.accountStatus === 'DISABLED'" status="DISABLED" />
              <GrowthLevelPill :level="member.level" />
              <RouterLink :to="{ name: 'admin-member', params: { memberId: member.memberId } }" :class="btnOutline">
                Details<span class="sr-only"> of {{ member.fullName }}</span>
              </RouterLink>
            </div>

            <ul v-if="member.roomsToday.length > 0" class="space-y-2" aria-label="Today by room">
              <li v-for="room in member.roomsToday" :key="room.roomId" class="flex flex-wrap items-center gap-2 text-sm">
                <span class="min-w-0 flex-1 break-words text-navy-900">
                  {{ room.churchName }} – {{ room.roomName }}<span class="text-muted"> · Led by {{ room.leaderName }}</span>
                </span>
                <TodayStatusPill :status="room.today" />
              </li>
            </ul>
            <p v-else class="text-sm text-muted">Not in an active room.</p>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="members.page"
        :total-pages="members.totalPages"
        :total-elements="members.totalElements"
        :loading="members.loading"
        @change="members.goTo"
      />
    </section>
  </div>
</template>
