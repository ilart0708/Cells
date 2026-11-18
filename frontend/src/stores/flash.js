import { defineStore } from 'pinia'
import { ref } from 'vue'

/** A one-time message shown by the next page ("Devotion created."), then forgotten. */
export const useFlashStore = defineStore('flash', () => {
  const message = ref('')

  function set(text) {
    message.value = text
  }

  function take() {
    const text = message.value
    message.value = ''
    return text
  }

  return { message, set, take }
})
