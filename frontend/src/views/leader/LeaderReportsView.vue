<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../api/http'
import { dismissReport, listReports, removeReportedAnswer } from '../../api/reports'
import { btnOutline, btnSecondary } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import PageState from '../../components/PageState.vue'
import { reasonLabel } from '../../constants/reportReasons'
import { formatDateTime } from '../../utils/format'

const props = defineProps({ roomId: { type: Number, required: true } })

const state = ref('loading') // loading | ready | notfound | error
const reports = ref([])
const notice = ref('')
const busyId = ref(null)
const actionError = ref('')
const confirmOpen = ref(false)
const confirmTarget = ref(null)

async function load() {
  state.value = 'loading'
  try {
    reports.value = await listReports(props.roomId)
    state.value = 'ready'
  } catch (e) {
    state.value = e instanceof ApiError && e.status === 404 ? 'notfound' : 'error'
  }
}

onMounted(load)

async function run(report, action, message) {
  busyId.value = report.reportId
  actionError.value = ''
  try {
    await action(props.roomId, report.reportId)
    notice.value = message
    await load()
  } catch (e) {
    actionError.value = e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
    confirmOpen.value = false
  } finally {
    busyId.value = null
  }
}

const keep = (report) => run(report, dismissReport, 'The answer was kept and the report closed.')

function askRemove(report) {
  confirmTarget.value = report
  actionError.value = ''
  confirmOpen.value = true
}

async function remove() {
  await run(confirmTarget.value, removeReportedAnswer, 'The answer was removed.')
  confirmOpen.value = false
}
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <header>
      <RouterLink
        :to="{ name: 'leader-room', params: { roomId } }"
        class="inline-flex min-h-11 items-center text-sm font-semibold text-muted underline underline-offset-4"
      >
        ← Back to the room
      </RouterLink>
      <h1 class="text-2xl font-bold text-navy-900">Reported answers</h1>
      <p class="mt-1 text-sm text-muted">
        Members can report an answer. Keep it, or remove its text. A removed answer still counts for participation and
        EXP. The person who reported is never shown.
      </p>
    </header>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>
    <p v-if="actionError" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
      {{ actionError }}
    </p>

    <PageState
      :state="state"
      loading-text="Loading reports…"
      not-found-title="This room could not be found."
      error-text="The reports could not be loaded."
      @retry="load"
    >
      <p v-if="reports.length === 0" class="rounded-[18px] border border-line bg-white p-6 text-center text-sm text-muted">
        No open reports.
      </p>
      <ul v-else class="space-y-3">
        <li v-for="report in reports" :key="report.reportId" class="space-y-2 rounded-[18px] border border-line bg-white p-4">
          <div class="flex flex-wrap items-center gap-3">
            <p class="min-w-0 flex-1 font-semibold text-navy-900">{{ report.authorName }}</p>
            <span class="rounded-full bg-gold-100 px-3 py-1 text-xs font-bold text-navy-900">{{ reasonLabel(report.reason) }}</span>
          </div>
          <p class="text-sm text-muted">
            Written {{ formatDateTime(report.answerSubmittedAt) }} · reported {{ formatDateTime(report.reportedAt) }}
          </p>
          <p class="whitespace-pre-line break-words text-sm text-navy-900">{{ report.content }}</p>
          <p v-if="report.note" class="rounded-xl bg-app-bg px-3 py-2 text-sm text-muted">Reporter’s note: {{ report.note }}</p>
          <div class="flex flex-wrap justify-end gap-2 pt-1">
            <button type="button" :class="btnOutline" :disabled="busyId === report.reportId" @click="keep(report)">
              Keep answer
            </button>
            <button type="button" :class="btnSecondary" :disabled="busyId === report.reportId" @click="askRemove(report)">
              Remove answer
            </button>
          </div>
        </li>
      </ul>
    </PageState>

    <ConfirmDialog
      v-model:open="confirmOpen"
      title="Remove this answer?"
      message="The text is replaced for everyone. It still counts for participation and EXP. This cannot be undone."
      confirm-label="Remove answer"
      :busy="busyId !== null"
      :error="actionError"
      @confirm="remove"
    />
  </div>
</template>
