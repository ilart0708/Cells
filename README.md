
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
`http://localhost:8080/api/v1` (the collection variable of the same name is already set). The Login request stores
`accessToken` automatically. Every request carries its cURL in its description.

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
`http://localhost:8080/api/v1` (the collection variable of the same name is already set). The Login request stores
`accessToken` automatically. Every request carries its cURL in its description.

## Optional setting
| Variable | Purpose |
| --- | --- |
| `APP_FRONTEND_URL` | Base URL used in invitation links (default `http://localhost:5173`) |

## Participation engine design (Phase 6)
- The "cycle" is the room's active devotion for today. Member #1 can always submit. Everyone else waits
  until the nearest earlier member who is not exempt has submitted. One pure function
  (`ParticipationRules.evaluate`) answers both "may this member submit?" and "what status do I show?".
- Statuses: SUBMITTED, ELIGIBLE, WAITING, EXEMPTED. An exempt member neither waits nor blocks.
- The order is read at the moment of each submission, so the leader may reorder or remove members while
  the day is in progress. Submitting, exempting, reordering, adding and removing in one room all take the
  same row lock on the room, so they take turns. A unique index (one answer per member per devotion) is
  the second line of defence.
- There is no participation_records table: a member's status for a day is derived from submissions,
  exemptions and the room roster. No job runs at midnight: the day is the server's date (app.clock.zone)
  and exemptions are keyed by date, so they expire by themselves.
- "Exempt for Today" (leader only, today only): granted and removed with PUT and DELETE (both harmless
  when repeated), recorded with who and when (revoked rows stay as history). A member who has already
  submitted cannot be exempted. An exempt member cannot submit until the exemption is removed.
- An answer is final: there is no edit and no delete. 1 to 2000 characters, plain text.
- Once anyone has answered, today's devotion can no longer be edited or cancelled.
- Reading answers: the leader reads all answers of their own room. A member reads the room's answers
  after sharing their own, while exempt, or once the day is over; until then the server sends no text,
  only a count. Members never get emails or ids. Pastors and administrators never read the text.
- The member and leader screens refresh every 30 seconds while the tab is visible.

## Daily cycle and notifications (Phase 9)
- **Midnight cycle.** At 12:00 AM in `APP_TIMEZONE` (default `Asia/Manila`) a scheduled job opens the new day and
  announces devotions that leaders prepared in advance. It also runs once at startup, so a restart or deploy
  at midnight cannot skip a day. It never changes history: submissions, exemptions and EXP are keyed by date.
  `GET /api/v1/clock/today` now also returns `nextResetAt`; the app shows a countdown.
- **Who is notified (email and phone push):**
  | Event | Who |
  | --- | --- |
  | Invitation created (pastor, leader or member) | The invitee, by email only (registration link) |
  | Devotion opens (created today, moved to today, or its day starts) | Members: "your turn" if they can submit now, otherwise "devotion open" |
  | A member submits or the leader changes an exemption | The member(s) whose turn it now is |
  | Leader saves the group summary or personal reflection (first save only) | The room's members |
  Messages never contain anyone's answer or the leader's text; they link into the app.
- **Reliability.** Notifications are saved with the action that caused them and sent after it commits, so a mail
  problem never undoes a submission. Failures are retried every 2 minutes, up to 5 attempts. Each event is stored once.
- **Local email with Mailpit.** `docker compose up -d` starts Mailpit. The backend's defaults already point to it
  (`localhost:1025`). Read every sent email at http://localhost:8025. Nothing reaches a real inbox.
- **Phone push** is off (`PUSH_ENABLED=false`) until the mobile app exists. The app registers its Firebase token with
  `POST /api/v1/devices` after login and removes it with `DELETE /api/v1/devices/{token}` at logout.

| Variable | Purpose | Default |
| --- | --- | --- |
| `MAIL_ENABLED` | Send emails at all | `true` locally, `false` in prod until SMTP is set |
| `MAIL_HOST`, `MAIL_PORT` | SMTP server | `localhost`, `1025` (Mailpit) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP login (never commit them) | empty |
| `MAIL_AUTH`, `MAIL_STARTTLS` | Set both to `true` for a real provider (usually port 587) | `false` |
| `MAIL_FROM` | From header, for example `CELLS <no-reply@yourdomain>` | `CELLS <no-reply@cells.local>` |
| `PUSH_ENABLED` | Turn on phone push once a Firebase sender is installed | `false` |

**Going live with a real SMTP provider.** Buy a domain, add the provider's SPF, DKIM and DMARC DNS records, create SMTP
credentials, then set the `MAIL_*` variables and `APP_FRONTEND_URL`. A `netlify.app` address cannot be authenticated, so it
cannot be the sender domain.

## Android app (Capacitor)
The same Vue app is packaged with Capacitor as `com.cells.app`. Setup, Firebase, running on a phone and Play Store
steps are in [docs/android-app.md](docs/android-app.md). Backend side: CORS for the app's origin
(`CORS_ALLOWED_ORIGINS`, defaults already include `https://localhost` and `capacitor://localhost`) and a native
sign-in mode: with the header `X-Client-Type: native` the refresh token is returned and accepted in the request body
instead of the cookie. That mode is honored only for the app's own origins (or requests with no Origin, such as Postman),
so a script on the website cannot ask for the refresh token. The website is unchanged.
