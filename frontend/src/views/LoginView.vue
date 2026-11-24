<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ApiError } from '../api/http'
import { ROLE_HOME } from '../constants/roles'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

// Only allow redirects to paths inside this app (prevents open-redirect tricks).
// If the target belongs to another role, the router guard sends the user to their own area.
function redirectTarget() {
  const target = route.query.redirect
  const safe = typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')
  return safe ? target : { name: ROLE_HOME[auth.role] }
}

async function submit() {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  fieldErrors.value = {}

  try {
    await auth.login(email.value, password.value)
    await router.push(redirectTarget())
  } catch (error) {
    if (error instanceof ApiError) {
      fieldErrors.value = error.fieldErrors
      errorMessage.value =
        error.status === 400 ? 'Please check the highlighted fields.' : error.message
    } else {
      errorMessage.value = 'Something went wrong. Please try again.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="bg-brand-backdrop flex min-h-screen items-center justify-center px-4 py-10">
    <div class="w-full max-w-sm">
      <h1 class="text-center font-heading text-[28px] font-extrabold uppercase tracking-[6px] text-white">
        CELLS
      </h1>
      <p class="mt-2 text-center text-sm text-on-navy-soft">
        Grow Together. Share the Word. Build a Stronger Community.
      </p>

      <form
        class="mt-8 space-y-5 rounded-[18px] border border-line bg-white p-6 shadow-[0_40px_90px_rgba(0,0,0,.55)]"
        @submit.prevent="submit"
      >
        <p
          v-if="route.query.deleted"
          role="status"
          class="rounded-xl bg-gold-100 px-4 py-3 text-sm font-semibold text-navy-900"
        >
          Your account has been deleted.
        </p>
        <p
          v-if="errorMessage"
          role="alert"
          class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700"
        >
          {{ errorMessage }}
        </p>

        <div>
          <label for="email" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
            Email
          </label>
          <input
            id="email"
            v-model="email"
            type="email"
            autocomplete="username"
            required
            :aria-invalid="fieldErrors.email ? 'true' : 'false'"
            class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
          />
          <p v-if="fieldErrors.email" class="mt-1 text-sm text-red-700">{{ fieldErrors.email }}</p>
        </div>

        <div>
          <label for="password" class="block text-[13px] font-bold uppercase tracking-[1px] text-muted">
            Password
          </label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
            maxlength="72"
            :aria-invalid="fieldErrors.password ? 'true' : 'false'"
            class="mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500"
          />
          <p v-if="fieldErrors.password" class="mt-1 text-sm text-red-700">
            {{ fieldErrors.password }}
          </p>
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="min-h-12 w-full rounded-xl bg-gold-500 px-4 text-base font-bold text-navy-900 hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>

      <p class="mt-6 text-center">
        <RouterLink
          :to="{ name: 'privacy' }"
          class="inline-flex min-h-11 items-center text-sm font-semibold text-on-navy-soft underline underline-offset-4 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
        >
          Privacy Policy
        </RouterLink>
        <span class="mx-2 text-on-navy-soft" aria-hidden="true">·</span>
        <RouterLink
          :to="{ name: 'delete-account' }"
          class="inline-flex min-h-11 items-center text-sm font-semibold text-on-navy-soft underline underline-offset-4 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
        >
          Delete account
        </RouterLink>
      </p>
    </div>
  </main>
</template>
