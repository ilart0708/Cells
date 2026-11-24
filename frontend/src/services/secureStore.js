import { isNative } from '../platform'

// The refresh token is a long-lived credential, so on the phone it goes into the Android Keystore
// (via @aparajita/capacitor-secure-storage), never into localStorage. The website never calls these.
// If a plugin update changes its method names, this is the only file to adjust.
//
// IMPORTANT: a Capacitor plugin is a special proxy object. Never return it from an async function or put it in a
// Promise: JavaScript then asks it for ".then", the plugin answers "not implemented", and the call hangs forever.
// Always import it inside the function that uses it and call its methods right there.

const TIMEOUT_MS = 4000

/** Never let a storage problem hold the app up: give up after a few seconds and carry on. */
function withTimeout(promise) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error('Secure storage timed out.')), TIMEOUT_MS)),
  ])
}

export async function secureGet(key) {
  if (!isNative) return null
  try {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    return (await withTimeout(SecureStorage.get(key))) ?? null
  } catch (error) {
    console.warn('Could not read secure storage.', error?.message ?? error)
    return null
  }
}

export async function secureSet(key, value) {
  if (!isNative) return
  try {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    await withTimeout(SecureStorage.set(key, value))
  } catch (error) {
    console.warn('Could not save to secure storage.', error?.message ?? error)
  }
}

export async function secureRemove(key) {
  if (!isNative) return
  try {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    await withTimeout(SecureStorage.remove(key))
  } catch {
    // nothing to remove, or storage unavailable: either way the key is gone for our purposes
  }
}
