import { Capacitor } from '@capacitor/core'

/** True only inside the Android/iOS app. On the website every native feature stays switched off. */
export const isNative = Capacitor.isNativePlatform()

/** 'android', 'ios' or 'web' */
export const platformName = Capacitor.getPlatform()

/**
 * Where the API lives. Empty on the website: it calls /api on its own address (Vite proxy in development,
 * Netlify redirect in production). The app is served from https://localhost inside the phone, so it needs the
 * backend's real address, set at build time by .env.native (npm run build:native).
 */
export const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
