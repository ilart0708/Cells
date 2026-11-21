import { request } from './http'

// The same board members see, for one of my rooms. Members who chose to hide are not listed.
export const getLeaderRoomBoard = (roomId) => request(`/leader/rooms/${roomId}/board`)
