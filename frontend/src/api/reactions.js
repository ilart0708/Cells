import { request } from './http'

// React to a shared answer. One reaction per person; the answer is the summary
// { counts: { AMEN, ENCOURAGED, HELPFUL, PRAYING }, mine } for that answer.
// role is 'member' or 'leader' (the leader reacts in their own room).
export const reactToAnswer = (role, roomId, submissionId, type) =>
  request(`/${role}/rooms/${roomId}/submissions/${submissionId}/reaction`, {
    method: 'PUT',
    body: { type },
  })

export const clearReaction = (role, roomId, submissionId) =>
  request(`/${role}/rooms/${roomId}/submissions/${submissionId}/reaction`, { method: 'DELETE' })
