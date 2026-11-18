import { request } from './http'
import { withQuery } from './query'

// Everything here is scoped to the logged-in leader by the backend (the id comes from the token).

export const getRoster = (roomId) => request(`/leader/rooms/${roomId}/members`)

export const listAvailableMembers = (roomId, params) =>
  request(withQuery(`/leader/rooms/${roomId}/members/available`, params))

export const addMember = (roomId, memberId) =>
  request(`/leader/rooms/${roomId}/members`, { method: 'POST', body: { memberId } })

// memberIds: every current member's id, exactly once, in the new order.
export const saveOrder = (roomId, memberIds) =>
  request(`/leader/rooms/${roomId}/members/order`, { method: 'PUT', body: { memberIds } })

export const removeMember = (roomId, memberId) =>
  request(`/leader/rooms/${roomId}/members/${memberId}`, { method: 'DELETE' })

// No churchId: the person joins this room's church.
export const inviteMember = (roomId, { email, fullName }) =>
  request(`/leader/rooms/${roomId}/invitations`, { method: 'POST', body: { email, fullName } })

export const listRoomInvitations = (roomId, params) =>
  request(withQuery(`/leader/rooms/${roomId}/invitations`, params))

export const revokeRoomInvitation = (roomId, id) =>
  request(`/leader/rooms/${roomId}/invitations/${id}`, { method: 'DELETE' })
