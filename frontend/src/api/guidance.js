import { request } from './http'

// The leader's summary and reflection for one devotion (Option A and Option B of the product document).
// All leader calls are scoped to the logged-in leader by the backend (the id comes from the token).

// { devotionId, answerCount, canWrite, writeBlockedReason, summary, reflection }
export const getLeaderGuidance = (roomId, devotionId) =>
  request(`/leader/rooms/${roomId}/devotions/${devotionId}/guidance`)

// Creates the note the first time, and replaces its text after that. Returns { content, createdAt, updatedAt }.
export const saveSummary = (roomId, devotionId, content) =>
  request(`/leader/rooms/${roomId}/devotions/${devotionId}/guidance/summary`, { method: 'PUT', body: { content } })

export const saveReflection = (roomId, devotionId, content) =>
  request(`/leader/rooms/${roomId}/devotions/${devotionId}/guidance/reflection`, { method: 'PUT', body: { content } })

// The member's view: { unlocked, lockedMessage, summary, reflection }. While locked there is no text at all.
export const getMemberGuidance = (roomId, devotionId) =>
  request(`/member/rooms/${roomId}/devotions/${devotionId}/guidance`)
