import { rawRequest, request } from './http'

export const login = (email, password) =>
  rawRequest('/auth/login', { method: 'POST', body: { email, password } })

export const refresh = () => rawRequest('/auth/refresh', { method: 'POST' })

export const logout = () => rawRequest('/auth/logout', { method: 'POST' })

export const me = () => request('/auth/me')
