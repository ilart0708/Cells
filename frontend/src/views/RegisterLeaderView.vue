<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { COMMUNITY_RULES } from '../constants/communityRules'
import { ROLE_HOME } from '../constants/roles'
import { useAuthStore } from '../stores/auth'

const PASSWORD_MIN = 12
const PASSWORD_MAX = 72

const inputClass =
  'mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500'
const labelClass = 'block text-[13px] font-bold uppercase tracking-[1px] text-muted'

const router = useRouter()
const auth = useAuthStore()

const fullName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const churchName = ref('')
const pastorName = ref('')
const pastorEmail = ref('')
const acceptRules = ref(false)

const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

function validate() {
  const errors = {}
  if (!fullName.value.trim()) errors.fullName = 'Please enter your name.'
  if (!email.value.trim()) errors.email = 'Please enter your email.'
  if (password.value.length < PASSWORD_MIN || password.value.length > PASSWORD_MAX) {
    errors.password = `Password must be ${PASSWORD_MIN} to ${PASSWORD_MAX} characters.`
  }
  if (confirmPassword.value !== password.value) {
    errors.confirmPassword = 'The two passwords do not match.'
  }
  if (!churchName.value.trim()) errors.churchName = 'Please enter your church’s name.'
  if (!pastorName.value.trim()) errors.pastorName = 'Please enter your pastor’s name.'
  if (!acceptRules.value) errors.acceptRules = 'Please agree to the community rules to continue.'
  return errors
}

async function submit() {
  if (submitting.value) return

  errorMessage.value = ''
  fieldErrors.value = validate()
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    await auth.registerLeader({
      fullName: fullName.value.trim(),
      email: email.value.trim(),
      password: password.value,
      churchName: churchName.value.trim(),
      pastorName: pastorName.value.trim(),
      pastorEmail: pastorEmail.value.trim() || null,
      acceptRules: acceptRules.value,
    })
    await router.replace({ name: ROLE_HOME[auth.role] })
  } catch (error) {
    if (error instanceof ApiError) {
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
      <p class="mt-2 text-center text-sm text-on-navy-soft">
        For a cell group leader whose church has not invited them yet.
      </p>

      <div
        class="mt-8 overflow-hidden rounded-[18px] border border-line bg-white shadow-[0_40px_90px_rgba(0,0,0,.55)]"
      >
        <div class="bg-room-card p-5 text-white">
          <p class="text-xs font-bold uppercase tracking-[2px] text-gold-500">New group</p>
          <h1 class="mt-1 text-xl font-bold">Create your group</h1>
          <p class="mt-1 text-sm text-on-navy-soft">
            Your church and pastor are shown as “not verified” until an administrator verifies them.
            Until then you can have up to 3 rooms and 30 members.
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
            <label for="email" :class="labelClass">Your email</label>
            <input
              id="email"
              v-model="email"
              type="email"
              autocomplete="username"
              maxlength="255"
              required
              :aria-invalid="fieldErrors.email ? 'true' : 'false'"
              :aria-describedby="fieldErrors.email ? 'email-error' : undefined"
              :class="inputClass"
            />
            <p v-if="fieldErrors.email" id="email-error" class="mt-1 text-sm text-red-700">
              {{ fieldErrors.email }}
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

          <div>
            <label for="churchName" :class="labelClass">Your church</label>
            <input
              id="churchName"
              v-model="churchName"
              type="text"
              maxlength="150"
              required
              :aria-invalid="fieldErrors.churchName ? 'true' : 'false'"
              :aria-describedby="fieldErrors.churchName ? 'churchName-error' : undefined"
              :class="inputClass"
            />
            <p v-if="fieldErrors.churchName" id="churchName-error" class="mt-1 text-sm text-red-700">
              {{ fieldErrors.churchName }}
            </p>
          </div>

          <div>
            <label for="pastorName" :class="labelClass">Your pastor’s name</label>
            <input
              id="pastorName"
              v-model="pastorName"
              type="text"
              maxlength="150"
              required
              :aria-invalid="fieldErrors.pastorName ? 'true' : 'false'"
              :aria-describedby="fieldErrors.pastorName ? 'pastorName-error' : undefined"
              :class="inputClass"
            />
            <p v-if="fieldErrors.pastorName" id="pastorName-error" class="mt-1 text-sm text-red-700">
              {{ fieldErrors.pastorName }}
            </p>
          </div>

          <div>
            <label for="pastorEmail" :class="labelClass">
              Pastor’s email <span class="font-semibold normal-case tracking-normal">(optional)</span>
            </label>
            <input
              id="pastorEmail"
              v-model="pastorEmail"
              type="email"
              maxlength="255"
              :aria-invalid="fieldErrors.pastorEmail ? 'true' : 'false'"
              aria-describedby="pastorEmail-hint pastorEmail-error"
              :class="inputClass"
            />
            <p id="pastorEmail-hint" class="mt-1 text-sm text-muted">
              Helps us confirm with your pastor later. It is only shown to the administrator.
            </p>
            <p
              v-if="fieldErrors.pastorEmail"
              id="pastorEmail-error"
              class="mt-1 text-sm text-red-700"
            >
              {{ fieldErrors.pastorEmail }}
            </p>
          </div>

          <section aria-labelledby="rules-heading" class="rounded-xl border border-line bg-app-bg p-4">
            <h2 id="rules-heading" class="text-[13px] font-bold uppercase tracking-[1px] text-muted">
              Community rules
            </h2>
            <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-navy-900">
              <li v-for="rule in COMMUNITY_RULES" :key="rule">{{ rule }}</li>
            </ul>
            <label class="mt-3 flex min-h-11 items-start gap-3 text-sm font-semibold text-navy-900">
              <input
                v-model="acceptRules"
                type="checkbox"
                class="mt-0.5 size-5 shrink-0 accent-navy-900"
                :aria-invalid="fieldErrors.acceptRules ? 'true' : 'false'"
                :aria-describedby="fieldErrors.acceptRules ? 'acceptRules-error' : undefined"
              />
              I agree to these rules.
            </label>
            <p v-if="fieldErrors.acceptRules" id="acceptRules-error" class="mt-1 text-sm text-red-700">
              {{ fieldErrors.acceptRules }}
            </p>
          </section>

          <button
            type="submit"
            :disabled="submitting"
            class="min-h-12 w-full rounded-xl bg-gold-500 px-4 text-base font-bold text-navy-900 hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {{ submitting ? 'Creating your group…' : 'Create my group' }}
          </button>

          <p class="text-center text-sm text-muted">
            Already have an account or an invitation?
            <RouterLink
              :to="{ name: 'login' }"
              class="inline-flex min-h-11 items-center font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
            >
              Sign in
            </RouterLink>
          </p>
        </form>
      </div>
    </div>
  </main>
</template>
