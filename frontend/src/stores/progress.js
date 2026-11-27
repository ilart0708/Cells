import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getExperience } from '../api/experience'
import { getHistorySummary } from '../api/history'

// A member's level and current streak, for the header. Cheap: two small requests, quiet when they fail.
export const useProgressStore = defineStore('progress', () => {
  const level = ref(null) // { code, name, threshold }
  const streak = ref(0) // devotions in a row: the longest current streak across the rooms I am in
  const forUserId = ref(null)
  let latest = 0

  const ready = computed(() => level.value !== null)

  function reset() {
    level.value = null
    streak.value = 0
    forUserId.value = null
    latest++
  }

  async function load(userId) {
    if (forUserId.value !== userId) reset() // never show the previous person's numbers
    forUserId.value = userId
    const id = ++latest
    try {
      const [experience, history] = await Promise.all([getExperience(), getHistorySummary()])
      if (id !== latest) return
      level.value = experience.level
      streak.value = Math.max(
        0,
        ...history.rooms.filter((room) => room.current && room.roomStatus === 'ACTIVE').map((room) => room.currentStreak),
      )
    } catch {
      // The header just stays without the badge; the pages show their own errors.
    }
  }

  return { level, streak, ready, load, reset }
})
