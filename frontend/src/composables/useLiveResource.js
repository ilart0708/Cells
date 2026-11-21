import { onMounted, ref } from 'vue'
import { ApiError } from '../api/http'
import { usePolling } from './usePolling'

/**
 * Loads one resource and keeps it current while the tab is visible.
 * A 400 or 404 means "not found" (for example a room archived while the page is open).
 * A failed refresh keeps what is on screen.
 */
export function useLiveResource(fetcher, { intervalMs = 60000 } = {}) {
  const state = ref('loading') // loading | ready | notfound | error
  const data = ref(null)
  const refreshing = ref(false)
  const announcement = ref('') // read out by screen readers only
  let latestRequest = 0

  async function run({ full = false, announce = false, busy = false } = {}) {
    const id = ++latestRequest
    if (full) {
      state.value = 'loading'
      data.value = null
    }
    if (busy) refreshing.value = true
    try {
      const result = await fetcher()
      if (id !== latestRequest) return // a newer request replaced this one
      data.value = result
      state.value = 'ready'
      if (announce) announcement.value = 'Updated.'
    } catch (e) {
      if (id !== latestRequest) return
      if (e instanceof ApiError && [400, 404].includes(e.status)) {
        state.value = 'notfound'
        data.value = null
      } else if (!data.value) {
        state.value = 'error'
      } else if (announce) {
        announcement.value = 'Could not refresh. Please try again.'
      }
    } finally {
      if (id === latestRequest) refreshing.value = false
    }
  }

  usePolling(() => {
    if (state.value === 'ready') run()
  }, intervalMs)

  onMounted(() => run({ full: true }))

  return {
    state,
    data,
    refreshing,
    announcement,
    reload: () => run({ full: true }),
    refresh: () => run({ announce: true, busy: true }),
    reloadQuietly: () => run({ busy: true }),
  }
}
