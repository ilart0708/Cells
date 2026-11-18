<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave } from 'vue-router'
import { ApiError } from '../../api/http'
import {
  inviteMember,
  listRoomInvitations,
  getRoster,
  removeMember,
  revokeRoomInvitation,
  saveOrder,
} from '../../api/roomMembers'
import { getRoom } from '../../api/rooms'
import AvailableMembersPanel from '../../components/AvailableMembersPanel.vue'
import BaseDialog from '../../components/BaseDialog.vue'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import InviteLinkPanel from '../../components/InviteLinkPanel.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import ParticipationBadge from '../../components/ParticipationBadge.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDate, memberLabel } from '../../utils/format'

const props = defineProps({ roomId: { type: Number, required: true } })

const notice = ref('')
const announcement = ref('') // read out by screen readers only

// ---------- the room ----------
const room = ref(null)
const roomState = ref('loading') // loading | ready | notfound | error
const editable = computed(() => room.value?.status === 'ACTIVE')

async function loadRoom({ quiet = false } = {}) {
  if (!quiet) roomState.value = 'loading'
  try {
    room.value = await getRoom(props.roomId)
    roomState.value = 'ready'
  } catch (e) {
    roomState.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// ---------- the roster: what the server has (roster) and what is on screen (draft) ----------
const roster = ref([])
const draft = ref([])
const rosterLoading = ref(true)
const rosterError = ref('')
let rosterRequest = 0

async function loadRoster() {
  const id = ++rosterRequest
  rosterLoading.value = true
  rosterError.value = ''
  try {
    const data = await getRoster(props.roomId)
    if (id !== rosterRequest) return // a newer request replaced this one
    roster.value = data
    draft.value = [...data]
  } catch (e) {
    if (id !== rosterRequest) return
    rosterError.value = e?.message || 'Could not load the participation order.'
  } finally {
    if (id === rosterRequest) rosterLoading.value = false
  }
}

const idsOf = (list) => list.map((member) => member.memberId)
const dirty = computed(() => idsOf(draft.value).join(',') !== idsOf(roster.value).join(','))

// ---------- reordering ----------
const order = reactive({ saving: false, error: '' })

function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= draft.value.length) return

  const next = [...draft.value]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  draft.value = next

  announcement.value = `${item.fullName} moved to position ${target + 1} of ${next.length}.`
  nextTick(() => keepFocus(item.memberId, delta, target, next.length))
}

// Keep the keyboard on the arrow that was pressed. At the top or bottom that arrow is now
// disabled, so focus goes to its twin.
function keepFocus(memberId, delta, target, count) {
  const pressed = delta < 0 ? 'up' : 'down'
  const atEdge = (pressed === 'up' && target === 0) || (pressed === 'down' && target === count - 1)
  const focus = atEdge ? (pressed === 'up' ? 'down' : 'up') : pressed
  document.getElementById(`move-${focus}-${memberId}`)?.focus()
}

async function saveDraft() {
  if (order.saving || !dirty.value) return
  order.saving = true
  order.error = ''
  try {
    const saved = await saveOrder(props.roomId, idsOf(draft.value))
    roster.value = saved
    draft.value = [...saved]
    notice.value = 'Participation order saved.'
    announcement.value = 'Participation order saved.'
  } catch (e) {
    if (e instanceof ApiError && e.status === 422) {
      // Someone joined or left meanwhile (or the room was archived): show what is true now.
      await loadRoom({ quiet: true })
      await loadRoster()
      order.error = editable.value
        ? 'The room changed while you were reordering, so the list was refreshed. Please arrange it again.'
        : e.message
    } else {
      order.error = e?.message || 'Something went wrong. Please try again.'
    }
  } finally {
    order.saving = false
  }
}

function discard() {
  draft.value = [...roster.value]
  order.error = ''
  announcement.value = 'Changes discarded.'
}

// Do not lose an unsaved order by accident.
onBeforeRouteLeave(
  () => !dirty.value || window.confirm('You have unsaved changes to the participation order. Leave without saving?'),
)

