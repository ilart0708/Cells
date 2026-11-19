import { request } from './http'

// Scoped to the logged-in member by the backend (the id comes from the token).

// Today's devotion, my status, and everyone's status in participation order
export const getParticipationBoard = (roomId) => request(`/member/rooms/${roomId}/participation/today`)

export const submitUnderstanding = (roomId, devotionId, content) =>
  request(`/member/rooms/${roomId}/devotions/${devotionId}/submissions`, {
    method: 'POST',
    body: { content },
  })

// { unlocked, lockedMessage, submittedCount, submissions }. While locked there is no text, only the count.
export const getRoomAnswers = (roomId, devotionId) =>
  request(`/member/rooms/${roomId}/devotions/${devotionId}/submissions`)
