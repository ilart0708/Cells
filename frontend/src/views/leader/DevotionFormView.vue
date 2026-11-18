<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, onBeforeRouteLeave, useRouter } from 'vue-router'
import { listBooks } from '../../api/bible'
import { getToday } from '../../api/clock'
import { createDevotion, getDevotion, listDevotions, updateDevotion } from '../../api/devotions'
import { ApiError } from '../../api/http'
import { getRoom } from '../../api/rooms'
import { btnOutline, btnPrimary } from '../../components/buttonClasses'
import DevotionCard from '../../components/DevotionCard.vue'
import RoomTitle from '../../components/RoomTitle.vue'
import TextField from '../../components/TextField.vue'
import { LEARNING_LEVELS } from '../../constants/devotion'
import { useFlashStore } from '../../stores/flash'
import { buildReference } from '../../utils/bible'
import { addDays, formatDay } from '../../utils/dates'

const props = defineProps({
  roomId: { type: Number, required: true },
  devotionId: { type: Number, default: null }, // null: creating
})

const router = useRouter()
const flash = useFlashStore()

const selectClass =
  'mt-2 block w-full rounded-xl border border-line-input bg-white px-4 py-3.5 text-base text-navy-900 outline-none focus:border-gold-500 focus:ring-1 focus:ring-gold-500'
const labelClass = 'block text-[13px] font-bold uppercase tracking-[1px] text-muted'

// loading | ready | notfound | error | archived | locked
const state = ref('loading')
const room = ref(null)
const devotion = ref(null)
const books = ref([])
const today = ref('')
const latest = ref('')
const zone = ref('')

const editing = computed(() => props.devotionId !== null)

const form = reactive({
  date: '',
  book: '',
  chapter: '',
  verseStart: '',
  verseEnd: '',
  title: '',
  level: '',
  instructions: '',
})
const errors = ref({})
const formError = ref('')
const saving = ref(false)
const initial = ref('')

const oldBooks = computed(() => books.value.filter((book) => book.testament === 'OLD'))
const newBooks = computed(() => books.value.filter((book) => book.testament === 'NEW'))
const selectedBook = computed(() => books.value.find((book) => book.code === form.book))

// Today's devotion keeps its day: the server refuses to move it.
const dateLocked = computed(() => editing.value && devotion.value !== null && devotion.value.date <= today.value)

const dateHint = computed(() =>
  dateLocked.value
    ? 'Today’s devotion stays on today. To use another day, cancel it and create a new one.'
    : `Choose a day from ${formatDay(today.value)} to ${formatDay(latest.value)}. Days follow the platform’s timezone (${zone.value}).`,
)

const chapterHint = computed(() => {
  const book = selectedBook.value
  if (!book) return ''
  return `${book.name} has ${book.chapters} ${book.chapters === 1 ? 'chapter' : 'chapters'}.`
})

// The preview mirrors the saved reference; the history page shows the one the server built.
const previewReference = computed(() => {
  const whole = (text) => (/^\d+$/.test(String(text).trim()) ? Number(String(text).trim()) : null)
  return buildReference(selectedBook.value?.name, whole(form.chapter), whole(form.verseStart), whole(form.verseEnd))
})

const dirty = computed(() => state.value === 'ready' && JSON.stringify(form) !== initial.value)

// ---------- loading ----------
function fillFrom(saved) {
  Object.assign(form, {
    date: saved.date,
    book: saved.book,
    chapter: String(saved.chapter),
    verseStart: saved.verseStart == null ? '' : String(saved.verseStart),
    verseEnd: saved.verseEnd == null ? '' : String(saved.verseEnd),
    title: saved.title ?? '',
    level: saved.learningLevel,
    instructions: saved.instructions ?? '',
  })
}

// The first day from today on that has no devotion yet, so a new devotion does not start with a clash.
function firstFreeDay(takenDays) {
  let day = today.value
  while (day <= latest.value && takenDays.has(day)) {
    day = addDays(day, 1)
  }
  return day <= latest.value ? day : today.value
}

async function load() {
  state.value = 'loading'
  errors.value = {}
  formError.value = ''
  try {
    const [roomData, clock, bookList, extra] = await Promise.all([
      getRoom(props.roomId),
      getToday(),
      listBooks(),
      editing.value
        ? getDevotion(props.roomId, props.devotionId)
        : listDevotions(props.roomId, { status: 'ACTIVE', size: 20 }),
    ])
    room.value = roomData
    books.value = bookList
    today.value = clock.today
    latest.value = clock.latestDevotionDate
    zone.value = clock.zone

    if (roomData.status !== 'ACTIVE') {
      state.value = 'archived'
      return
    }

    if (editing.value) {
      devotion.value = extra
      if (!extra.editable) {
        state.value = 'locked'
        return
      }
      fillFrom(extra)
    } else {
      devotion.value = null
      Object.assign(form, {
        date: firstFreeDay(new Set(extra.content.map((item) => item.date))),
        book: '',
        chapter: '',
        verseStart: '',
        verseEnd: '',
        title: '',
        level: '',
        instructions: '',
      })
    }

    initial.value = JSON.stringify(form)
    state.value = 'ready'
  } catch (e) {
    state.value = e instanceof ApiError && [400, 404].includes(e.status) ? 'notfound' : 'error'
  }
}