function warnBeforeUnload(event) {
  if (dirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

// ---------- removing a member ----------
const removal = reactive({ open: false, target: null, busy: false, error: '' })

function askRemove(member) {
  Object.assign(removal, { open: true, target: member, busy: false, error: '' })
}

async function confirmRemove() {
  if (removal.busy || !removal.target) return
  const member = removal.target
  removal.busy = true
  removal.error = ''
  try {
    await removeMember(props.roomId, member.memberId)
    removal.open = false
    notice.value = `${member.fullName} was removed from the room.`
    announcement.value = notice.value
    await loadRoster()
  } catch (e) {
    removal.error = e?.message || 'Something went wrong. Please try again.'
    if (e instanceof ApiError && e.status === 404) await loadRoster()
  } finally {
    removal.busy = false
  }
}

// ---------- adding from the church ----------
const addOpen = ref(false)

// ---------- the room's invitations ----------
const invitations = reactive(usePagedList((params) => listRoomInvitations(props.roomId, params)))
const revoke = reactive({ open: false, target: null, busy: false, error: '' })

function askRevoke(invitation) {
  Object.assign(revoke, { open: true, target: invitation, busy: false, error: '' })
}

async function confirmRevoke() {
  if (revoke.busy || !revoke.target) return
  revoke.busy = true
  revoke.error = ''
  try {
    await revokeRoomInvitation(props.roomId, revoke.target.id)
    revoke.open = false
    notice.value = `The invitation for ${revoke.target.email} was revoked.`
    await invitations.reload()
  } catch (e) {
    revoke.error = e?.message || 'Something went wrong. Please try again.'
  } finally {
    revoke.busy = false
  }
}

// ---------- inviting a new person ----------
const created = ref(null)
const invite = reactive({ open: false, fullName: '', email: '', saving: false, error: '', errors: {} })

function openInvite() {
  created.value = null
  Object.assign(invite, { open: true, fullName: '', email: '', saving: false, error: '', errors: {} })
}

function validateInvite() {
  const errors = {}
  if (!invite.fullName.trim()) errors.fullName = 'Please enter the person’s name.'
  if (!invite.email.trim()) errors.email = 'Please enter an email address.'
  return errors
}

async function sendInvite() {
  if (invite.saving) return
  invite.error = ''
  invite.errors = validateInvite()
  if (Object.keys(invite.errors).length > 0) return

  invite.saving = true
  try {
    created.value = await inviteMember(props.roomId, {
      email: invite.email.trim(),
      fullName: invite.fullName.trim(),
    })
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.status === 409) {
        invite.errors = { email: e.message }
      } else if (e.status === 400) {
        invite.errors = e.fieldErrors
        invite.error = 'Please check the highlighted fields.'
      } else {
        invite.error = e.message
      }
    } else {
      invite.error = 'Something went wrong. Please try again.'
    }
  } finally {
    invite.saving = false
  }
}

// When the dialog closes after an invitation was created (Done button or Escape), refresh the list.
watch(
  () => invite.open,
  (isOpen) => {
    if (!isOpen && created.value) {
      notice.value = `Invitation created for ${created.value.email}.`
      created.value = null
      invitations.reload()
    }
  },
)

// ---------- loading ----------
onMounted(() => {
  window.addEventListener('beforeunload', warnBeforeUnload)
  loadRoom()
  loadRoster()
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeUnload))

