import { request } from './http'

// The app registers its Firebase (FCM) token after login and removes it at logout.
export const registerDevice = (token, platform) =>
  request('/devices', { method: 'POST', body: { token, platform } })

export const unregisterDevice = (token) =>
  request(`/devices/${encodeURIComponent(token)}`, { method: 'DELETE' })
