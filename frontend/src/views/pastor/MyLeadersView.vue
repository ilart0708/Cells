<script setup>
import { reactive, ref, watch } from 'vue'
import { ApiError } from '../../api/http'
import {
  inviteLeader,
  listMyInvitations,
  listMyLeaders,
  revokeMyInvitation,
  setMyLeaderStatus,
} from '../../api/pastorArea'
import BaseDialog from '../../components/BaseDialog.vue'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import InviteLinkPanel from '../../components/InviteLinkPanel.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import StatusConfirmDialog from '../../components/StatusConfirmDialog.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { useStatusToggle } from '../../composables/useStatusToggle'
import { formatDate } from '../../utils/format'

const notice = ref('')

// ---------- my leaders ----------
const leaders = reactive(usePagedList(listMyLeaders))
const toggle = reactive(
  useStatusToggle(setMyLeaderStatus, async (leader, status) => {
    notice.value = `${leader.fullName} is now ${status === 'ACTIVE' ? 'active' : 'disabled'}.`
    await leaders.reload()
  }),
)

// ---------- my invitations ----------
const invitations = reactive(usePagedList(listMyInvitations))
const revoke = reactive({ open: false, target: null, busy: false, error: '' })

function askRevoke(invitation) {
  Object.assign(revoke, { open: true, target: invitation, busy: false, error: '' })
}

async function confirmRevoke() {
  if (revoke.busy) return
  revoke.busy = true
  revoke.error = ''
  try {
    await revokeMyInvitation(revoke.target.id)
    revoke.open = false
    notice.value = `The invitation for ${revoke.target.email} was revoked.`
    await invitations.reload()
  } catch (e) {
    revoke.error = e?.message || 'Something went wrong. Please try again.'
  } finally {
    revoke.busy = false
  }
}

// ---------- invite dialog ----------
const created = ref(null)
const invite = reactive({ open: false, fullName: '', email: '', saving: false, error: '', errors: {} })

function openInvite() {
  created.value = null
  Object.assign(invite, { open: true, fullName: '', email: '', saving: false, error: '', errors: {} })
}

function validate() {
  const errors = {}
  if (!invite.fullName.trim()) errors.fullName = 'Please enter the leader’s name.'
  if (!invite.email.trim()) errors.email = 'Please enter an email address.'
  return errors
}

async function sendInvite() {
  if (invite.saving) return
  invite.error = ''
  invite.errors = validate()
  if (Object.keys(invite.errors).length > 0) return

  invite.saving = true
  try {
    created.value = await inviteLeader({
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
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold text-navy-900">My Cell Group Leaders</h1>
      <button type="button" :class="btnPrimary" @click="openInvite">Invite leader</button>
    </header>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>

    <!-- Leaders -->
    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="leaders-title">
      <div class="space-y-4 border-b border-line p-4">
        <h2 id="leaders-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Leaders</h2>
        <TextField v-model="leaders.query" label="Search leaders" type="search" />
      </div>

      <ListState
        :loading="leaders.loading"
        :error="leaders.error"
        :count="leaders.items.length"
        :empty-message="leaders.query ? 'No leaders match your search.' : 'You have no leaders yet. Invite your first leader to get started.'"
        @retry="leaders.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="leader in leaders.items" :key="leader.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ leader.fullName }}</p>
              <p class="truncate text-sm text-muted">{{ leader.email }} · {{ leader.churchName }}</p>
            </div>
            <StatusPill :status="leader.status" />
            <button type="button" :class="btnOutline" @click="toggle.ask(leader)">
              {{ leader.status === 'ACTIVE' ? 'Disable' : 'Enable' }}<span class="sr-only"> {{ leader.fullName }}</span>
            </button>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="leaders.page"
        :total-pages="leaders.totalPages"
        :total-elements="leaders.totalElements"
        :loading="leaders.loading"
        @change="leaders.goTo"
      />
    </section>

    <!-- Invitations -->
    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="invitations-title">
      <div class="border-b border-line p-4">
        <h2 id="invitations-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">My invitations</h2>
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

    <!-- Invite dialog: the form first, then the one-time link -->
    <BaseDialog v-model:open="invite.open" :title="created ? 'Invitation link ready' : 'Invite a leader'">
      <InviteLinkPanel
        v-if="created"
        :url="created.inviteUrl"
        :email="created.email"
        :expires-at="created.expiresAt"
        @done="invite.open = false"
      />

      <form v-else class="space-y-5" novalidate @submit.prevent="sendInvite">
        <p class="text-sm text-muted">The leader will join your church automatically.</p>

        <p v-if="invite.error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ invite.error }}
        </p>

        <TextField v-model="invite.fullName" label="Leader’s name" :error="invite.errors.fullName" maxlength="150" autocomplete="off" required />
        <TextField v-model="invite.email" label="Email" type="email" :error="invite.errors.email" maxlength="255" autocomplete="off" required />

        <div class="flex justify-end gap-3">
          <button type="button" :class="btnOutline" @click="invite.open = false">Cancel</button>
          <button type="submit" :class="btnPrimary" :disabled="invite.saving">
            {{ invite.saving ? 'Creating…' : 'Create invitation' }}
          </button>
        </div>
      </form>
    </BaseDialog>

    <StatusConfirmDialog
      v-model:open="toggle.open"
      :name="toggle.target?.fullName ?? ''"
      :next-status="toggle.nextStatus"
      :busy="toggle.busy"
      :error="toggle.error"
      @confirm="toggle.confirm"
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
