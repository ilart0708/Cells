import { request } from './http'

// My total EXP, level, progress to the next level, the whole ladder, and EXP per room.
export const getExperience = () => request('/member/experience')
