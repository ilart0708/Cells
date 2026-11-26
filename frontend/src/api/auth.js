import { rawRequest, request } from './http'
import { isNative } from '../platform'

export const login = (email, password) =>
  rawRequest('/auth/login', { method: 'POST', body: { email, password } })

// A leader signing up without an invitation. Public, so no refresh logic (rawRequest).
export const registerLeader = (details) =>
  rawRequest('/auth/register-leader', { method: 'POST', body: details })

// The website's refresh token travels in its cookie. The app has no usable cookie, so it sends the token it keeps
// in secure storage in the request body instead.
const refreshBody = (refreshToken) => (isNative ? { refreshToken } : undefined)

export const refresh = (refreshToken) =>
  rawRequest('/auth/refresh', { method: 'POST', body: refreshBody(refreshToken) })

export const logout = (refreshToken) =>
  rawRequest('/auth/logout', { method: 'POST', body: refreshBody(refreshToken) })

export const me = () => request('/auth/me')
