import { isNative, platformName } from '../platform'
import { registerDevice, unregisterDevice } from '../api/devices'
import { secureGet, secureRemove, secureSet } from './secureStore'

// Phone push for the Android/iOS app. Everything here is a no-op on the website.
// The plugin is imported on demand, so the website's bundle never contains it.
// A Capacitor plugin is a proxy object: never return it from an async function or put it in a Promise
// (JavaScript would ask it for ".then" and the call would hang). Import it where it is used.

// The backend's Firebase sender must use this same id as the channel of every message it sends.
export const NOTIFICATION_CHANNEL_ID = 'cells-default'

const TOKEN_KEY = 'cells_push_token'
const PLATFORM = { android: 'ANDROID', ios: 'IOS' }

let active = false
let listenersReady = false
let currentToken = null
let openHandler = () => {}

/** Called when the person taps a notification; receives an in-app path such as /member/rooms/1/devotion. */
export function setNotificationOpenHandler(handler) {
  openHandler = handler
}

async function ensureListeners(PushNotifications) {
  if (listenersReady) return
  listenersReady = true

  await PushNotifications.addListener('registration', async ({ value }) => {
    currentToken = value
    await secureSet(TOKEN_KEY, value)
    if (!active) return
    try {
      await registerDevice(value, PLATFORM[platformName] ?? 'ANDROID')
    } catch (error) {
      console.warn('Could not register this phone for notifications.', error?.message ?? error)
    }
  })

  await PushNotifications.addListener('registrationError', (error) => {
    console.warn('Push registration failed.', error?.error ?? error)
  })

  await PushNotifications.addListener('pushNotificationActionPerformed', ({ notification }) => {
    const path = notification?.data?.path
    // Only paths inside this app: a notification can never send the person to another site.
    if (typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')) openHandler(path)
  })
}

/** Ask for permission and register this phone. Safe to call more than once. */
export async function enablePush() {
  if (!isNative || import.meta.env.VITE_PUSH_ENABLED !== 'true') return
  active = true
  try {
    const { PushNotifications } = await import('@capacitor/push-notifications')
    await ensureListeners(PushNotifications)

    let permission = await PushNotifications.checkPermissions()
    if (permission.receive === 'prompt' || permission.receive === 'prompt-with-rationale') {
      permission = await PushNotifications.requestPermissions()
    }
    if (permission.receive !== 'granted') return

    if (platformName === 'android') {
      // Android 8+ shows every notification through a channel. Without our own, messages land in a generic
      // low-priority one and often arrive without a sound or banner.
      try {
        await PushNotifications.createChannel({
          id: NOTIFICATION_CHANNEL_ID,
          name: 'CELLS notifications',
          description: 'New devotions, your turn, and notes from your leader',
          importance: 4,
          visibility: 1,
          vibration: true,
        })
      } catch (error) {
        console.warn('Could not create the notification channel.', error?.message ?? error)
      }
    }

    await PushNotifications.register()
  } catch (error) {
    console.warn('Push notifications are not available.', error?.message ?? error)
  }
}

/** Call BEFORE the session ends (it needs the access token): this phone stops receiving this account's pushes. */
export async function disablePush() {
  if (!isNative) return
  active = false
  const token = currentToken ?? (await secureGet(TOKEN_KEY))
  if (token) {
    try {
      await unregisterDevice(token)
    } catch {
      // The server also drops tokens that Firebase reports as gone, and a new login moves the token to its owner.
    }
  }
  currentToken = null
  await secureRemove(TOKEN_KEY)
}
