<script setup>
import { reactive, ref } from 'vue'
import { createChurch, listChurches, renameChurch } from '../../api/churches'
import { ApiError } from '../../api/http'
import BaseDialog from '../../components/BaseDialog.vue'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import ListState from '../../components/ListState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import StatusPill from '../../components/StatusPill.vue'
import TextField from '../../components/TextField.vue'
import { usePagedList } from '../../composables/usePagedList'
import { formatDate } from '../../utils/format'

const churches = reactive(usePagedList(listChurches))
const notice = ref('')

const form = reactive({ open: false, editing: null, name: '', saving: false, error: '', nameError: '' })

function openCreate() {
  Object.assign(form, { open: true, editing: null, name: '', error: '', nameError: '' })
}

function openRename(church) {
  Object.assign(form, { open: true, editing: church, name: church.name, error: '', nameError: '' })
}

async function save() {
  if (form.saving) return
  form.error = ''
  form.nameError = ''

  const name = form.name.trim()
  if (!name) {
    form.nameError = 'Please enter the church name.'
    return
  }

  form.saving = true
  try {
    const renaming = form.editing !== null
    if (renaming) {
      await renameChurch(form.editing.id, name)
    } else {
      await createChurch(name)
    }
    notice.value = renaming ? 'Church renamed.' : 'Church created.'
    form.open = false
    await churches.reload()
  } catch (e) {
    if (e instanceof ApiError) {
      form.nameError = e.fieldErrors.name || (e.status === 409 ? e.message : '')
      if (!form.nameError) form.error = e.message
    } else {
      form.error = 'Something went wrong. Please try again.'
    }
  } finally {
    form.saving = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold text-navy-900">Church Management</h1>
      <button type="button" :class="btnPrimary" @click="openCreate">Add church</button>
    </header>

    <p v-if="notice" role="status" class="rounded-xl border border-gold-100 bg-gold-50 px-4 py-3 text-sm font-semibold text-navy-900">
      {{ notice }}
    </p>

    <section class="overflow-hidden rounded-[18px] border border-line bg-white" aria-label="Churches">
      <div class="border-b border-line p-4">
        <TextField v-model="churches.query" label="Search churches" type="search" />
      </div>

      <ListState
        :loading="churches.loading"
        :error="churches.error"
        :count="churches.items.length"
        :empty-message="churches.query ? 'No churches match your search.' : 'No churches yet. Add your first church to get started.'"
        @retry="churches.reload"
      >
        <ul class="divide-y divide-line">
          <li v-for="church in churches.items" :key="church.id" class="flex flex-wrap items-center gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <p class="truncate font-semibold text-navy-900">{{ church.name }}</p>
              <p class="text-sm text-muted">Added {{ formatDate(church.createdAt) }}</p>
            </div>
            <StatusPill :status="church.status" />
            <button type="button" :class="btnOutline" @click="openRename(church)">
              Rename<span class="sr-only"> {{ church.name }}</span>
            </button>
          </li>
        </ul>
      </ListState>

      <PaginationBar
        :page="churches.page"
        :total-pages="churches.totalPages"
        :total-elements="churches.totalElements"
        :loading="churches.loading"
        @change="churches.goTo"
      />
    </section>

    <BaseDialog v-model:open="form.open" :title="form.editing ? 'Rename church' : 'Add church'">
      <form class="space-y-5" novalidate @submit.prevent="save">
        <p v-if="form.error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
          {{ form.error }}
        </p>
        <TextField v-model="form.name" label="Church name" :error="form.nameError" maxlength="150" required />
        <div class="flex justify-end gap-3">
          <button type="button" :class="btnOutline" @click="form.open = false">Cancel</button>
          <button type="submit" :class="btnPrimary" :disabled="form.saving">
            {{ form.saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </form>
    </BaseDialog>
  </div>
</template>