// ---------- checking and saving ----------
function parseWhole(text) {
  const trimmed = String(text).trim()
  if (trimmed === '') return { value: null }
  return /^\d+$/.test(trimmed) ? { value: Number(trimmed) } : { error: 'Enter a whole number.' }
}

// The same rules as the server (which stays the real authority), so mistakes show before a round trip.
function check() {
  const found = {}

  if (!dateLocked.value) {
    if (!form.date) found.date = 'Choose a day.'
    else if (form.date < today.value) found.date = 'Choose today or a later day.'
    else if (form.date > latest.value) found.date = `Choose a day on or before ${formatDay(latest.value)}.`
  }

  const book = selectedBook.value
  if (!book) found.book = 'Choose a book.'

  const chapter = parseWhole(form.chapter)
  if (chapter.error) found.chapter = chapter.error
  else if (chapter.value === null) found.chapter = 'Enter the chapter.'
  else if (chapter.value < 1) found.chapter = 'Chapters start at 1.'
  else if (book && chapter.value > book.chapters) found.chapter = chapterHint.value

  const start = parseWhole(form.verseStart)
  const end = parseWhole(form.verseEnd)
  if (start.error) found.verseStart = start.error
  else if (start.value === 0) found.verseStart = 'Verses start at 1.'
  if (end.error) found.verseEnd = end.error
  else if (end.value === 0) found.verseEnd = 'Verses start at 1.'

  if (!found.verseStart && !found.verseEnd) {
    if (start.value === null && end.value !== null) {
      found.verseStart = 'Enter the first verse too, or clear the last verse.'
    } else if (start.value !== null && end.value !== null && end.value < start.value) {
      found.verseEnd = 'The last verse cannot come before the first.'
    }
  }

  if (!form.level) found.learningLevel = 'Choose a learning level.'

  return {
    found,
    body: {
      date: form.date,
      book: form.book,
      chapter: chapter.value,
      verseStart: start.value,
      verseEnd: end.value,
      title: form.title.trim() || null,
      learningLevel: form.level,
      instructions: form.instructions.trim() || null,
    },
  }
}

async function submit() {
  if (saving.value) return
  formError.value = ''

  const { found, body } = check()
  errors.value = found
  if (Object.keys(found).length > 0) return

  saving.value = true
  try {
    if (editing.value) await updateDevotion(props.roomId, props.devotionId, body)
    else await createDevotion(props.roomId, body)

    initial.value = JSON.stringify(form) // saved: nothing left to protect
    flash.set(editing.value ? 'Devotion updated.' : 'Devotion created.')
    await router.push({ name: 'leader-devotions', params: { roomId: props.roomId } })
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.status === 400 && Object.keys(e.fieldErrors).length > 0) {
        errors.value = e.fieldErrors
        formError.value = 'Please check the highlighted fields.'
      } else if (e.status === 409) {
        errors.value = { date: e.message }
      } else {
        formError.value = e.message
      }
    } else {
      formError.value = 'Something went wrong. Please try again.'
    }
  } finally {
    saving.value = false
  }
}

// ---------- do not lose unsaved work by accident ----------
onBeforeRouteLeave(
  () => !dirty.value || window.confirm('You have unsaved changes to this devotion. Leave without saving?'),
)

function warnBeforeUnload(event) {
  if (dirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', warnBeforeUnload)
  load()
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeUnload))

