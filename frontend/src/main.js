import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { configureHttp } from './api/http'
import { setNotificationOpenHandler } from './services/push'
import { isNative } from './platform'
import { ROLE_HOME } from './constants/roles'

// App only: how long to wait for the saved login to be checked before showing the first screen.
// The server can be asleep (free hosting) and a blank screen for a minute looks like a broken app.
const NATIVE_RESTORE_WAIT_MS = 6000

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)

  const auth = useAuthStore(pinia)
  configureHttp({
    getToken: () => auth.accessToken,
    refresh: auth.refresh,
    onSessionExpired: () => {
      auth.clearSession()
      router.push({ name: 'login' })
    },
  })

  // Tapping a push notification opens the page it is about (the router sends logged-out people to login first).
  setNotificationOpenHandler((path) => router.push(path))

  // Restore the session BEFORE the router starts, so the first navigation already knows
  // whether the user is logged in.
  const restoring = auth.restoreSession()
  let restored = true
  if (isNative) {
    // On the phone, do not hold the screen for a slow server: continue after a few seconds and let the
    // login finish in the background.
    restored = await Promise.race([
      restoring.then(() => true),
      new Promise((resolve) => setTimeout(() => resolve(false), NATIVE_RESTORE_WAIT_MS)),
    ])
  } else {
    await restoring
  }

  app.use(router)
  app.mount('#app')
  window.__cellsMounted = true

  if (!restored) {
    // The saved login was still being checked when the first screen appeared. If it succeeds and the person is
    // sitting on the login page, take them home.
    restoring.then(() => {
      if (auth.isAuthenticated && router.currentRoute.value.name === 'login') {
        router.replace({ name: ROLE_HOME[auth.role] })
      }
    })
  }
}

bootstrap().catch((error) => {
  // Without this, a failure before mount leaves a blank white page.
  console.error('CELLS failed to start.', error)
  window.__cellsBootError?.(error?.message ?? error)
})
