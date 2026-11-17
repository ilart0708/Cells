import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { configureHttp, request } from '../http'

const json = (status, body) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })

describe('request', () => {
  let token
  let fetchMock
  let refresh
  let onSessionExpired

  beforeEach(() => {
    token = 'old-token'
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    refresh = vi.fn(async () => {
      token = 'new-token'
      return true
    })
    onSessionExpired = vi.fn()
    configureHttp({ getToken: () => token, refresh, onSessionExpired })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends the bearer token', async () => {
    fetchMock.mockResolvedValueOnce(json(200, { ok: true }))

    const data = await request('/anything')

    expect(data).toEqual({ ok: true })
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer old-token')
  })

  it('refreshes once on a 401 and retries with the new token', async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { detail: 'expired' }))
      .mockResolvedValueOnce(json(200, { ok: true }))

    const data = await request('/anything')

    expect(data).toEqual({ ok: true })
    expect(refresh).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer new-token')
  })

  it('ends the session when the refresh fails', async () => {
    refresh.mockResolvedValueOnce(false)
    fetchMock.mockResolvedValueOnce(json(401, { detail: 'expired' }))

    await expect(request('/anything')).rejects.toMatchObject({ status: 401 })

    expect(onSessionExpired).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('retries only once, so it can never loop', async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { detail: 'expired' }))
      .mockResolvedValueOnce(json(401, { detail: 'still no' }))

    await expect(request('/anything')).rejects.toMatchObject({ status: 401 })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('does not refresh on a 403', async () => {
    fetchMock.mockResolvedValueOnce(json(403, { detail: 'forbidden' }))

    await expect(request('/anything')).rejects.toMatchObject({ status: 403 })

    expect(refresh).not.toHaveBeenCalled()
  })

  it('turns a network failure into a friendly error', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(request('/anything')).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining('Cannot reach the server'),
    })
  })
})
