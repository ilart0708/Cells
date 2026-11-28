# CELLS - Handoff status (last updated 28 November 2026)

For anyone (or any Claude session, on any account) continuing this work. Read this first. The Roadmap, Product Concept and Design System Guide are the source of truth for product rules. A copy of this note also lives in the Claude Project "Cells" as `claude/CELLS - Handoff Status (28 Nov 2026).md`.

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

## Current state of work (28 Nov 2026)

### Merged into `main` on 27 Nov 2026 (PRs #41 to #44)
All of the stacked branches below are merged. PR #44 contained the whole stack, so none of them needs a separate merge, and the branches can be deleted. None of this was compiled or built by Claude: check the Render build log and the next Netlify build, and fix any small errors on a new branch.

**Deployment status of this batch is not recorded here.** Open item 1 says what to confirm. Note that the new frontend needs the new backend; old frontend plus new backend is safe.

#### PR #44 `feature/notification-icon` (built on `feature/leader-dashboard-status`)
Android only: `res/drawable/ic_stat_cells.xml` (white CELLS mark on transparent), `res/values/colors.xml` (navy accent) and two Firebase `default_notification_*` meta-data entries in `AndroidManifest.xml`, so push notifications no longer show the launcher icon as a white square. Needs `npm run cap:sync` and a rebuild; check on a real phone by sending a test push (see `docs/android-app.md`).

#### PR #43 `feature/leader-dashboard-status`
Frontend only. The leader dashboard (`views/LeaderDashboardView.vue`) was just a welcome card and a button. It now shows, for each active room (up to 20): room title, today's devotion, a "Participation today" progress bar (exempt members not counted), whose turn it is, a warning when the line is stuck on a disabled account, and every member with number, name and status pill (Submitted, Their turn, Waiting, Exempted). Top tiles: active rooms, members, shared today, still to share. Buttons: Today's board, Open room, All my rooms. It reuses `GET /leader/rooms` and `GET /leader/rooms/{id}/participation/today` (one call per room, on load and every 30 s while the tab is visible), so no backend change and no Postman request. `PlaceholderDashboard.vue` is no longer used by the leader.

#### PR #42 `feature/level-badges-header`
Frontend only, no backend or database change, so no Postman request.
- `LevelBadge.vue`: round badge icons for the five levels (Seed, Sprout, Branch, Tree, Fruitful), grey when locked. Used on the Experience page (big on the hero card, one per row of the ladder) and in `GrowthLevelPill`.
- Member header (`AppShell.vue`): order is Name, level badge, streak flame, role, Log out. Badge links to Experience, streak links to History. Streak = the longest current streak (devotions in a row) across the member's active rooms. Data comes from the existing `/member/experience` and `/member/participation/summary` through `stores/progress.js` (reset on logout, refreshed after sharing an answer). Other roles keep the old header.

#### PR #41 `feature/answer-reactions` (includes `feature/answer-highlights`)
Members and the room's leader can react to a shared answer with Amen 🙏, Encouraged 💛, Helpful 💡 or Praying 🕊️.
- Rules (owner chose "B"): a member can react once they could read the answers (shared their own, exempt, or day over); the leader any time in their own active room. One reaction per person per answer, tapping another changes it, tapping the same takes it back. Not on your own answer or a removed answer. Counts are shown, who reacted is never returned. No EXP, no notifications.
- Migration `V25__answer_reactions.sql` (unique per answer and user, cascades on answer or account delete). Package `reaction/` (`ReactionService`, upsert is one atomic SQL statement). Endpoints `PUT/DELETE /member|leader/rooms/{roomId}/submissions/{id}/reaction` return `{counts, mine}`; answer lists now carry `reactions`.
- Frontend: `ReactionBar.vue` under every answer (member panel and leader answers), `api/reactions.js`, `constants/reactions.js`. Postman folder "Reactions (shared answers)". Privacy policy and Play docs updated.

#### `feature/answer-highlights` (merged as part of PR #41)
Members can mark key sentences in their own answer and add a one-line key takeaway; the leader and the other members see them.
- Migration `V24__answer_highlights.sql`: `devotion_submissions.takeaway` (200) and `highlights` (text "start-end,start-end"). Additive, so the live website keeps working after the backend deploys.
- `POST .../submissions` accepts optional `takeaway` and `highlights` (max 5 non-overlapping character ranges of the trimmed answer, checked by `Highlights.validated`, 400 with `errors.highlights`). Written once with the answer, never edited. A leader removing a reported answer clears both.
- Answer responses (own, room list, leader list) now include `takeaway` and `highlights`.
- Frontend: highlight tool and takeaway box in `SubmissionForm` (select text, tap Highlight; editing the text clears the marks), new `AnswerText.vue` used everywhere an answer is shown, `utils/highlights.js`. Privacy policy sentence and Postman submit request updated.

### Merged and deployed: `release/self-registration-and-rules` (PRs #39 and #40)
The backend is deployed on Render (owner confirmed 26 Nov). Netlify publish is still manual and pending, so "Create your group", the rules checkbox and the "not verified" label are not on the website until the frontend is published. After the website is published, make `acceptRules` required again.

