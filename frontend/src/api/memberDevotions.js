import { request } from './http'
import { withQuery } from './query'

// Scoped to the logged-in member by the backend (the id comes from the token).
// The server only returns today's and earlier devotions, never the leader's preparation.

// { today: "2026-11-19", devotion: {...} | null }: null when nothing is posted today
export const getTodayDevotion = (roomId) => request(`/member/rooms/${roomId}/devotions/today`)

export const listMemberDevotions = (roomId, params) =>
  request(withQuery(`/member/rooms/${roomId}/devotions`, params))

export const getMemberDevotion = (roomId, id) => request(`/member/rooms/${roomId}/devotions/${id}`)
