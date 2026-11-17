import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

/**
 * Loads a paginated list with search. `fetchPage` receives { q, page, size }
 * and must return the backend's page shape.
 */
export function usePagedList(fetchPage, { size = 20, debounceMs = 300 } = {}) {
  const items = ref([])
  const page = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const query = ref('')
  const loading = ref(false)
  const error = ref('')

  let latestRequest = 0
  let timer = null

  async function load() {
    const id = ++latestRequest
    loading.value = true
    error.value = ''
    try {
      const data = await fetchPage({ q: query.value, page: page.value, size })
      if (id !== latestRequest) return // a newer request replaced this one
      items.value = data.content
      totalPages.value = data.totalPages
      totalElements.value = data.totalElements
    } catch (e) {
      if (id !== latestRequest) return
      error.value = e?.message || 'Could not load the list.'
    } finally {
      if (id === latestRequest) loading.value = false
    }
  }

  function goTo(next) {
    page.value = next
    return load()
  }

  watch(query, () => {
    clearTimeout(timer)
    timer = setTimeout(() => {
      page.value = 0
      load()
    }, debounceMs)
  })

  onMounted(load)
  onBeforeUnmount(() => clearTimeout(timer))

  return { items, page, totalPages, totalElements, query, loading, error, reload: load, goTo }
}
