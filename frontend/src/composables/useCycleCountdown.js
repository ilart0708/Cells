import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getToday } from '../api/clock'
import { usePolling } from './usePolling'

/**
 * Time until the platform's next daily reset (12:00 AM in the platform's zone, from the server, never the
 * phone's own clock). When the countdown reaches zero it asks the server again, and it also re-checks every
 * few minutes so a phone that slept through midnight corrects itself.
 */
export function useCycleCountdown() {
  const zone = ref('')
  const nextResetAt = ref(null)
  const now = ref(Date.now())
  const failed = ref(false)
  let recheck = null

  async function load() {
    try {
      const clock = await getToday()
      zone.value = clock.zone
      nextResetAt.value = new Date(clock.nextResetAt).getTime()
      failed.value = false
    } catch {
      // A missing indicator is never an error worth showing.
      failed.value = true
    }
  }

  function tick() {
    now.value = Date.now()
    if (nextResetAt.value !== null && now.value >= nextResetAt.value) load()
  }

  usePolling(tick, 30000)

  onMounted(() => {
    load()
    recheck = setInterval(load, 5 * 60 * 1000)
  })
  onBeforeUnmount(() => clearInterval(recheck))

  const remainingMs = computed(() => (nextResetAt.value === null ? null : Math.max(0, nextResetAt.value - now.value)))

  const label = computed(() => {
    if (remainingMs.value === null) return ''
    const totalMinutes = Math.floor(remainingMs.value / 60000)
    if (totalMinutes < 1) return 'less than a minute'
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
  })

  return { zone, label, visible: computed(() => remainingMs.value !== null && !failed.value) }
}
