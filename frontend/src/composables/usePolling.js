import { onBeforeUnmount, onMounted } from 'vue'

/**
 * Calls `callback` every `intervalMs` while the tab is visible, and right away when the tab becomes
 * visible again. A background tab stays quiet. Everything is cleaned up when the page unmounts.
 */
export function usePolling(callback, intervalMs = 30000) {
  let timer = null

  function whenVisible() {
    if (document.visibilityState === 'visible') callback()
  }

  onMounted(() => {
    timer = setInterval(whenVisible, intervalMs)
    document.addEventListener('visibilitychange', whenVisible)
  })

  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', whenVisible)
  })
}
