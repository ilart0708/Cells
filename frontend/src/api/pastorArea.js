import { request } from './http'
import { withQuery } from './query'

// Everything here is scoped to the logged-in pastor by the backend (the id comes from the token).

export const listMyLeaders = (params) => request(withQuery('/pastor/leaders', params))

export const setMyLeaderStatus = (id, status) =>
  request(`/pastor/leaders/${id}/status`, { method: 'PATCH', body: { status } })

// No churchId: the leader joins the pastor's own church.
export const inviteLeader = ({ email, fullName }) =>
  request('/pastor/invitations', { method: 'POST', body: { email, fullName } })

export const listMyInvitations = (params) => request(withQuery('/pastor/invitations', params))

export const revokeMyInvitation = (id) => request(`/pastor/invitations/${id}`, { method: 'DELETE' })
