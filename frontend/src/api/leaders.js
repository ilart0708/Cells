import { request } from './http'
import { withQuery } from './query'

export const listLeaders = (params) => request(withQuery('/admin/leaders', params))

export const setLeaderStatus = (id, status) =>
  request(`/admin/leaders/${id}/status`, { method: 'PATCH', body: { status } })
