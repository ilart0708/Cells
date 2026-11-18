import { request } from './http'

// Scoped to the logged-in member by the backend (the id comes from the token).

export const listMyRooms = () => request('/member/rooms')

export const getMyRoom = (roomId) => request(`/member/rooms/${roomId}`)
