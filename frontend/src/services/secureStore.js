import { isNative } from '../platform'

// The refresh token is a long-lived credential, so on the phone it goes into the Android Keystore
// (via @aparajita/capacitor-secure-storage), never into localStorage. The website never calls these.
// If a plugin update changes its method names, this is the only file to adjust.

async function plugin() {
  const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
  return SecureStorage
}

export async function secureGet(key) {
  if (!isNative) return null
  try {
    return (await (await plugin()).get(key)) ?? null
  } catch {
    return null
  }
}

export async function secureSet(key, value) {
  if (!isNative) return
  try {
    await (await plugin()).set(key, value)
  } catch (error) {
    console.warn('Could not save to secure storage.', error?.message ?? error)
  }
}

export async function secureRemove(key) {
  if (!isNative) return
  try {
    await (await plugin()).remove(key)
  } catch {
    // nothing to remove, or storage unavailable: either way the key is gone for our purposes
  }
}