### Release details: self-registration and rules (merged)
It supersedes `feature/leader-self-registration` and `feature/rules-on-invitation` (older, can be deleted).

1. **Leader self-registration** (`POST /api/v1/auth/register-leader`, public, rate-limited):
   - A leader creates their own group without an invitation: name, email, password, church name, pastor name (required, typed text, not an account), optional pastor email (admin only), and a required "I agree" to community rules.
   - Always creates a NEW church (never joins one by name). The church starts "not verified". `churches` gains `self_registered`, `verified`, `pastor_name`, `pastor_email` (migration `V23__self_registered_churches.sql`; name uniqueness now only for admin-created churches).
   - Limits while unverified: 3 active rooms, 30 members (pending member invitations count). See `church/UnverifiedLimits`.
   - Admin: `PUT /api/v1/admin/churches/{id}/verification` and a Verify / Remove verification button on Church Management. Leader list marks "(not verified)". Leader and members see "Pastor: Name · not verified" under the room title.
   - Deleting the last leader of a self-registered church also removes the typed pastor name and email.
   - Public page `/register` ("Create your group"), linked from the login page.
2. **Community rules on invitation accept:** the accept-invitation page shows role-specific rules (member, invited leader, pastor) with an agree checkbox and sends `acceptRules`. **The server treats `acceptRules` as optional on `/invitations/accept`** so the website currently live on Netlify (which does not send it) keeps working. Make it required again (`@AssertTrue` in `AcceptInvitationRequest`) only after the new frontend is published. Accounts that existed before this have not seen the rules.
3. Privacy policy, delete-account page, `docs/play-store.md` and Postman were updated to match.

### Earlier work already merged
Phase 9 (daily cycle and notifications), Capacitor app, Firebase push, hardening, privacy policy edits and reports text (PRs #37/#38), leader dashboard placeholder removed, Play Store answers in `docs/play-store.md`.

### Branch `feature/reaction-push-notification` (NOT merged, NOT deployed; built on `main`)
Backend only, no frontend change and no new endpoint, so no Postman request. When someone reacts to a member's shared answer, the author gets a phone push: "Someone reacted to your understanding" (body: room label and passage). It never says who reacted or which reaction. Tapping it opens `/member/rooms/{roomId}/devotions/{devotionId}`.
- Migration `V26__reaction_notification_type.sql` adds `ANSWER_REACTED` to `chk_notifications_type`. Additive.
- `NotificationType.ANSWER_REACTED`, a message in `NotificationMessages`, `NotificationService.answerReacted(roomId, devotionId, authorId)`, called from `ReactionService` after a member or leader reaction. Push only: `NotificationDispatcher` skips email for this type.
- One push per answer: the unique key (user, type, room, devotion, day) stores it once, so a second reactor, a changed reaction or remove-and-add never sends another. Disabled accounts are skipped.
- Not compiled or built by Claude. Deploy order: merge, let Render deploy, check the log that Flyway applied V26. No frontend publish needed, but the Android app needs push enabled on the phone to receive it.

## Google Play status
- Done: privacy policy and delete-account pages, Data safety and store listing drafts (`docs/play-store.md`), graphics and 8 screenshots (delivered as a zip), tester invitation message.
- Launcher icon and splash exist (`frontend/assets`). Notification icon merged in PR #44. Still to do: signed `.aab` (`docs/android-app.md`); create test accounts for Google's App access; start the closed test (12 testers opted in for 14 days; personal account created after 13 Nov 2023); then apply for production.
- Before submitting: `APP_RATE_LIMIT_DEBUG` must be off on Render.

## Open items
1. Deploy and publish. Confirm that Render deployed `main` after PR #44 and that the log shows Flyway applied V24 and V25. Then publish the frontend on Netlify (owner, manual, in bulk) and make `acceptRules` required on the server. Until the publish, none of these are on the website: Create your group, the rules checkbox, the "not verified" label, highlights, reactions, level badges, the new leader dashboard. For the Android app, run `npm run cap:sync` and rebuild to include the notification icon.
2. Rate limiter: DONE. The owner verified on 27 Nov 2026 that Render has `APP_RATE_LIMIT_CLIENT_IP_HEADER=CF-Connecting-IP`, that `APP_RATE_LIMIT_NETLIFY_SIGN_SECRET` matches the Netlify variable, and that `APP_RATE_LIMIT_DEBUG` is not set (debug off). Optional re-test: 12 wrong logins in a row should end in 429.
3. `fix/android-allow-backup` (`android:allowBackup="false"`) if not merged yet.
4. Real SMTP and a custom domain (`MAIL_ENABLED=false` on Render); Android App Links for invitation links.
5. Push retry can duplicate a notification on a second phone when one phone's send fails.
6. One moderate Dependabot alert on the default branch.
7. Self-registered groups: no real pastor verification yet (needs working email). Reports go to the room's leader only; there is no per-user block.
8. Roadmap Phase 10 (audit log, CI, further hardening) not started beyond the above.
