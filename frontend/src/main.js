import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { configureHttp } from './api/http'
import { setNotificationOpenHandler } from './services/push'

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
  await auth.restoreSession()

  app.use(router)
  app.mount('#app')
}

bootstrap()
