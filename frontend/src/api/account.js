import { request } from './http'

// Deletes the signed-in person's own account. The password is asked again as a safety check.
export const deleteAccount = (password) =>
  request('/account/delete', { method: 'POST', body: { password } })
