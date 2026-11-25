import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth'
import * as invitationsApi from '../api/invitations'
import { isNative } from '../platform'
import { secureGet, secureRemove, secureSet } from '../services/secureStore'
import { disablePush, enablePush } from '../services/push'

const REFRESH_KEY = 'cells_refresh_token'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null) // memory only: never written to localStorage
  const user = ref(null)

  const isAuthenticated = computed(() => accessToken.value !== null && user.value !== null)
  const role = computed(() => user.value?.role ?? null)

  let refreshPromise = null
  // App only: the website's refresh token lives in an httpOnly cookie that scripts cannot see.
  let nativeRefreshToken = null
  let pushStarted = false

  async function applySession(data) {
    accessToken.value = data.accessToken
    user.value = data.user
    if (isNative) {
      // Refresh tokens rotate: the new one must be saved before the next refresh can happen.
      if (data.refreshToken) {
        nativeRefreshToken = data.refreshToken
        await secureSet(REFRESH_KEY, data.refreshToken)
      }
      if (!pushStarted) {
        pushStarted = true
        void enablePush() // never blocks the login: the permission prompt can wait
      }
    }
  }

  function clearSession() {
    accessToken.value = null
    user.value = null
  }

  async function login(email, password) {
    await applySession(await authApi.login(email, password))
  }

  /** A leader signing up without an invitation: creates the account and its (unverified) church, and logs in. */
  async function registerLeader(details) {
    await applySession(await authApi.registerLeader(details))
  }

  /** Accepting an invitation creates the account and logs the person in. */
  async function acceptInvitation(token, fullName, password) {
    await applySession(await invitationsApi.acceptInvitation(token, fullName, password))
  }

  // Many requests can hit a 401 at the same moment. They all share ONE refresh call,
  // because a second call with the same cookie would look like token reuse and log the user out.
  function refresh() {
    if (!refreshPromise) {
      refreshPromise = (isNative && !nativeRefreshToken
        ? Promise.reject(new Error('No saved session.'))
        : authApi.refresh(nativeRefreshToken)
      )
        .then(async (data) => {
          await applySession(data)
          return true
        })
        .catch(async (error) => {
          clearSession()
          // Only a real rejection (401) means the saved token is dead. A dropped connection must not log the
          // person out of the app for good.
          if (isNative && error?.status === 401) {
            nativeRefreshToken = null
            await secureRemove(REFRESH_KEY)
          }
          return false
        })
        .finally(() => {
          refreshPromise = null
        })
    }
    return refreshPromise
  }

  /** Called once at startup: the refresh cookie (the app: the saved refresh token) brings the session back. */
  async function restoreSession() {
    if (isNative) nativeRefreshToken = await secureGet(REFRESH_KEY)
    await refresh()
  }

  async function logout() {
    if (isNative) {
      // While the access token still works: stop this phone receiving this account's notifications.
      pushStarted = false
      await disablePush()
    }
    try {
      await authApi.logout(nativeRefreshToken)
    } catch {
      // even if the server is unreachable, the user leaves this browser session
    }
    if (isNative) {
      nativeRefreshToken = null
      await secureRemove(REFRESH_KEY)
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
    registerLeader,
    logout,
    refresh,
    restoreSession,
    clearSession,
  }
})