watch(() => [props.roomId, props.devotionId], load)
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-4">
    <RouterLink
      :to="{ name: 'leader-devotions', params: { roomId } }"
      class="inline-flex min-h-11 items-center text-sm font-bold text-navy-900 underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
    >
      ← Devotions
    </RouterLink>

    <p v-if="state === 'loading'" role="status" class="p-6 text-center text-sm text-muted">Loading…</p>

    <div v-else-if="state === 'notfound'" role="alert" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room or devotion could not be found.</p>
      <p class="mt-1 text-sm text-muted">Go back to My Rooms and choose one of your rooms.</p>
    </div>

    <div v-else-if="state === 'error'" role="alert" class="space-y-3 rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="text-sm font-semibold text-red-700">The page could not be loaded.</p>
      <button type="button" :class="btnOutline" @click="load">Try again</button>
    </div>

    <div v-else-if="state === 'archived'" role="status" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This room is archived.</p>
      <p class="mt-1 text-sm text-muted">Devotions can’t be added or changed. Restore the room from My Rooms to continue.</p>
    </div>

    <div v-else-if="state === 'locked'" role="status" class="rounded-[18px] border border-line bg-white p-6 text-center">
      <p class="font-semibold text-navy-900">This devotion can no longer be changed.</p>
      <p class="mt-1 text-sm text-muted">Devotions from earlier days, and cancelled ones, are part of the history.</p>
    </div>

    <template v-else>
      <header class="space-y-2">
        <h1 class="text-2xl font-bold text-navy-900">{{ editing ? 'Edit devotion' : 'Create devotion' }}</h1>
        <RoomTitle :church-name="room.churchName" :room-name="room.name" :leader-name="room.leaderName" :level="2" />
      </header>

      <div class="gap-6 lg:grid lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <form class="space-y-5 rounded-[18px] border border-line bg-white p-5" novalidate @submit.prevent="submit">
          <p v-if="formError" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
            {{ formError }}
          </p>

          <TextField
            v-model="form.date"
            label="Day"
            type="date"
            :min="today"
            :max="latest"
            :readonly="dateLocked"
            :hint="dateHint"
            :error="errors.date"
            required
          />

          <div>
            <label for="devotion-book" :class="labelClass">Book</label>
            <select
              id="devotion-book"
              v-model="form.book"
              required
              :aria-invalid="errors.book ? 'true' : 'false'"
              :aria-describedby="errors.book ? 'devotion-book-error' : undefined"
              :class="selectClass"
            >
              <option value="" disabled>Choose a book</option>
              <optgroup label="Old Testament">
                <option v-for="book in oldBooks" :key="book.code" :value="book.code">{{ book.name }}</option>
              </optgroup>
              <optgroup label="New Testament">
                <option v-for="book in newBooks" :key="book.code" :value="book.code">{{ book.name }}</option>
              </optgroup>
            </select>
            <p v-if="errors.book" id="devotion-book-error" class="mt-1 text-sm text-red-700">{{ errors.book }}</p>
          </div>

          <div class="grid gap-4 sm:grid-cols-3">
            <TextField
              v-model="form.chapter"
              label="Chapter"
              inputmode="numeric"
              maxlength="3"
              :hint="chapterHint"
              :error="errors.chapter"
              required
            />
            <TextField v-model="form.verseStart" label="From verse" inputmode="numeric" maxlength="3" :error="errors.verseStart" />
            <TextField v-model="form.verseEnd" label="To verse" inputmode="numeric" maxlength="3" :error="errors.verseEnd" />
          </div>
          <p class="-mt-2 text-sm text-muted">
            Leave both verses empty for the whole chapter, or fill only “From verse” for a single verse.
          </p>

          <fieldset :aria-describedby="errors.learningLevel ? 'devotion-level-error' : undefined">
            <legend :class="labelClass">Learning level</legend>
            <div class="mt-2 grid gap-2 sm:grid-cols-3">
              <label
                v-for="option in LEARNING_LEVELS"
                :key="option.value"
                :class="[
                  'flex min-h-11 cursor-pointer items-center gap-3 rounded-xl border px-4 py-2 text-sm font-semibold text-navy-900 focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-navy-900',
                  form.level === option.value ? 'border-gold-500 bg-gold-50' : 'border-line-input bg-white',
                ]"
              >
                <input v-model="form.level" type="radio" name="devotion-level" :value="option.value" class="size-5 accent-navy-900" />
                {{ option.label }}
              </label>
            </div>
            <p v-if="errors.learningLevel" id="devotion-level-error" class="mt-1 text-sm text-red-700">
              {{ errors.learningLevel }}
            </p>
          </fieldset>

          <TextField v-model="form.title" label="Title (optional)" maxlength="150" autocomplete="off" :error="errors.title" />

          <div>
            <label for="devotion-instructions" :class="labelClass">Instructions (optional)</label>
            <textarea
              id="devotion-instructions"
              v-model="form.instructions"
              rows="4"
              maxlength="1000"
              :aria-invalid="errors.instructions ? 'true' : 'false'"
              :aria-describedby="errors.instructions ? 'devotion-instructions-error' : undefined"
              :class="selectClass"
            ></textarea>
            <p class="mt-1 text-sm text-muted">{{ form.instructions.length }} / 1000</p>
            <p v-if="errors.instructions" id="devotion-instructions-error" class="text-sm text-red-700">
              {{ errors.instructions }}
            </p>
          </div>

          <div class="flex flex-wrap justify-end gap-3">
            <RouterLink :to="{ name: 'leader-devotions', params: { roomId } }" :class="btnOutline">Cancel</RouterLink>
            <button type="submit" :class="btnPrimary" :disabled="saving">
              {{ saving ? 'Saving…' : editing ? 'Save changes' : 'Create devotion' }}
            </button>
          </div>
        </form>

        <aside aria-labelledby="preview-title" class="mt-6 space-y-3 lg:sticky lg:top-6 lg:mt-0 lg:self-start">
          <h2 id="preview-title" class="text-xs font-bold uppercase tracking-[2px] text-muted">
            Preview: what members will see
          </h2>
          <DevotionCard
            :reference="previewReference"
            :title="form.title.trim()"
            :level="form.level"
            :instructions="form.instructions.trim()"
            :date="form.date"
          />
        </aside>
      </div>
    </template>
  </div>
</template>
