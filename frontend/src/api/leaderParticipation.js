import { request } from './http'

// Everything here is scoped to the logged-in leader by the backend (the id comes from the token).

// Today's devotion and everyone's status, with email and account status
export const getLeaderBoard = (roomId) => request(`/leader/rooms/${roomId}/participation/today`)

// "Exempt for Today" on. Granting twice is harmless.
export const exemptMember = (roomId, memberId) =>
  request(`/leader/rooms/${roomId}/members/${memberId}/exemption`, { method: 'PUT' })

// "Exempt for Today" off. Removing twice is harmless.
export const removeExemption = (roomId, memberId) =>
  request(`/leader/rooms/${roomId}/members/${memberId}/exemption`, { method: 'DELETE' })

// { devotion, submissions }: every answer to one devotion, oldest first
export const getDevotionAnswers = (roomId, devotionId) =>
  request(`/leader/rooms/${roomId}/devotions/${devotionId}/submissions`)
