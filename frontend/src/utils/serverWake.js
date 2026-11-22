import { ref } from 'vue'

export const serverWaking = ref(false)
let pending = 0
let timer = null

export function trackRequest(promise) {
  pending++
  if (!timer) timer = setTimeout(() => { if (pending > 0) serverWaking.value = true }, 4000)
  const done = () => {
    pending--
    if (pending <= 0) {
      pending = 0
      clearTimeout(timer)
      timer = null
      serverWaking.value = false
    }
  }
  promise.then(done, done)
  return promise
}
