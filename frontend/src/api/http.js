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
  let body = null
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

  let response
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'same-origin',
    })
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
