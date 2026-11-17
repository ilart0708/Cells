<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { invitePastor, listInvitations, revokeInvitation } from '../../api/adminInvitations'
import { listChurches } from '../../api/churches'
import { ApiError } from '../../api/http'
import { listPastors, setPastorStatus } from '../../api/pastors'
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

// ---------- pastors ----------
const pastors = reactive(usePagedList(listPastors))
const toggle = reactive(
  useStatusToggle(setPastorStatus, async (pastor, status) => {
    notice.value = `${pastor.fullName} is now ${status === 'ACTIVE' ? 'active' : 'disabled'}.`
    await pastors.reload()
  }),
)

// ---------- invitations ----------
const invitations = reactive(usePagedList(listInvitations))

const revoke = reactive({ open: false, target: null, busy: false, error: '' })

function askRevoke(invitation) {
  Object.assign(revoke, { open: true, target: invitation, busy: false, error: '' })
}

async function confirmRevoke() {
  if (revoke.busy) return
  revoke.busy = true
  revoke.error = ''
  try {
    await revokeInvitation(revoke.target.id)
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
const churches = ref([])
const churchesError = ref('')
const created = ref(null)

const invite = reactive({
  open: false,
  fullName: '',
  email: '',
  churchId: '',
  saving: false,
  error: '',
  errors: {},
})

onMounted(async () => {
  try {
    // The backend caps pages at 100. Past 100 churches this needs a search-as-you-type picker.
    churches.value = (await listChurches({ size: 100 })).content
  } catch {
    churchesError.value = 'The church list could not be loaded.'
  }
})

function openInvite() {
  created.value = null
  Object.assign(invite, { open: true, fullName: '', email: '', churchId: '', saving: false, error: '', errors: {} })
}

function validate() {
  const errors = {}
  if (!invite.fullName.trim()) errors.fullName = 'Please enter the pastor’s name.'
  if (!invite.email.trim()) errors.email = 'Please enter an email address.'
  if (!invite.churchId) errors.churchId = 'Please choose a church.'
  return errors
}

async function sendInvite() {
  if (invite.saving) return
  invite.error = ''
  invite.errors = validate()
  if (Object.keys(invite.errors).length > 0) return

  invite.saving = true
  try {
    created.value = await invitePastor({
      email: invite.email.trim(),
      fullName: invite.fullName.trim(),
      churchId: invite.churchId,
    })
  } catch (e) {
    if (e instanceof ApiError) {
      invite.errors = e.fieldErrors
      if (e.status === 409) invite.errors = { email: e.message }
      if (Object.keys(invite.errors).length === 0) invite.error = e.message
      else if (e.status === 400) invite.error = 'Please check the highlighted fields.'
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
      <h1 class="text-2xl font-bold text-navy-900">Pastor Management</h1>
      <button type="button" :class="btnPrimary" @click="openInvite">Invite pastor</button>
    </header>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>

    <!-- Pastors -->
    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="pastors-title">
      <div class="space-y-4 border-b border-line p-4">
        <h2 id="pastors-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Pastors</h2>
        <TextField v-model="pastors.query" label="Search pastors" type="search" />
      </div>

      <ListState
        :loading="pastors.loading"
        :error="pastors.error"
        :count="pastors.items.length"
        :empty-message="pastors.query ? 'No pastors match your search.' : 'No pastors yet. Invite your first pastor to get started.'"
        @retry="pastors.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="pastor in pastors.items" :key="pastor.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ pastor.fullName }}</p>
              <p class="truncate text-sm text-muted">{{ pastor.email }} · {{ pastor.churchName }}</p>
            </div>
            <StatusPill :status="pastor.status" />
            <button type="button" :class="btnOutline" @click="toggle.ask(pastor)">
              {{ pastor.status === 'ACTIVE' ? 'Disable' : 'Enable' }}<span class="sr-only"> {{ pastor.fullName }}</span>
            </button>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="pastors.page"
        :total-pages="pastors.totalPages"
        :total-elements="pastors.totalElements"
        :loading="pastors.loading"
        @change="pastors.goTo"
      />
    </section>

    <!-- Invitations -->
    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-labelledby="invitations-title">
      <div class="border-b border-line p-4">
        <h2 id="invitations-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">Invitations</h2>
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
              <p class="truncate text-sm text-muted">{{ invitation.email }} · {{ invitation.churchName }}</p>
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
    <BaseDialog v-model:open="invite.open" :title="created ? 'Invitation link ready' : 'Invite a pastor'">
      <InviteLinkPanel
        v-if="created"
        :url="created.inviteUrl"
        :email="created.email"
        :expires-at="created.expiresAt"
        @done="invite.open = false"
      />

      <p v-else-if="churchesError" role="alert" class="text-sm font-semibold text-red-700">{{ churchesError }}</p>

      <p v-else-if="churches.length === 0" class="text-sm text-muted">
        A pastor needs a church. Please
        <RouterLink :to="{ name: 'admin-churches' }" class="font-bold text-navy-900 underline underline-offset-4">add a church</RouterLink>
        first.
      </p>

      <form v-else class="space-y-5" novalidate @submit.prevent="sendInvite">
        <p v-if="invite.error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ invite.error }}
        </p>

        <TextField v-model="invite.fullName" label="Pastor’s name" :error="invite.errors.fullName" maxlength="150" autocomplete="off" required />
        <TextField v-model="invite.email" label="Email" type="email" :error="invite.errors.email" maxlength="255" autocomplete="off" required />

        <div>
          <label for="invite-church" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">Church</label>
          <select
            id="invite-church"
            v-model.number="invite.churchId"
            required
            :aria-invalid="invite.errors.churchId ? 'true' : 'false'"
            aria-describedby="invite-church-error"
            class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
          >
            <option value="" disabled>Choose a church</option>
            <option v-for="church in churches" :key="church.id" :value="church.id">{{ church.name }}</option>
          </select>
          <p v-if="invite.errors.churchId" id="invite-church-error" class="mt-1 text-sm text-red-700">
            {{ invite.errors.churchId }}
          </p>
        </div>

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
