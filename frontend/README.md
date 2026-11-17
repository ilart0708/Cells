# frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
## Environment variables (backend)

| Variable | Needed | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | Production (local has a dev fallback) | HS256 signing key, at least 32 bytes, random |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | First start | Creates the first Administrator if missing (12+ character password) |
| `BOOTSTRAP_ADMIN_NAME` | Optional | Display name of that administrator |
| `DEV_SEED_PASSWORD` | Local only | Creates test users: pastor, leader, member |
| `REFRESH_COOKIE_SECURE` | Production = `true` | Refresh cookie sent only over HTTPS |
| `DB_PASSWORD` | Production | PostgreSQL password |

## Auth design (Phase 2)
- Access token: JWT, 15 minutes, kept in memory only (never localStorage).
- Refresh token: random value in an httpOnly, SameSite=Strict cookie scoped to `/api/v1/auth`;
  stored hashed in `refresh_tokens`, rotated on every use, reuse revokes the whole family.
- Every endpoint is denied by default. Every new controller method needs `@PreAuthorize`
  (enforced by `EndpointSecurityGuardTest`).