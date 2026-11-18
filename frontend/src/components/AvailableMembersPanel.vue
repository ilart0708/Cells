<script setup>
import { reactive, ref } from 'vue'
import { ApiError } from '../api/http'
import { addMember, listAvailableMembers } from '../api/roomMembers'
import { usePagedList } from '../composables/usePagedList'
import { btnOutline } from './buttonClasses'
import ListState from './ListState.vue'
import PaginationBar from './PaginationBar.vue'
import TextField from './TextField.vue'

const props = defineProps({ roomId: { type: Number, required: true } })
const emit = defineEmits(['added'])

const list = reactive(usePagedList((params) => listAvailableMembers(props.roomId, params), { size: 10 }))

const addingId = ref(null)
const message = ref('')
const error = ref('')

async function add(person) {
  if (addingId.value !== null) return
  addingId.value = person.id
  message.value = ''
  error.value = ''
  try {
    const added = await addMember(props.roomId, person.id)
    message.value = `${added.fullName} was added as No. ${added.participationNumber}.`
    emit('added')
    await list.reload()
  } catch (e) {
    error.value = e?.message || 'Something went wrong. Please try again.'
    // Someone else may have added or removed this person in the meantime: refresh the list.
    if (e instanceof ApiError && [404, 409].includes(e.status)) await list.reload()
  } finally {
    addingId.value = null
  }
}
</script>

<template>
  <div class="space-y-4">
    <p class="text-sm text-muted">
      Members of your church who are not in this room yet. New people join at the end of the order.
    </p>

    <TextField v-model="list.query" label="Search church members" type="search" />

    <p v-if="message" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ message }}
    </p>
    <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
      {{ error }}
    </p>

    <div class="overflow-hidden rounded-xl border border-line">
      <ListState
        :loading="list.loading"
        :error="list.error"
        :count="list.items.length"
        :empty-message="
          list.query
            ? 'No one matches your search.'
            : 'Everyone from your church is already in this room. To bring in someone new, use “Invite new person”.'
        "
        @retry="list.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="person in list.items" :key="person.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ person.fullName }}</p>
              <p class="truncate text-sm text-muted">{{ person.email }}</p>
            </div>
            <button type="button" :class="btnOutline" :disabled="addingId !== null" @click="add(person)">
              Add<span class="sr-only">{{ ` ${person.fullName}` }}</span>
            </button>
          </li>
        </ul>
      </ListState>
      <PaginationBar
        :page="list.page"
        :total-pages="list.totalPages"
        :total-elements="list.totalElements"
        :loading="list.loading"
        @change="list.goTo"
      />
    </div>
  </div>
</template>
