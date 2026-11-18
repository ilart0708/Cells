<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ApiError } from '../../api/http'
import { createRoom, listRooms, renameRoom, setRoomStatus } from '../../api/rooms'
import BaseDialog from '../../components/BaseDialog.vue'
import { btnOutline, btnPrimary, btnSecondary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { RouterLink } from 'vue-router'
import { formatDate, memberLabel } from '../../utils/format'

const FILTERS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ARCHIVED', label: 'Archived' },
  { value: 'ALL', label: 'All' },
]

const notice = ref('')
const filter = ref('ACTIVE')

const rooms = reactive(usePagedList((params) => listRooms({ ...params, status: filter.value })))

watch(filter, () => rooms.goTo(0))

const emptyMessage = computed(() => {
  if (rooms.query) return 'No rooms match your search.'
  if (filter.value === 'ARCHIVED') return 'No archived rooms.'
  return 'You have no rooms yet. Create your first room to start your cell group.'
})

// ---------- create / rename ----------
const form = reactive({ open: false, editing: null, name: '', saving: false, error: '', nameError: '' })

function openCreate() {
  Object.assign(form, { open: true, editing: null, name: '', error: '', nameError: '' })
}

function openRename(room) {
  Object.assign(form, { open: true, editing: room, name: room.name, error: '', nameError: '' })
}

async function save() {
  if (form.saving) return
  form.error = ''
  form.nameError = ''

  const name = form.name.trim()
  if (!name) {
    form.nameError = 'Please enter the room name.'
    return
  }

  form.saving = true
  try {
    const renaming = form.editing !== null
    if (renaming) {
      await renameRoom(form.editing.id, name)
    } else {
      await createRoom(name)
    }
    notice.value = renaming ? 'Room renamed.' : 'Room created.'
    form.open = false
    await rooms.reload()
  } catch (e) {
    if (e instanceof ApiError) {
      form.nameError = e.fieldErrors.name || (e.status === 409 ? e.message : '')
      if (!form.nameError) form.error = e.message
    } else {
      form.error = 'Something went wrong. Please try again.'
    }
  } finally {
    form.saving = false
  }
}

// ---------- archive / restore ----------
const status = reactive({ open: false, target: null, busy: false, error: '' })
const archiving = computed(() => status.target?.status === 'ACTIVE')

function askStatus(room) {
  Object.assign(status, { open: true, target: room, busy: false, error: '' })
}

async function confirmStatus() {
  if (status.busy || !status.target) return
  const room = status.target
  const next = room.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'

  status.busy = true
  status.error = ''
  try {
    await setRoomStatus(room.id, next)
    status.open = false
    notice.value = `${room.name} was ${next === 'ARCHIVED' ? 'archived' : 'restored'}.`
    await rooms.reload()
  } catch (e) {
    status.error = e?.message || 'Something went wrong. Please try again.'
  } finally {
    status.busy = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold text-navy-900">My Rooms</h1>
      <button type="button" :class="btnPrimary" @click="openCreate">Create room</button>
    </header>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Rooms">
      <div class="space-y-4 border-b border-line p-4">
        <div role="group" aria-label="Show rooms" class="flex flex-wrap gap-2">
          <button
            v-for="option in FILTERS"
            :key="option.value"
            type="button"
            :aria-pressed="filter === option.value"
            :class="filter === option.value ? btnSecondary : btnOutline"
            @click="filter = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <TextField v-model="rooms.query" label="Search rooms" type="search" />
      </div>

      <ListState
        :loading="rooms.loading"
        :error="rooms.error"
        :count="rooms.items.length"
        :empty-message="emptyMessage"
        @retry="rooms.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="room in rooms.items" :key="room.id" class="flex flex-wrap items-center gap-3 px-4 py-4">
            <div class="min-w-0 flex-1">
              <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" />
              <p class="mt-1 text-sm text-muted">
                {{ memberLabel(room.memberCount) }} ·
                <template v-if="room.status === 'ARCHIVED'">Archived {{ formatDate(room.archivedAt) }}</template>
                <template v-else>Created {{ formatDate(room.createdAt) }}</template>
              </p>
            </div>
            <StatusPill :status="room.status" />
         <RouterLink :to="{ name: 'leader-room', params: { roomId: room.id } }" :class="btnSecondary">
           Open<span class="sr-only"> {{ room.name }}</span>
         </RouterLink>
        <button
          v-if="room.status === 'ACTIVE'"
          type="button"
          :class="btnOutline"
          :aria-label="`Rename ${room.name}`"
          @click="openRename(room)"
        >
          Rename
        </button>
        <button
          type="button"
          :class="btnOutline"
          :aria-label="`${room.status === 'ACTIVE' ? 'Archive' : 'Restore'} ${room.name}`"
          @click="askStatus(room)"
        >
          {{ room.status === 'ACTIVE' ? 'Archive' : 'Restore' }}
        </button>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="rooms.page"
        :total-pages="rooms.totalPages"
        :total-elements="rooms.totalElements"
        :loading="rooms.loading"
        @change="rooms.goTo"
      />
    </section>

    <BaseDialog v-model:open="form.open" :title="form.editing ? 'Rename room' : 'Create room'">
      <form class="space-y-5" novalidate @submit.prevent="save">
        <p v-if="form.error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ form.error }}
        </p>
        <TextField
          v-model="form.name"
          label="Room name"
          hint="Your church’s name is added in front automatically."
          :error="form.nameError"
          maxlength="100"
          required
        />
        <div class="flex justify-end gap-3">
          <button type="button" :class="btnOutline" @click="form.open = false">Cancel</button>
          <button type="submit" :class="btnPrimary" :disabled="form.saving">
            {{ form.saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </form>
    </BaseDialog>

    <ConfirmDialog
      v-model:open="status.open"
      :title="`${archiving ? 'Archive' : 'Restore'} ${status.target?.name ?? ''}?`"
      :message="
        archiving
          ? 'The room is hidden from your active list and from its members. Nothing is deleted: the members, their order and the history are kept, and you can restore the room at any time. Archived rooms cannot get new members.'
          : 'The room becomes active again and its members can see it. If another active room already uses this name, rename that room first.'
      "
      :confirm-label="archiving ? 'Archive' : 'Restore'"
      :busy="status.busy"
      :error="status.error"
      @confirm="confirmStatus"
    />
  </div>
</template>
