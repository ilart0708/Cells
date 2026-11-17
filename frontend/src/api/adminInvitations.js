import { request } from './http'
import { withQuery } from './query'

export const listInvitations = (params) => request(withQuery('/admin/invitations', params))

export const invitePastor = ({ email, fullName, churchId }) =>
  request('/admin/invitations', { method: 'POST', body: { email, fullName, churchId } })

export const revokeInvitation = (id) => request(`/admin/invitations/${id}`, { method: 'DELETE' })
