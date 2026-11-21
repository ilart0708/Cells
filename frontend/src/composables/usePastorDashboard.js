import { ref } from 'vue'
import { getPastorDashboard } from '../api/pastorStats'
import { useLiveResource } from './useLiveResource'

/** The pastor's dashboard, paged by leader. The dashboard and the reports page both use it. */
export function usePastorDashboard(size = 20) {
  const page = ref(0)
  const live = useLiveResource(() => getPastorDashboard({ page: page.value, size }))

  function goTo(next) {
    page.value = next
    return live.reloadQuietly()
  }

  return { ...live, goTo }
}
