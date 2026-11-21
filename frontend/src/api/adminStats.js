import { request } from './http'
import { withQuery } from './query'

// Administrator only. Counts and participation records: never the text of an answer.

// { periods, churches, pastors, disabledPastors, totals }
export const getAdminDashboard = () => request('/admin/dashboard')

// Each list returns { periods, items: a page of lines }. Alphabetical, searchable.
export const listChurchStats = (params) => request(withQuery('/admin/stats/churches', params))
export const listPastorStats = (params) => request(withQuery('/admin/stats/pastors', params))
export const listLeaderStats = (params) => request(withQuery('/admin/stats/leaders', params))
export const listRoomStats = (params) => request(withQuery('/admin/stats/rooms', params))

// { periods, leader, rooms }
export const getAdminLeaderStats = (leaderId) => request(`/admin/stats/leaders/${leaderId}`)

// { periods, room, todayDevotion, days }
export const getAdminRoomStats = (roomId) => request(`/admin/stats/rooms/${roomId}`)

// { today, members: a page of member lines }
export const listAdminMembers = (params) => request(withQuery('/admin/members', params))

// { today, member }
export const getAdminMember = (memberId) => request(`/admin/members/${memberId}`)

// A page of { submissionId, submittedAt, devotionDate, roomId, roomName, churchName, reference, title, points }
export const listMemberSubmissions = (memberId, params) =>
  request(withQuery(`/admin/members/${memberId}/submissions`, params))
