import { request } from './http'

// Growing Together for one of my rooms, for this month.
export const getBoard = (roomId) => request(`/member/rooms/${roomId}/board`)

// Show me on my rooms' boards (true) or hide me (false). Repeating it is harmless.
export const saveBoardSetting = (showOnBoard) =>
  request('/member/settings/board', { method: 'PUT', body: { showOnBoard } })
