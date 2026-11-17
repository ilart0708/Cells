<script setup>
import { reactive, ref } from 'vue'
import { listLeaders, setLeaderStatus } from '../../api/leaders'
import { btnOutline } from '../../components/buttonClasses'
import ConfirmDialog from '../../components/ConfirmDialog.vue'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
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
              <p class="truncate text-sm text-muted">{{ leader.churchName }} · Pastor {{ leader.pastorName }}</p>
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

    <ConfirmDialog
      v-model:open="toggle.open"
      :title="`${toggle.nextStatus === 'DISABLED' ? 'Disable' : 'Enable'} ${toggle.target?.fullName ?? ''}?`"
      :message="
        toggle.nextStatus === 'DISABLED'
          ? 'They will no longer be able to sign in, and anyone already signed in is signed out within 15 minutes. Their data is kept and you can enable them again at any time.'
          : 'They will be able to sign in again.'
      "
      :confirm-label="toggle.nextStatus === 'DISABLED' ? 'Disable' : 'Enable'"
      :busy="toggle.busy"
      :error="toggle.error"
      @confirm="toggle.confirm"
    />
  </div>
</template>
