
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
