<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteAccount } from '../api/account'
import { ApiError } from '../api/http'
import BaseDialog from '../components/BaseDialog.vue'
import TextField from '../components/TextField.vue'
import { btnOutline, btnSecondary } from '../components/buttonClasses'
import { ROLE_LABEL } from '../constants/roles'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const open = ref(false)
const password = ref('')
const busy = ref(false)
const error = ref('')

// What gets removed depends on the role; the backend enforces the same rules.
const notes = {
  MEMBER: 'You will leave your rooms. Your answers and experience points are deleted, so your group’s participation counts change.',
  LEADER: 'You must archive all your active rooms first. Your group summaries and reflections are deleted.',
  PASTOR: 'Cell group leaders assigned to you must be moved to another pastor first, by an administrator.',
}

function begin() {
  password.value = ''
  error.value = ''
  open.value = true
}

async function confirm() {
  if (busy.value) return
  if (!password.value) {
    error.value = 'Enter your password to confirm.'
    return
  }
  busy.value = true
  error.value = ''
  try {
    await deleteAccount(password.value)
    open.value = false
    await auth.logout() // clears the saved session, and on the app the phone registration
    await router.push({ name: 'login', query: { deleted: '1' } })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <header>
      <h1 class="text-2xl font-bold text-navy-900">Account</h1>
      <p class="mt-1 text-sm text-muted">Your sign-in details and your privacy choices.</p>
    </header>

    <section class="rounded-[18px] border border-line bg-white p-5">
      <p class="text-xs font-bold uppercase tracking-[1px] text-muted">Name</p>
      <p class="mt-1 font-semibold text-navy-900">{{ auth.user.fullName }}</p>
      <p class="mt-4 text-xs font-bold uppercase tracking-[1px] text-muted">Email</p>
      <p class="mt-1 font-semibold text-navy-900">{{ auth.user.email }}</p>
      <p class="mt-4 text-xs font-bold uppercase tracking-[1px] text-muted">Role</p>
      <p class="mt-1 font-semibold text-navy-900">{{ ROLE_LABEL[auth.role] }}</p>
    </section>

    <section class="space-y-3 rounded-[18px] border border-line bg-white p-5">
      <h2 class="text-lg font-bold text-navy-900">Delete my account</h2>
      <p class="text-sm text-muted">
        This permanently removes your name, email, password, what you wrote, your experience points, your notifications
        and your phone registrations. It cannot be undone.
      </p>
      <p class="rounded-xl bg-gold-50 px-4 py-3 text-sm text-navy-900">{{ notes[auth.role] }}</p>
      <button type="button" :class="btnSecondary" @click="begin">Delete my account</button>
    </section>

    <BaseDialog v-model:open="open" title="Delete your account?">
      <form class="space-y-4" @submit.prevent="confirm">
        <p class="text-sm text-muted">Enter your password to confirm. You will be signed out straight away.</p>
        <TextField v-model="password" label="Password" type="password" autocomplete="current-password" maxlength="72" />
        <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ error }}
        </p>
        <div class="flex justify-end gap-3">
          <button type="button" :class="btnOutline" @click="open = false">Cancel</button>
          <button type="submit" :class="btnSecondary" :disabled="busy">
            {{ busy ? 'Deleting…' : 'Delete permanently' }}
          </button>
        </div>
      </form>
    </BaseDialog>
  </div>
</template>
