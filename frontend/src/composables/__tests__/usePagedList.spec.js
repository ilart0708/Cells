import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { usePagedList } from '../usePagedList'

const pageOf = (names, totalPages = 1) => ({
  content: names.map((name, id) => ({ id, name })),
  page: 0,
  size: 20,
  totalElements: names.length,
  totalPages,
})

// Mounts a throwaway component so lifecycle hooks (onMounted) run.
function useInComponent(fetchPage) {
  let list
  mount(
    defineComponent({
      setup() {
        list = usePagedList(fetchPage, { debounceMs: 300 })
        return () => h('div')
      },
    }),
  )
  return list
}

const settle = () => vi.advanceTimersByTimeAsync(0)

describe('usePagedList', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads the first page on mount', async () => {
    vi.useFakeTimers()
    const fetchPage = vi.fn().mockResolvedValue(pageOf(['Victory']))

    const list = useInComponent(fetchPage)
    await settle()

    expect(fetchPage).toHaveBeenCalledWith({ q: '', page: 0, size: 20 })
    expect(list.items.value).toHaveLength(1)
    expect(list.loading.value).toBe(false)
  })

  it('debounces searching and goes back to the first page', async () => {
    vi.useFakeTimers()
    const fetchPage = vi.fn().mockResolvedValue(pageOf(['Grace'], 3))
    const list = useInComponent(fetchPage)
    await settle()
    await list.goTo(2)
    fetchPage.mockClear()

    list.query.value = 'gra'
    await vi.advanceTimersByTimeAsync(100)
    expect(fetchPage).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(300)
    expect(fetchPage).toHaveBeenCalledTimes(1)
    expect(fetchPage).toHaveBeenCalledWith({ q: 'gra', page: 0, size: 20 })
  })

  it('ignores a slow response that arrives after a newer one', async () => {
    vi.useFakeTimers()
    const resolvers = []
    const fetchPage = vi.fn(() => new Promise((resolve) => resolvers.push(resolve)))

    const list = useInComponent(fetchPage) // request 1 (pending)
    const second = list.goTo(1) // request 2 (pending)

    resolvers[1](pageOf(['Newer']))
    await second
    resolvers[0](pageOf(['Older']))
    await settle()

    expect(list.items.value[0].name).toBe('Newer')
    expect(list.loading.value).toBe(false)
  })

  it('shows an error and recovers on reload', async () => {
    vi.useFakeTimers()
    const fetchPage = vi
      .fn()
      .mockRejectedValueOnce(new Error('Cannot reach the server.'))
      .mockResolvedValueOnce(pageOf(['Victory']))

    const list = useInComponent(fetchPage)
    await settle()
    expect(list.error.value).toBe('Cannot reach the server.')
    expect(list.items.value).toHaveLength(0)

    await list.reload()

    expect(list.error.value).toBe('')
    expect(list.items.value).toHaveLength(1)
  })
})
