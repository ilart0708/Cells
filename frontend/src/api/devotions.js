import { request } from './http'
import { withQuery } from './query'

// Everything here is scoped to the logged-in leader by the backend (the id comes from the token).
// status: ACTIVE (default) | CANCELLED | ALL

export const listDevotions = (roomId, params) =>
  request(withQuery(`/leader/rooms/${roomId}/devotions`, params))

export const getDevotion = (roomId, id) => request(`/leader/rooms/${roomId}/devotions/${id}`)

export const createDevotion = (roomId, body) =>
  request(`/leader/rooms/${roomId}/devotions`, { method: 'POST', body })

export const updateDevotion = (roomId, id, body) =>
  request(`/leader/rooms/${roomId}/devotions/${id}`, { method: 'PUT', body })

export const setDevotionStatus = (roomId, id, status) =>
  request(`/leader/rooms/${roomId}/devotions/${id}/status`, { method: 'PATCH', body: { status } })
