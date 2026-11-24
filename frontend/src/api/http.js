import { API_BASE, isNative } from '../platform'

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || 'Request failed.')
    this.status = status
    this.problem = problem
    this.fieldErrors = problem?.errors ?? {}
  }
}

let config = {
  getToken: () => null,
  refresh: async () => false,
  onSessionExpired: () => {},
}

export function configureHttp(next) {
  config = { ...config, ...next }
}

async function parse(response) {
  if (response.status === 204) return null
  const text = await response.text()
  let body
  try {
    body = text ? JSON.parse(text) : null
  } catch {
    body = null
  }
  if (!response.ok) throw new ApiError(response.status, body)
  return body
}

/** One HTTP call, no refresh logic. Used by the auth endpoints themselves. */
export async function rawRequest(path, { method = 'GET', body, token } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  // Tells the backend to return the refresh token in the body (the app cannot use the website's cookie).
  if (isNative) headers['X-Client-Type'] = 'native'

  // The app talks to the backend cross-origin and never sends cookies; the website stays same-origin with its cookie.
  const init = { method, headers, credentials: isNative ? 'omit' : 'same-origin' }
  if (body !== undefined) init.body = JSON.stringify(body)

  let response
  try {
    response = await fetch(`${API_BASE}/api/v1${path}`, init)
  } catch {
    throw new ApiError(0, { detail: 'Cannot reach the server. Please check your connection.' })
  }
  return parse(response)
}

/** Authenticated call: on a 401, refresh once and retry once. */
export async function request(path, options = {}) {
  try {
    return await rawRequest(path, { ...options, token: config.getToken() })
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error

    const refreshed = await config.refresh()
    if (!refreshed) {
      config.onSessionExpired()
      throw error
    }
    return rawRequest(path, { ...options, token: config.getToken() })
  }
}
