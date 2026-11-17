import { computed, ref } from 'vue'

/**
 * Enable/disable flow with a confirmation step.
 * apply(id, status) calls the API; onDone(item, status) runs after it succeeds.
 */
export function useStatusToggle(apply, onDone) {
  const target = ref(null)
  const open = ref(false)
  const busy = ref(false)
  const error = ref('')

  const nextStatus = computed(() => (target.value?.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'))

  function ask(item) {
    target.value = item
    error.value = ''
    open.value = true
  }

  async function confirm() {
    if (busy.value || !target.value) return
    const item = target.value
    const status = nextStatus.value
    busy.value = true
    error.value = ''
    try {
      await apply(item.id, status)
      open.value = false
      await onDone(item, status)
    } catch (e) {
      error.value = e?.message || 'Something went wrong. Please try again.'
    } finally {
      busy.value = false
    }
  }

  return { target, open, busy, error, nextStatus, ask, confirm }
}
