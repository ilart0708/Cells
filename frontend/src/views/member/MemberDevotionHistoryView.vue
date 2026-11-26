<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getToday } from '../../api/clock'
import { ApiError } from '../../api/http'
import { listMemberDevotions } from '../../api/memberDevotions'
import { getMyRoom } from '../../api/memberRooms'
import { btnOutline } from '../../components/buttonClasses'
import LevelPill from '../../components/LevelPill.vue'
import ListState from '../../components/ListState.vue'
import PageState from '../../components/PageState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDay } from '../../utils/dates'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const room = ref(null)
const today = ref('')

async function loadContext() {
  state.value = 'loading'
  try {
    const [roomData, clock] = await Promise.all([getMyRoom(props.roomId), getToday()])
    room.value = roomData.room
    today.value = clock.today
    state.value = 'ready'
  } catch (e) {
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

const devotions = reactive(usePagedList((params) => listMemberDevotions(props.roomId, params)))

onMounted(loadContext)

watch(
  () => props.roomId,
  () => {
    loadContext()
    devotions.goTo(0)
  },
)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <RouterLink
      :to="{ name: 'member-room', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Your room
    </RouterLink>

    <PageState
      :state="state"
      not-found-title="This room could not be found."
      not-found-hint="You may no longer be in this room, or it may have been archived. Your dashboard lists the rooms you belong to."
      error-text="The devotions could not be loaded."
      @retry="loadContext"
    >
      <header class="space-y-2">
        <h1 class="text-2xl font-bold text-navy-900">Devotions</h1>
        <RoomTitle
          :church-name="room.churchName"
          :room-name="room.roomName"
          :leader-name="room.leaderName" :pastor-name="room.pastorName" :church-verified="room.churchVerified"
          :level="2"
        />
      </header>

      <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Devotions">
        <ListState
          :loading="devotions.loading"
          :error="devotions.error"
          :count="devotions.items.length"
          empty-message="No devotions yet. The passages your leader posts will appear here."
          @retry="devotions.reload"
        >
          <ul class="divide-y divide-line">
            <li v-for="devotion in devotions.items" :key="devotion.id" class="flex flex-wrap items-center gap-3 px-4 py-4">
              <div class="min-w-0 flex-1 space-y-1">
                <p class="flex flex-wrap items-center gap-2 text-sm text-muted">
                  <span>{{ formatDay(devotion.date) }}</span>
                  <span v-if="devotion.date === today" class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">
                    Today
                  </span>
                </p>
                <p class="break-words font-semibold text-navy-900">
                  {{ devotion.reference }}<template v-if="devotion.title"> · {{ devotion.title }}</template>
                </p>
                <LevelPill :level="devotion.learningLevel" />
              </div>
              <RouterLink
                :to="{ name: 'member-devotion', params: { roomId, devotionId: devotion.id } }"
                :class="btnOutline"
              >
                Open<span class="sr-only"> the devotion for {{ formatDay(devotion.date) }}</span>
              </RouterLink>
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
    </PageState>
  </div>
</template>
