<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { previewInvitation } from '../api/invitations'
import { ROLE_HOME, ROLE_LABEL } from '../constants/roles'
import { useAuthStore } from '../stores/auth'
import RoomTitle from '../components/RoomTitle.vue'

const PASSWORD_MIN = 12
const PASSWORD_MAX = 72

const inputClass =
  'mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500'
const readonlyClass =
  'mt-2 block w-full rounded-xl border border-line bg-app-bg px-4 py-3.5 text-base text-muted outline-none'
const labelClass = 'block text-[13px] font-bold uppercase tracking-[1px] text-muted'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// loading | ready | invalid | unavailable
const state = ref('loading')
const token = ref('')
const invitation = ref(null)

const fullName = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

function readToken() {
  return new URLSearchParams(route.hash.replace(/^#/, '')).get('token') ?? ''
}

async function loadInvitation() {
  state.value = 'loading'
  try {
    invitation.value = await previewInvitation(token.value)
    fullName.value = invitation.value.fullName
    state.value = 'ready'
  } catch (error) {
    const linkIsBad = error instanceof ApiError && [400, 410].includes(error.status)
    state.value = linkIsBad ? 'invalid' : 'unavailable'
  }
}

onMounted(async () => {
  token.value = readToken()
  if (!token.value) {
    state.value = 'invalid'
    return
  }
  // From here on the token lives only in memory: drop it from the address bar and history.
  await router.replace({ name: 'accept-invitation' })
  await loadInvitation()
})

function validate() {
  const errors = {}
  if (!fullName.value.trim()) {
    errors.fullName = 'Please enter your name.'
  }
  if (password.value.length < PASSWORD_MIN || password.value.length > PASSWORD_MAX) {
    errors.password = `Password must be ${PASSWORD_MIN} to ${PASSWORD_MAX} characters.`
  }
  if (confirmPassword.value !== password.value) {
    errors.confirmPassword = 'The two passwords do not match.'
  }
  return errors
}

async function submit() {
  if (submitting.value) return

  errorMessage.value = ''
  fieldErrors.value = validate()
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    await auth.acceptInvitation(token.value, fullName.value.trim(), password.value)
    await router.replace({ name: ROLE_HOME[auth.role] })
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.status === 410) {
        state.value = 'invalid' // used, revoked or expired while the form was open
        return
      }
      fieldErrors.value = error.fieldErrors
      errorMessage.value =
        error.status === 400 ? 'Please check the highlighted fields.' : error.message
    } else {
      errorMessage.value = 'Something went wrong. Please try again.'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="bg-brand-backdrop flex min-h-screen items-center justify-center px-4 py-10">
    <div class="w-full max-w-sm">
      <p class="text-center font-heading text-[28px] font-extrabold uppercase tracking-[6px] text-white">
        CELLS
      </p>

      <div
        class="mt-8 overflow-hidden rounded-[18px] border border-line bg-white shadow-[0_40px_90px_rgba(0,0,0,.55)]"
      >
        <p v-if="state === 'loading'" role="status" class="p-6 text-center text-sm text-muted">
          Checking your invitation…
        </p>

        <div v-else-if="state === 'invalid'" class="space-y-4 p-6">
          <h1 class="text-xl font-bold text-navy-900">Invitation not available</h1>
          <p class="text-sm text-muted">
            This invitation link is invalid or has expired. Please ask the person who invited you to
            send a new one.
          </p>
          <RouterLink
            :to="{ name: 'login' }"
            class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
          >
            Go to sign in
          </RouterLink>
        </div>

        <div v-else-if="state === 'unavailable'" class="space-y-4 p-6">
          <h1 class="text-xl font-bold text-navy-900">We couldn’t check your invitation</h1>
          <p class="text-sm text-muted">
            The server could not be reached. Please check your connection and try again.
          </p>
          <button
            type="button"
            class="min-h-12 w-full rounded-xl bg-navy-900 px-4 text-base font-bold text-white hover:bg-navy-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
            @click="loadInvitation"
          >
            Try again
          </button>
        </div>

        <template v-else>
          <div class="bg-room-card p-5 text-white">
            <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">You’re invited</p>

            <!-- A member invitation names the room and its leader -->
            <div v-if="invitation.roomName && invitation.leaderName" class="mt-2">
              <RoomTitle
                :church-name="invitation.churchName"
                :room-name="invitation.roomName"
                :leader-name="invitation.leaderName"
                :level="1"
                tone="dark"
              />
            </div>
            <h1 v-else class="mt-1 text-xl font-bold">{{ invitation.churchName }}</h1>

            <p class="mt-1 text-sm text-on-navy-soft">
              Joining as {{ ROLE_LABEL[invitation.role] }}
            </p>
          </div>

          <form class="space-y-5 p-6" novalidate @submit.prevent="submit">
            <p
              v-if="errorMessage"
              role="alert"
              class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700"
            >
              {{ errorMessage }}
            </p>

            <div>
              <label for="email" :class="labelClass">Email</label>
              <input
                id="email"
                type="email"
                :value="invitation.email"
                readonly
                autocomplete="username"
                :class="readonlyClass"
              />
            </div>

            <div>
              <label for="fullName" :class="labelClass">Your name</label>
              <input
                id="fullName"
                v-model="fullName"
                type="text"
                autocomplete="name"
                maxlength="150"
                required
                :aria-invalid="fieldErrors.fullName ? 'true' : 'false'"
                :aria-describedby="fieldErrors.fullName ? 'fullName-error' : undefined"
                :class="inputClass"
              />
              <p v-if="fieldErrors.fullName" id="fullName-error" class="mt-1 text-sm text-red-700">
                {{ fieldErrors.fullName }}
              </p>
            </div>

            <div>
              <label for="password" :class="labelClass">Choose a password</label>
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                :maxlength="PASSWORD_MAX"
                required
                :aria-invalid="fieldErrors.password ? 'true' : 'false'"
                aria-describedby="password-hint password-error"
                :class="inputClass"
              />
              <p id="password-hint" class="mt-1 text-sm text-muted">
                At least {{ PASSWORD_MIN }} characters. A short sentence you will remember works well.
              </p>
              <p v-if="fieldErrors.password" id="password-error" class="mt-1 text-sm text-red-700">
                {{ fieldErrors.password }}
              </p>
            </div>

            <div>
              <label for="confirmPassword" :class="labelClass">Repeat the password</label>
              <input
                id="confirmPassword"
                v-model="confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                :maxlength="PASSWORD_MAX"
                required
                :aria-invalid="fieldErrors.confirmPassword ? 'true' : 'false'"
                :aria-describedby="fieldErrors.confirmPassword ? 'confirmPassword-error' : undefined"
                :class="inputClass"
              />
              <p
                v-if="fieldErrors.confirmPassword"
                id="confirmPassword-error"
                class="mt-1 text-sm text-red-700"
              >
                {{ fieldErrors.confirmPassword }}
              </p>
            </div>

            <label class="flex min-h-11 items-center gap-3 text-sm text-muted">
              <input v-model="showPassword" type="checkbox" class="size-5 accent-navy-900" />
              Show passwords
            </label>

            <button
              type="submit"
              :disabled="submitting"
              class="min-h-12 w-full rounded-xl bg-gold-500 px-4 text-base font-bold text-navy-900 hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {{ submitting ? 'Creating account…' : 'Create account' }}
            </button>
          </form>
        </template>
      </div>
    </div>
  </main>
</template>
