import { request } from './http'
import { withQuery } from './query'

export const listPastors = (params) => request(withQuery('/admin/pastors', params))

export const setPastorStatus = (id, status) =>
  request(`/admin/pastors/${id}/status`, { method: 'PATCH', body: { status } })
