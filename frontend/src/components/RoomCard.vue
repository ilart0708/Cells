<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { formatDate, memberLabel } from '../utils/format'
import { btnOutline, btnPrimary } from './buttonClasses'
import RoomTitle from './RoomTitle.vue'

const props = defineProps({
  // A room as its member sees it (the backend's MemberRoomResponse).
  room: { type: Object, required: true },
  // Today's devotion for this room: undefined = not known (still loading, or it could not be checked),
  // null = nothing posted yet today, object = the devotion.
  todayDevotion: { type: Object, default: undefined },
  // The member's status today: ELIGIBLE | WAITING | SUBMITTED | EXEMPTED (undefined when not known).
  myStatus: { type: String, default: undefined },
})

const STATUS_TEXT = {
  ELIGIBLE: 'It’s your turn to share.',
  WAITING: 'Waiting for your turn.',
  SUBMITTED: 'You shared today. Thank you!',
  EXEMPTED: 'You’re exempt from today’s devotion.',
}

const statusText = computed(() => (props.todayDevotion && props.myStatus ? (STATUS_TEXT[props.myStatus] ?? '') : ''))
const sharingNow = computed(() => Boolean(props.todayDevotion) && props.myStatus === 'ELIGIBLE')
</script>

<template>
  <article class="bg-room-card rounded-[18px] p-5 text-white">
    <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your room</p>
    <div class="mt-2">
      <RoomTitle
        :church-name="room.churchName"
        :room-name="room.roomName"
        :leader-name="room.leaderName"
        :level="3"
        tone="dark"
      />
    </div>
    <p class="mt-3 text-sm text-on-navy-soft">
      {{ memberLabel(room.memberCount) }} · Joined {{ formatDate(room.joinedAt) }}
    </p>

    <p v-if="todayDevotion" class="mt-3 rounded-xl bg-white/10 px-3 py-2 text-sm text-on-navy">
      <span class="font-bold text-gold-500">Today:</span>
      {{ todayDevotion.reference }}<template v-if="todayDevotion.title"> · {{ todayDevotion.title }}</template>
    </p>
    <p v-else-if="todayDevotion === null" class="mt-3 text-sm text-on-navy-soft">
      No devotion posted yet today.
    </p>

    <p
      v-if="statusText"
      :class="[
        'mt-2 inline-flex rounded-full px-3 py-1 text-xs font-bold',
        sharingNow ? 'bg-gold-500 text-navy-900' : 'bg-white/10 text-on-navy',
      ]"
    >
      {{ statusText }}
    </p>

    <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
      <span class="inline-flex items-center rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">
        Your number: {{ room.myParticipationNumber }}
      </span>
      <div class="flex flex-wrap gap-2">
        <RouterLink :to="{ name: 'member-devotion-current', params: { roomId: room.roomId } }" :class="btnPrimary">
          {{ sharingNow ? 'Share now' : 'Today’s devotion' }}<span class="sr-only"> in {{ room.roomName }}</span>
        </RouterLink>
        <RouterLink :to="{ name: 'member-room', params: { roomId: room.roomId } }" :class="btnOutline">
          Open<span class="sr-only"> {{ room.roomName }}</span>
        </RouterLink>
      </div>
    </div>
  </article>
</template>
