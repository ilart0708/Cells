<script setup>
import { reactive, ref } from 'vue'
import { listLeaders, setLeaderStatus } from '../../api/leaders'
import { btnOutline } from '../../components/buttonClasses'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import StatusConfirmDialog from '../../components/StatusConfirmDialog.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { useStatusToggle } from '../../composables/useStatusToggle'

const notice = ref('')

const leaders = reactive(usePagedList(listLeaders))
const toggle = reactive(
  useStatusToggle(setLeaderStatus, async (leader, status) => {
    notice.value = `${leader.fullName} is now ${status === 'ACTIVE' ? 'active' : 'disabled'}.`
    await leaders.reload()
  }),
)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <h1 class="text-2xl font-bold text-navy-900">Cell Group Leader Management</h1>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Cell group leaders">
      <div class="border-b border-line p-4">
        <TextField v-model="leaders.query" label="Search leaders" type="search" />
      </div>

      <ListState
        :loading="leaders.loading"
        :error="leaders.error"
        :count="leaders.items.length"
        :empty-message="leaders.query ? 'No leaders match your search.' : 'No leaders yet. Pastors invite their own leaders.'"
        @retry="leaders.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="leader in leaders.items" :key="leader.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ leader.fullName }}</p>
              <p class="truncate text-sm text-muted">{{ leader.email }}</p>
              <p class="truncate text-sm text-muted">{{ leader.churchName }} · Pastor {{ leader.pastorName }}<span v-if="!leader.churchVerified"> (not verified)</span></p>
            </div>
            <StatusPill :status="leader.status" />
            <button type="button" :class="btnOutline" @click="toggle.ask(leader)">
              {{ leader.status === 'ACTIVE' ? 'Disable' : 'Enable' }}<span class="sr-only"> {{ leader.fullName }}</span>
            </button>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="leaders.page"
        :total-pages="leaders.totalPages"
        :total-elements="leaders.totalElements"
        :loading="leaders.loading"
        @change="leaders.goTo"
      />
    </section>

    <StatusConfirmDialog
      v-model:open="toggle.open"
      :name="toggle.target?.fullName ?? ''"
      :next-status="toggle.nextStatus"
      :busy="toggle.busy"
      :error="toggle.error"
      @confirm="toggle.confirm"
    />
  </div>
</template>
