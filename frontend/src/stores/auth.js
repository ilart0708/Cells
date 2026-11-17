import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth'
import * as invitationsApi from '../api/invitations'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null) // memory only: never written to localStorage
  const user = ref(null)

  const isAuthenticated = computed(() => accessToken.value !== null && user.value !== null)
  const role = computed(() => user.value?.role ?? null)

  let refreshPromise = null

  function applySession(data) {
    accessToken.value = data.accessToken
    user.value = data.user
  }

  function clearSession() {
    accessToken.value = null
    user.value = null
  }

  async function login(email, password) {
    applySession(await authApi.login(email, password))
  }

  /** Accepting an invitation creates the account and logs the person in. */
  async function acceptInvitation(token, fullName, password) {
    applySession(await invitationsApi.acceptInvitation(token, fullName, password))
  }

  // Many requests can hit a 401 at the same moment. They all share ONE refresh call,
  // because a second call with the same cookie would look like token reuse and log the user out.
  function refresh() {
    if (!refreshPromise) {
      refreshPromise = authApi
        .refresh()
        .then((data) => {
          applySession(data)
          return true
        })
        .catch(() => {
          clearSession()
          return false
        })
        .finally(() => {
          refreshPromise = null
        })
    }
    return refreshPromise
  }

  /** Called once at startup: the refresh cookie (if any) brings the session back. */
  async function restoreSession() {
    await refresh()
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // even if the server is unreachable, the user leaves this browser session
    }
    clearSession()
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    role,
    login,
    acceptInvitation,
    logout,
    refresh,
    restoreSession,
    clearSession,
  }
})
