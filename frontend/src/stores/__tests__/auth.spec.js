import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from '../../api/auth'
import { useAuthStore } from '../auth'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
  refresh: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

const session = {
  accessToken: 'token-123',
  user: { id: 1, email: 'admin@test.com', fullName: 'Admin', role: 'ADMIN' },
}

// In-memory replacement, so the test does not depend on the runtime's own localStorage.
function createFakeStorage() {
  const data = new Map()
  return {
    getItem: vi.fn((key) => data.get(key) ?? null),
    setItem: vi.fn((key, value) => data.set(key, String(value))),
    removeItem: vi.fn((key) => data.delete(key)),
    clear: vi.fn(() => data.clear()),
  }
}

describe('auth store', () => {
  let auth
  let fakeLocalStorage
  let fakeSessionStorage

  beforeEach(() => {
    vi.resetAllMocks()
    fakeLocalStorage = createFakeStorage()
    fakeSessionStorage = createFakeStorage()
    vi.stubGlobal('localStorage', fakeLocalStorage)
    vi.stubGlobal('sessionStorage', fakeSessionStorage)
    setActivePinia(createPinia())
    auth = useAuthStore()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts logged out', () => {
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.role).toBeNull()
  })

  it('stores the session after login, without writing to browser storage', async () => {
    authApi.login.mockResolvedValue(session)

    await auth.login('admin@test.com', 'pw')

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.role).toBe('ADMIN')
    expect(fakeLocalStorage.setItem).not.toHaveBeenCalled()
    expect(fakeSessionStorage.setItem).not.toHaveBeenCalled()
  })

  it('does not log in when the server rejects the credentials', async () => {
    authApi.login.mockRejectedValue(new Error('Invalid email or password.'))

  await expect(auth.login('a@b.com', 'bad')).rejects.toThrow('Invalid email or password.')

    expect(auth.isAuthenticated).toBe(false)
  })

  it('shares ONE refresh call between simultaneous requests', async () => {
    let resolveRefresh
    authApi.refresh.mockImplementation(
      () => new Promise((resolve) => (resolveRefresh = resolve)),
    )

    const calls = [auth.refresh(), auth.refresh(), auth.refresh()]
    resolveRefresh(session)

    expect(await Promise.all(calls)).toEqual([true, true, true])
    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(auth.isAuthenticated).toBe(true)
  })

  it('allows a new refresh after the previous one finished', async () => {
    authApi.refresh.mockResolvedValue(session)

    await auth.refresh()
    await auth.refresh()

    expect(authApi.refresh).toHaveBeenCalledTimes(2)
  })

  it('clears the session and reports false when the refresh fails', async () => {
    authApi.login.mockResolvedValue(session)
    await auth.login('admin@test.com', 'pw')
    authApi.refresh.mockRejectedValue(new Error('401'))

    expect(await auth.refresh()).toBe(false)
    expect(auth.isAuthenticated).toBe(false)
  })

  it('restores a session from the refresh cookie at startup', async () => {
    authApi.refresh.mockResolvedValue(session)

    await auth.restoreSession()

    expect(auth.isAuthenticated).toBe(true)
  })

  it('logs out locally even when the server cannot be reached', async () => {
    authApi.login.mockResolvedValue(session)
    await auth.login('admin@test.com', 'pw')
    authApi.logout.mockRejectedValue(new Error('network down'))

    await auth.logout()

    expect(auth.isAuthenticated).toBe(false)
  })
})
