<script setup>
import { RouterLink } from 'vue-router'
import { formatDate, memberLabel } from '../utils/format'
import { btnPrimary } from './buttonClasses'
import RoomTitle from './RoomTitle.vue'

// A room as its member sees it (the backend's MemberRoomResponse).
defineProps({ room: { type: Object, required: true } })
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
    <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
      <span class="inline-flex items-center rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">
        Your number: {{ room.myParticipationNumber }}
      </span>
      <RouterLink :to="{ name: 'member-room', params: { roomId: room.roomId } }" :class="btnPrimary">
        Open<span class="sr-only"> {{ room.roomName }}</span>
      </RouterLink>
    </div>
  </article>
</template>
