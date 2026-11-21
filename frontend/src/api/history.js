import { request } from './http'
import { withQuery } from './query'

// My totals this week, this month and all time, and my streak in each room I have been in.
export const getHistorySummary = () => request('/member/participation/summary')

// One line per devotion in one room, newest first. Returns { summary, entries: page }.
export const getRoomHistory = (roomId, params) => request(withQuery(`/member/rooms/${roomId}/history`, params))
