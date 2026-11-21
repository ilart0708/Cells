import { request } from './http'
import { withQuery } from './query'

// Scoped to the logged-in pastor by the backend (the id comes from the token).
// Counts only: no member is ever named in these responses.

// { periods, totals, leaders: page of leader lines }
export const getPastorDashboard = (params) => request(withQuery('/pastor/dashboard', params))

// { periods, leader, rooms }
export const getLeaderStats = (leaderId) => request(`/pastor/leaders/${leaderId}/stats`)

// { periods, room, todayDevotion, days }
export const getRoomStats = (roomId) => request(`/pastor/rooms/${roomId}/stats`)
