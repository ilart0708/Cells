import { rawRequest } from './http'

// Public endpoints: the person has no session yet, so no refresh logic (rawRequest).
export const previewInvitation = (token) =>
  rawRequest('/invitations/preview', { method: 'POST', body: { token } })

export const acceptInvitation = (token, fullName, password, acceptRules) =>
  rawRequest('/invitations/accept', { method: 'POST', body: { token, fullName, password, acceptRules } })
