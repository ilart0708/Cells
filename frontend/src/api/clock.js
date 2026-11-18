import { request } from './http'

// The platform's own date (Asia/Manila by default). Screens use this, never the browser's clock,
// to decide which days are allowed.
export const getToday = () => request('/clock/today')
