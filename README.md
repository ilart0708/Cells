
## Organization design (Phase 3)
- Accounts are created by **invitation only**. Nobody receives a temporary password: the invited
  person opens a link and chooses their own password.
- Invitation links are single-use and expire after 7 days. Only a SHA-256 hash is stored, and the
  link is shown once, in the response that creates it. Inviting the same email again replaces
  that inviter's older pending link.
- Hierarchy: Administrator -> Pastor -> Cell Group Leader. A pastor belongs to one church, and a
  leader belongs to one pastor in that pastor's church (the church is never taken from the request).
- A pastor sees only their own leaders and invitations. Another pastor's record answers 404, not 403.
- Disabling a user blocks login and refresh immediately. An access token already issued lasts up to
  15 minutes more.

## Frontend structure
- `api/` one file per backend area, all calls go through `api/http.js` (adds the token, refreshes once on 401)
- `stores/` Pinia (the auth store keeps the access token in memory only)
- `composables/` `usePagedList` (lists with search), `useStatusToggle` (enable/disable flow)
- `components/` shared UI that follows the CELLS Design System Guide
- `layouts/` one layout per role, `views/<role>/` the pages of each role

## Postman
Import `docs/postman/cells.postman_collection.json`. Set the environment variable `baseUrl` to
`http://localhost:8080/api/v1`. The login, refresh and accept requests store `accessToken` automatically.

## Optional setting
| Variable | Purpose |
| --- | --- |
| `APP_FRONTEND_URL` | Base URL used in invitation links (default `http://localhost:5173`) |

## Organization design (Phase 3)
- Accounts are created by **invitation only**. Nobody receives a temporary password: the invited
  person opens a link and chooses their own password.
- Invitation links are single-use and expire after 7 days. Only a SHA-256 hash is stored, and the
  link is shown once, in the response that creates it. Inviting the same email again replaces
  that inviter's older pending link.
- Hierarchy: Administrator -> Pastor -> Cell Group Leader. A pastor belongs to one church, and a
  leader belongs to one pastor in that pastor's church (the church is never taken from the request).
- A pastor sees only their own leaders and invitations. Another pastor's record answers 404, not 403.
- Disabling a user blocks login and refresh immediately. An access token already issued lasts up to
  15 minutes more.

## Frontend structure
- `api/` one file per backend area, all calls go through `api/http.js` (adds the token, refreshes once on 401)
- `stores/` Pinia (the auth store keeps the access token in memory only)
- `composables/` `usePagedList` (lists with search), `useStatusToggle` (enable/disable flow)
- `components/` shared UI that follows the CELLS Design System Guide
- `layouts/` one layout per role, `views/<role>/` the pages of each role

## Postman
Import `docs/postman/cells.postman_collection.json`. Set the environment variable `baseUrl` to
`http://localhost:8080/api/v1`. The login, refresh and accept requests store `accessToken` automatically.

## Optional setting
| Variable | Purpose |
| --- | --- |
| `APP_FRONTEND_URL` | Base URL used in invitation links (default `http://localhost:5173`) |
