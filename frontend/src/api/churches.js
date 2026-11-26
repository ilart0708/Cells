import { request } from './http'
import { withQuery } from './query'

export const listChurches = (params) => request(withQuery('/admin/churches', params))

export const createChurch = (name) =>
  request('/admin/churches', { method: 'POST', body: { name } })

export const setChurchVerified = (id, verified) =>
  request(`/admin/churches/${id}/verification`, { method: 'PUT', body: { verified } })

export const renameChurch = (id, name) =>
  request(`/admin/churches/${id}`, { method: 'PUT', body: { name } })
