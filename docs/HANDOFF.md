# CELLS - Handoff status (last updated 26 November 2026, evening)

For anyone (or any Claude session, on any account) continuing this work. Read this first. The Roadmap, Product Concept and Design System Guide are the source of truth for product rules. A copy of this note also lives in the Claude Project "Cells" as `claude/CELLS - Handoff Status (26 Nov 2026).md`.

**Rule for every session:** at the end of each piece of work, update this file (and the Project copy) with what changed, what is open and which branch holds it.

## Stack and hosting
- Frontend: Vue 3 + Vite on Netlify (https://cells-alliance.netlify.app). `frontend/netlify.toml` proxies `/api/*` to the backend. **Netlify auto-publish is OFF and the owner has no Netlify credits: the owner publishes the frontend by hand, in bulk.**
- Backend: Spring Boot 4 / Java 21, PostgreSQL (Neon), Flyway, on Render (https://cells-d3m6.onrender.com, free tier, slow to wake).
- Android app: Capacitor 8, `com.cells.app`, same Vue app (`npm run build:native`). After frontend changes run `npm run cap:sync` and rebuild.
- Repo: github.com/ilart0708/Cells. A Claude session pushes feature or release branches only, never `main`; the owner opens and merges the PR.

## Working rules the owner set
- Base work on the Roadmap, Product Concept and Design System Guide.
- Skip backend and frontend tests for now (repo CLAUDE.md).
- Any new endpoint needs a Postman request (folder, then request, with a cURL in the description) in `docs/postman/cells.postman_collection.json`.
- Never paste secrets into chats or Git.
- Claude's sandbox has no network: the backend and frontend cannot be compiled or built there. Code is reviewed by hand. The owner's local build, Render deploy and Netlify build are the real checks. If a build fails, give the log to Claude and fix it on the same branch.
- A Stop hook sometimes claims "unpushed commits". It has been wrong every time; verify with `git ls-remote --heads origin <branch>` against `git rev-parse HEAD`.
- Screenshots for Play are done; the owner does not want them retaken.

## Current state of work (26 Nov 2026)

### Branch `feature/notification-icon` (pushed, NOT merged; built on `feature/leader-dashboard-status`)
Android only: `res/drawable/ic_stat_cells.xml` (white CELLS mark on transparent), `res/values/colors.xml` (navy accent) and two Firebase `default_notification_*` meta-data entries in `AndroidManifest.xml`, so push notifications no longer show the launcher icon as a white square. Needs `npm run cap:sync` and a rebuild; check on a real phone by sending a test push (see `docs/android-app.md`).

### Branch `feature/leader-dashboard-status` (pushed, NOT merged, NOT deployed; built on `feature/level-badges-header`)
Frontend only. The leader dashboard (`views/LeaderDashboardView.vue`) was just a welcome card and a button. It now shows, for each active room (up to 20): room title, today's devotion, a "Participation today" progress bar (exempt members not counted), whose turn it is, a warning when the line is stuck on a disabled account, and every member with number, name and status pill (Submitted, Their turn, Waiting, Exempted). Top tiles: active rooms, members, shared today, still to share. Buttons: Today's board, Open room, All my rooms. It reuses `GET /leader/rooms` and `GET /leader/rooms/{id}/participation/today` (one call per room, on load and every 30 s while the tab is visible), so no backend change and no Postman request. `PlaceholderDashboard.vue` is no longer used by the leader.

### Branch `feature/level-badges-header` (pushed, NOT merged, NOT deployed; built on `feature/answer-reactions`)
Frontend only, no backend or database change, so no Postman request.
- `LevelBadge.vue`: round badge icons for the five levels (Seed, Sprout, Branch, Tree, Fruitful), grey when locked. Used on the Experience page (big on the hero card, one per row of the ladder) and in `GrowthLevelPill`.
- Member header (`AppShell.vue`): order is Name, level badge, streak flame, role, Log out. Badge links to Experience, streak links to History. Streak = the longest current streak (devotions in a row) across the member's active rooms. Data comes from the existing `/member/experience` and `/member/participation/summary` through `stores/progress.js` (reset on logout, refreshed after sharing an answer). Other roles keep the old header.
- Not compiled or built by Claude.

### Branch `feature/answer-reactions` (pushed, NOT merged, NOT deployed; built on top of `feature/answer-highlights`)
Members and the room's leader can react to a shared answer with Amen 🙏, Encouraged 💛, Helpful 💡 or Praying 🕊️.
- Rules (owner chose "B"): a member can react once they could read the answers (shared their own, exempt, or day over); the leader any time in their own active room. One reaction per person per answer, tapping another changes it, tapping the same takes it back. Not on your own answer or a removed answer. Counts are shown, who reacted is never returned. No EXP, no notifications.
- Migration `V25__answer_reactions.sql` (unique per answer and user, cascades on answer or account delete). Package `reaction/` (`ReactionService`, upsert is one atomic SQL statement). Endpoints `PUT/DELETE /member|leader/rooms/{roomId}/submissions/{id}/reaction` return `{counts, mine}`; answer lists now carry `reactions`.
- Frontend: `ReactionBar.vue` under every answer (member panel and leader answers), `api/reactions.js`, `constants/reactions.js`. Postman folder "Reactions (shared answers)". Privacy policy and Play docs updated.
- Not compiled or built by Claude. Merge order: `feature/answer-highlights` first (this branch contains it), or merge this one alone since it includes both.

### Branch `feature/answer-highlights` (pushed, NOT merged, NOT deployed)
Members can mark key sentences in their own answer and add a one-line key takeaway; the leader and the other members see them.
- Migration `V24__answer_highlights.sql`: `devotion_submissions.takeaway` (200) and `highlights` (text "start-end,start-end"). Additive, so the live website keeps working after the backend deploys.
- `POST .../submissions` accepts optional `takeaway` and `highlights` (max 5 non-overlapping character ranges of the trimmed answer, checked by `Highlights.validated`, 400 with `errors.highlights`). Written once with the answer, never edited. A leader removing a reported answer clears both.
- Answer responses (own, room list, leader list) now include `takeaway` and `highlights`.
- Frontend: highlight tool and takeaway box in `SubmissionForm` (select text, tap Highlight; editing the text clears the marks), new `AnswerText.vue` used everywhere an answer is shown, `utils/highlights.js`. Privacy policy sentence and Postman submit request updated.
- Not compiled or built by Claude (node_modules is incomplete in the sandbox). Check the Render build and the next Netlify publish. Old frontend plus new backend is safe; new frontend needs the new backend.

### Merged and deployed: `release/self-registration-and-rules` (PRs #39 and #40)
The backend is deployed on Render (owner confirmed 26 Nov). Netlify publish is still manual and pending, so "Create your group", the rules checkbox and the "not verified" label are not on the website yet. After the website is published, make `acceptRules` required again.

### Earlier description of that release (for reference)
One combined branch from `main`, ready for one PR. It supersedes `feature/leader-self-registration` and `feature/rules-on-invitation` (older, can be deleted after merge).

1. **Leader self-registration** (`POST /api/v1/auth/register-leader`, public, rate-limited):
   - A leader creates their own group without an invitation: name, email, password, church name, pastor name (required, typed text, not an account), optional pastor email (admin only), and a required "I agree" to community rules.
   - Always creates a NEW church (never joins one by name). The church starts "not verified". `churches` gains `self_registered`, `verified`, `pastor_name`, `pastor_email` (migration `V23__self_registered_churches.sql`; name uniqueness now only for admin-created churches).
   - Limits while unverified: 3 active rooms, 30 members (pending member invitations count). See `church/UnverifiedLimits`.
   - Admin: `PUT /api/v1/admin/churches/{id}/verification` and a Verify / Remove verification button on Church Management. Leader list marks "(not verified)". Leader and members see "Pastor: Name · not verified" under the room title.
   - Deleting the last leader of a self-registered church also removes the typed pastor name and email.
   - Public page `/register` ("Create your group"), linked from the login page.
2. **Community rules on invitation accept:** the accept-invitation page shows role-specific rules (member, invited leader, pastor) with an agree checkbox and sends `acceptRules`. **The server treats `acceptRules` as optional on `/invitations/accept`** so the website currently live on Netlify (which does not send it) keeps working. Make it required again (`@AssertTrue` in `AcceptInvitationRequest`) only after the new frontend is published. Accounts that existed before this have not seen the rules.
3. Privacy policy, delete-account page, `docs/play-store.md` and Postman were updated to match.

**Deploy order when the owner is ready:** merge the PR, let Render deploy and check the log that Flyway applied V23, then publish the frontend on Netlify. Until Netlify is published only the new website pages (Create your group, rules checkbox, "not verified" label) are missing; nothing existing breaks. Not built or compiled by Claude: expect to fix small compile errors if the Render or local build complains.

### Earlier work already merged
Phase 9 (daily cycle and notifications), Capacitor app, Firebase push, hardening, privacy policy edits and reports text (PRs #37/#38), leader dashboard placeholder removed, Play Store answers in `docs/play-store.md`.

## Google Play status
- Done: privacy policy and delete-account pages, Data safety and store listing drafts (`docs/play-store.md`), graphics and 8 screenshots (delivered as a zip), tester invitation message.
- Launcher icon and splash exist (`frontend/assets`). Notification icon added on `feature/notification-icon`. Still to do: signed `.aab` (`docs/android-app.md`); create test accounts for Google's App access; start the closed test (12 testers opted in for 14 days; personal account created after 13 Nov 2023); then apply for production.
- Before submitting: `APP_RATE_LIMIT_DEBUG` must be off on Render.

## Open items
1. Publish the frontend on Netlify (owner, manual, in bulk). Afterwards make `acceptRules` required on the server. Merge and deploy `feature/notification-icon` (it includes `feature/leader-dashboard-status`, `feature/level-badges-header`, `feature/answer-reactions` and `feature/answer-highlights`) before or together with that publish.
2. Rate limiter: DONE. The owner verified on 27 Nov 2026 that Render has `APP_RATE_LIMIT_CLIENT_IP_HEADER=CF-Connecting-IP`, that `APP_RATE_LIMIT_NETLIFY_SIGN_SECRET` matches the Netlify variable, and that `APP_RATE_LIMIT_DEBUG` is not set (debug off). Optional re-test: 12 wrong logins in a row should end in 429.
3. `fix/android-allow-backup` (`android:allowBackup="false"`) if not merged yet.
4. Real SMTP and a custom domain (`MAIL_ENABLED=false` on Render); Android App Links for invitation links.
5. Push retry can duplicate a notification on a second phone when one phone's send fails.
6. One moderate Dependabot alert on the default branch.
7. Self-registered groups: no real pastor verification yet (needs working email). Reports go to the room's leader only; there is no per-user block.
8. Roadmap Phase 10 (audit log, CI, further hardening) not started beyond the above.
