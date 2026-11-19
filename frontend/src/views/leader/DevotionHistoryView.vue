<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getToday } from '../../api/clock'
import { listDevotions, setDevotionStatus } from '../../api/devotions'
import { ApiError } from '../../api/http'
import { getRoom } from '../../api/rooms'
import { btnOutline, btnPrimary, btnSecondary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import LevelPill from '../../components/LevelPill.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import StatusPill from '../../components/StatusPill.vue'
import { usePagedList } from '../../composables/usePagedList'
import { useFlashStore } from '../../stores/flash'
import { formatDay } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const FILTERS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'ALL', label: 'All' },
]

const flash = useFlashStore()
const notice = ref(flash.take())

// ---------- the room and the platform's date ----------
const room = ref(null)
const today = ref('')
const roomState = ref('loading') // loading | ready | notfound | error
const canChange = computed(() => room.value?.status === 'ACTIVE')

async function loadContext() {
  roomState.value = 'loading'
  try {
    const [roomData, clock] = await Promise.all([getRoom(props.roomId), getToday()])
    room.value = roomData
    today.value = clock.today
    roomState.value = 'ready'
  } catch (e) {
    roomState.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// ---------- the list ----------
const filter = ref('ACTIVE')
const devotions = reactive(
  usePagedList((params) => listDevotions(props.roomId, { ...params, status: filter.value })),
)

watch(filter, () => devotions.goTo(0))

const emptyMessage = computed(() => {
  if (filter.value === 'CANCELLED') return 'No cancelled devotions.'
  return 'No devotions yet. Create your first devotion for this room.'
})

function relation(date) {
  if (!today.value) return ''
  if (date === today.value) return 'today'
  return date > today.value ? 'upcoming' : 'past'
}

// ---------- cancel / restore ----------
const change = reactive({ open: false, target: null, busy: false, error: '' })
const cancelling = computed(() => change.target?.status === 'ACTIVE')

function askChange(devotion) {
  Object.assign(change, { open: true, target: devotion, busy: false, error: '' })
}

async function confirmChange() {
  if (change.busy || !change.target) return
  const devotion = change.target
  const next = devotion.status === 'ACTIVE' ? 'CANCELLED' : 'ACTIVE'

  change.busy = true
  change.error = ''
  try {
    await setDevotionStatus(props.roomId, devotion.id, next)
    change.open = false
    notice.value = `The devotion for ${formatDay(devotion.date)} was ${next === 'CANCELLED' ? 'cancelled' : 'restored'}.`
    await devotions.reload()
  } catch (e) {
    change.error = e?.message || 'Something went wrong. Please try again.'
  } finally {
    change.busy = false
  }
}

onMounted(loadContext)

watch(
  () => props.roomId,
  () => {
    notice.value = ''
    loadContext()
    devotions.goTo(0)
  },
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <RouterLink
      :to="{ name: 'leader-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Room
    </RouterLink>

    <p v-if="roomState === 'loading'" role="status" class="p-6 text-center text-sm text-muted">Loading…</p>

    <div v-else-if="roomState === 'notfound'" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room could not be found.</p>
      <p class="mt-1 text-sm text-muted">Go back to My Rooms and choose one of your rooms.</p>
    </div>

    <div v-else-if="roomState === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">The room could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="loadContext">Try again</button>
    </div>

    <template v-else>
      <header class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0 space-y-2">
          <h1 class="text-2xl font-bold text-navy-900">Devotions</h1>
          <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" :level="2" />
        </div>
        <RouterLink v-if="canChange" :to="{ name: 'leader-devotion-new', params: { roomId } }" :class="btnPrimary">
          Create devotion
        </RouterLink>
      </header>

      <p v-if="!canChange" role="status" class="rounded-xl border border-line bg-white px-4 py-3 text-sm text-muted">
        This room is archived, so devotions can’t be added or changed. Restore the room from My Rooms to continue.
      </p>

      <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
        {{ notice }}
      </p>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Devotions">
        <div class="border-b border-line p-4">
          <div role="group" aria-label="Show devotions" class="flex flex-wrap gap-2">
            <button
              v-for="option in FILTERS"
              :key="option.value"
              type="button"
              :aria-pressed="filter === option.value"
              :class="filter === option.value ? btnSecondary : btnOutline"
              @click="filter = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <ListState
          :loading="devotions.loading"
          :error="devotions.error"
          :count="devotions.items.length"
          :empty-message="emptyMessage"
          @retry="devotions.reload"
        >
          <ul class="divide-y divide-line">
            <li v-for="devotion in devotions.items" :key="devotion.id" class="flex flex-wrap items-center gap-3 px-4 py-4">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="flex flex-wrap items-center gap-2 text-sm text-muted">
                  <span>{{ formatDay(devotion.date) }}</span>
                  <span v-if="relation(devotion.date) === 'today'" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">Today</span>
                  <span v-else-if="relation(devotion.date) === 'upcoming'" class="rounded-full bg-track px-3 py-1 text-xs font-bold text-navy-900">Upcoming</span>
                </p>
                <p class="break-words font-semibold text-navy-900">
                  {{ devotion.reference }}<template v-if="devotion.title"> · {{ devotion.title }}</template>
                </p>
                <LevelPill :level="devotion.learningLevel" />
                <p v-if="devotion.submissionCount > 0" class="text-sm text-muted">
                  {{ devotion.submissionCount }} {{ devotion.submissionCount === 1 ? 'answer' : 'answers' }}<template
                    v-if="devotion.status === 'ACTIVE' && !devotion.editable && relation(devotion.date) !== 'past'"
                  > · locked, because members have already shared</template>
                </p>
              </div>

              <StatusPill v-if="devotion.status === 'CANCELLED'" status="CANCELLED" />

              <RouterLink
                v-if="devotion.submissionCount > 0"
                :to="{ name: 'leader-answers', params: { roomId, devotionId: devotion.id } }"
                :class="btnOutline"
              >
                Answers<span class="sr-only"> for {{ formatDay(devotion.date) }}</span>
              </RouterLink>
              <template v-if="canChange">
                <RouterLink
                  v-if="devotion.editable"
                  :to="{ name: 'leader-devotion-edit', params: { roomId, devotionId: devotion.id } }"
                  :class="btnOutline"
                >
                  Edit<span class="sr-only"> devotion for {{ formatDay(devotion.date) }}</span>
                </RouterLink>
                <button v-if="devotion.editable" type="button" :class="btnOutline" @click="askChange(devotion)">
                  Cancel devotion<span class="sr-only"> for {{ formatDay(devotion.date) }}</span>
                </button>
                <button v-if="devotion.restorable" type="button" :class="btnOutline" @click="askChange(devotion)">
                  Restore<span class="sr-only"> devotion for {{ formatDay(devotion.date) }}</span>
                </button>
              </template>
            </li>
          </ul>
        </ListState>

        <PaginationBar
          :page="devotions.page"
          :total-pages="devotions.totalPages"
          :total-elements="devotions.totalElements"
          :loading="devotions.loading"
          @change="devotions.goTo"
        />
      </section>
    </template>

    <ConfirmDialog
      v-model:open="change.open"
      :title="`${cancelling ? 'Cancel' : 'Restore'} the devotion for ${formatDay(change.target?.date)}?`"
      :message="
        cancelling
          ? 'Members will no longer see it. Nothing is deleted, and you can restore it later as long as its day has not passed and no other devotion uses that day.'
          : 'Members will see it again on its day.'
      "
      :confirm-label="cancelling ? 'Cancel devotion' : 'Restore'"
      :busy="change.busy"
      :error="change.error"
      @confirm="confirmChange"
    />
  </div>
</template>