watch(
  () => props.roomId,
  () => {
    notice.value = ''
    loadRoom()
    loadRoster()
    invitations.goTo(0)
  },
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <p class="sr-only" aria-live="polite">{{ announcement }}</p>

    <RouterLink
      :to="{ name: 'leader-rooms' }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← My Rooms
    </RouterLink>

    <p v-if="roomState === 'loading'" role="status" class="p-6 text-center text-sm text-muted">Loading room…</p>

    <div v-else-if="roomState === 'notfound'" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room could not be found.</p>
      <p class="mt-1 text-sm text-muted">Go back to My Rooms and choose one of your rooms.</p>
    </div>

    <div v-else-if="roomState === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">The room could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="loadRoom()">Try again</button>
    </div>

    <template v-else>
      <section class="bg-room-card rounded-[18px] p-5 text-white">
        <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">Your room</p>
        <div class="mt-2 flex flex-wrap items-start justify-between gap-3">
          <RoomTitle
            :church-name="room.churchName"
            :room-name="room.name"
            :leader-name="room.leaderName"
            :level="1"
            tone="dark"
          />
          <StatusPill :status="room.status" />
        </div>
        <p class="mt-2 text-sm text-on-navy-soft">{{ memberLabel(draft.length) }}</p>
        <RouterLink :to="{ name: 'leader-devotions', params: { roomId: room.id } }" :class="[btnPrimary, 'mt-4']">
          Devotions
        </RouterLink>
      </section>

      <p v-if="!editable" role="status" class="rounded-xl border border-line bg-white px-4 py-3 text-sm text-muted">
        This room is archived, so it can’t be changed. Restore it from My Rooms to add people or change the order.
      </p>

      <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
        {{ notice }}
      </p>

      <!-- Participation order -->
      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="order-title">
        <div class="space-y-3 border-b border-line p-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 id="order-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Participation order</h2>
            <div v-if="editable" class="flex flex-wrap gap-2">
              <button type="button" :class="btnPrimary" :disabled="dirty" @click="addOpen = true">Add from church</button>
              <button type="button" :class="btnOutline" @click="openInvite">Invite new person</button>
            </div>
          </div>
          <p class="text-sm text-muted">Members take their turn in this order. New people join at the end.</p>
        </div>

        <div v-if="dirty" role="status" class="flex flex-wrap items-center gap-3 border-b border-line bg-gold-50 px-4 py-3">
          <p class="min-w-0 flex-1 text-sm font-semibold text-navy-900">The new order is not saved yet.</p>
          <button type="button" :class="btnOutline" :disabled="order.saving" @click="discard">Discard changes</button>
          <button type="button" :class="btnPrimary" :disabled="order.saving" @click="saveDraft">
            {{ order.saving ? 'Saving…' : 'Save order' }}
          </button>
        </div>

        <p v-if="order.error" role="alert" class="border-b border-line bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ order.error }}
        </p>

        <ListState
          :loading="rosterLoading"
          :error="rosterError"
          :count="draft.length"
          empty-message="No members yet. Add someone from your church or invite a new person."
          @retry="loadRoster"
        >
          <ol aria-label="Participation order" class="divide-y divide-line">
            <li v-for="(member, index) in draft" :key="member.memberId" class="flex flex-wrap items-center gap-3 px-4 py-3">
              <ParticipationBadge :number="index + 1" />
              <div class="min-w-0 flex-1">
                <p data-member-name class="truncate font-semibold text-navy-900">{{ member.fullName }}</p>
                <p class="truncate text-sm text-muted">{{ member.email }}</p>
              </div>
              <StatusPill v-if="member.accountStatus !== 'ACTIVE'" status="DISABLED" />
              <div v-if="editable" class="flex gap-2">
                <button
                  :id="`move-up-${member.memberId}`"
                  type="button"
                  :class="btnOutline"
                  :aria-label="`Move ${member.fullName} up`"
                  :disabled="index === 0 || order.saving"
                  @click="move(index, -1)"
                >
                  <span aria-hidden="true">↑</span>
                </button>
                <button
                  :id="`move-down-${member.memberId}`"
                  type="button"
                  :class="btnOutline"
                  :aria-label="`Move ${member.fullName} down`"
                  :disabled="index === draft.length - 1 || order.saving"
                  @click="move(index, 1)"
                >
                  <span aria-hidden="true">↓</span>
                </button>
                <button type="button" :class="btnOutline" :disabled="dirty || order.saving" @click="askRemove(member)">
                  Remove<span class="sr-only">{{ ` ${member.fullName}` }}</span>
                </button>
              </div>
            </li>
          </ol>
        </ListState>

        <p v-if="dirty && editable" class="border-t border-line px-4 py-3 text-sm text-muted">
          Save or discard your order changes to add or remove members.
        </p>
      </section>

      <!-- Invitations -->
      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="invitations-title">
        <div class="border-b border-line p-4">
          <h2 id="invitations-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">
            Invitations to this room
          </h2>
        </div>

        <ListState
          :loading="invitations.loading"
          :error="invitations.error"
          :count="invitations.items.length"
          empty-message="No invitations yet."
          @retry="invitations.reload"
        >
          <ul class="divide-y divide-line">
            <li v-for="invitation in invitations.items" :key="invitation.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
              <div class="min-w-0 flex-1">
                <p class="truncate font-semibold text-navy-900">{{ invitation.fullName }}</p>
                <p class="truncate text-sm text-muted">{{ invitation.email }}</p>
                <p class="text-sm text-muted">
                  Invited {{ formatDate(invitation.createdAt) }}
                  <template v-if="invitation.status === 'PENDING'"> · expires {{ formatDate(invitation.expiresAt) }}</template>
                </p>
              </div>
              <StatusPill :status="invitation.status" />
              <button v-if="invitation.status === 'PENDING'" type="button" :class="btnOutline" @click="askRevoke(invitation)">
                Revoke<span class="sr-only"> invitation for {{ invitation.email }}</span>
              </button>
            </li>
          </ul>
        </ListState>

        <PaginationBar
          :page="invitations.page"
          :total-pages="invitations.totalPages"
          :total-elements="invitations.totalElements"
          :loading="invitations.loading"
          @change="invitations.goTo"
        />
      </section>
    </template>

    <!-- Add from the church's members -->
    <BaseDialog v-model:open="addOpen" title="Add from your church">
      <AvailableMembersPanel :room-id="roomId" @added="loadRoster" />
      <div class="mt-6 flex justify-end">
        <button type="button" :class="btnPrimary" @click="addOpen = false">Done</button>
      </div>
    </BaseDialog>

    <!-- Invite a new person: the form first, then the one-time link -->
    <BaseDialog v-model:open="invite.open" :title="created ? 'Invitation link ready' : 'Invite a new person'">
      <InviteLinkPanel
        v-if="created"
        :url="created.inviteUrl"
        :email="created.email"
        :expires-at="created.expiresAt"
        @done="invite.open = false"
      />

      <form v-else class="space-y-5" novalidate @submit.prevent="sendInvite">
        <p class="text-sm text-muted">
          They get an account and join this room when they open the link. If they already have an account,
          use “Add from church” instead.
        </p>

        <p v-if="invite.error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ invite.error }}
        </p>

        <TextField v-model="invite.fullName" label="Name" :error="invite.errors.fullName" maxlength="150" autocomplete="off" required />
        <TextField v-model="invite.email" label="Email" type="email" :error="invite.errors.email" maxlength="255" autocomplete="off" required />

        <div class="flex justify-end gap-3">
          <button type="button" :class="btnOutline" @click="invite.open = false">Cancel</button>
          <button type="submit" :class="btnPrimary" :disabled="invite.saving">
            {{ invite.saving ? 'Creating…' : 'Create invitation' }}
          </button>
        </div>
      </form>
    </BaseDialog>

    <ConfirmDialog
      v-model:open="removal.open"
      :title="`Remove ${removal.target?.fullName ?? ''} from this room?`"
      message="Their account and earlier history are kept, and you can add them again later (at the end of the order). Everyone after them moves up one place."
      confirm-label="Remove"
      :busy="removal.busy"
      :error="removal.error"
      @confirm="confirmRemove"
    />

    <ConfirmDialog
      v-model:open="revoke.open"
      title="Revoke this invitation?"
      :message="`The link sent to ${revoke.target?.email ?? ''} will stop working.`"
      confirm-label="Revoke"
      :busy="revoke.busy"
      :error="revoke.error"
      @confirm="confirmRevoke"
    />
  </div>
</template>
