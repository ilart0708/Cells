import { request } from './http'
import { withQuery } from './query'

// Everything here is scoped to the logged-in leader by the backend (the id comes from the token).
// status: ACTIVE (default) | ARCHIVED | ALL

export const listRooms = (params) => request(withQuery('/leader/rooms', params))

export const createRoom = (name) => request('/leader/rooms', { method: 'POST', body: { name } })

export const renameRoom = (id, name) =>
  request(`/leader/rooms/${id}`, { method: 'PUT', body: { name } })

export const setRoomStatus = (id, status) =>
  request(`/leader/rooms/${id}/status`, { method: 'PATCH', body: { status } })

export const getRoom = (id) => request(`/leader/rooms/${id}`)
