# Google Play Console answers for CELLS

Draft answers, written from the code and the privacy policy as of 25 Nov 2026. Re-check each one against the Play Console
wording when you fill it in, because Google changes its forms. Nothing here has been submitted.

## Before you start
- Personal developer account: first release needs a **closed test with 12 testers opted in for 14 days** before you can apply for production.
- Needed URLs: privacy policy `https://cells-alliance.netlify.app/privacy`, delete account `https://cells-alliance.netlify.app/delete-account`.
- Still to do in the app: launcher icon (1024 px source), splash, transparent notification icon, signed `.aab` (see `docs/android-app.md`).

## Store listing
- **App name:** CELLS
- **Short description (max 80):** Daily Bible devotion for church cell groups. Share the Word together.
- **Full description (max 4000):**

CELLS helps church cell groups read the Bible and share what they understand, one day at a time.

A cell group leader shares a Bible passage. Each member reads it and writes what they understood, in turn, following the group's participation order. Everyone is encouraged to take part, not just a few voices.

For members
- Read today's passage and write your understanding
- See whose turn it is in your group
- Follow your own participation history and experience points (EXP)
- Optional reminders when a devotion is posted or it is your turn

For cell group leaders
- Create rooms for your groups and invite members
- Set the participation order and share a passage with a learning level
- Give a member a day off when they cannot take part
- Optionally add a group summary or a personal reflection
- Review reported answers and keep or remove them

For pastors and administrators
- See participation statistics for leaders and rooms
- Pastors see statistics, not members' written answers

Safe and respectful: members can report an answer, and the room's leader decides whether to keep it or remove it. The person who reported is never shown.

Members join by invitation from their leader. Leaders join by invitation from their pastor or church administrator, or create their own group by naming their church and pastor; a group created this way is marked "not verified" until an administrator verifies it. EXP shows that you took part. It is not a measure of faith and there is no public leaderboard.

Grow Together. Share the Word. Build a Stronger Community.

- **Category:** Lifestyle (or Books & Reference). **Tags:** religion/spirituality.
- **Contact email / website:** use the address in `frontend/src/constants/contact.js`; website `https://cells-alliance.netlify.app`.
- **Graphics (made 25 Nov 2026):** icon `app-icon-512x512.png`, feature graphic `feature-graphic-1024x500.png`, and 8 phone screenshots at 1080x2100
  (1 to 4 member screens, 5 to 8 leader screens, ending with Reported answers). Upload them in that order; the first two show most in the listing.
  Screenshots come from a test room, so the names Hazel and Clark appear in them.

## App access
Members join only from an invitation, and a leader can sign up themselves ("Create your group"), so give Google test logins:
- Create on the live system: one church, one **member** and one **leader** account, one active room with both in it, and a devotion for today.
- Enter the emails and passwords in Play Console > App content > App access. Do not reuse a real person's account.
- Keep the member first in the participation order so the reviewer can submit.

## Data safety form
Collected, encrypted in transit (HTTPS), user can request deletion (in app, and on the web page above). No data sold. Service
providers (hosting, database, Firebase push) process data for you and are not "sharing" under Google's definition.

| Data type (Play Console name) | Collected | Purpose | Optional? |
| --- | --- | --- | --- |
| Personal info > Name | Yes | App functionality, account management | No |
| Personal info > Email address | Yes | App functionality, account management | No |
| Personal info > User IDs (account id) | Yes | App functionality | No |
| App activity > Other user-generated content (written answers, leader summaries and reflections, report reasons and notes) | Yes | App functionality | Members write answers as part of the app; report notes are optional |
| Device or other IDs (push token) | Yes | App functionality (notifications) | Yes, only if notifications are allowed |
| IP address (held briefly in memory for sign-in rate limiting; see the privacy policy) | Treat as collected | Security | No |

Judgement call for you: written answers are Bible reflections, so they can reveal religious beliefs. Playing it safe means also ticking
**Personal info > Political or religious beliefs** (collected, app functionality, not shared). Declaring too much is not penalised;
declaring too little can be. The privacy policy needs no change either way.

Before you submit: make sure `APP_RATE_LIMIT_DEBUG` is off on Render. With it on, visitor IP addresses are written to the server log, which the privacy policy does not describe.

Shared with third parties: No. Firebase Cloud Messaging only delivers the push token's notifications for you, so it counts as a service provider.
Not collected: location, contacts, photos, files, audio, health, financial info, browsing history, advertising ID, analytics.

Other answers: "Is all data encrypted in transit?" Yes. "Do you provide a way to request data deletion?" Yes, with both URLs above.

## Content rating (IARC questionnaire)
Category: utility/reference or social. Answers: no violence, no sexual content, no profanity, no drugs/gambling, no
location sharing, no in-app purchases. Religious content: yes (Bible study; not promoting hate). Users can interact and
share text with each other (closed invited groups). Expect a rating of Everyone / PEGI 3 or similar; accept whatever
the questionnaire returns.

## Target audience and content
- **Target age:** 18 and over (matches the privacy policy). Not designed for children, so no Families policy.
- **Ads:** none. **Contains in-app purchases:** no. **News app / government / health app / financial features:** no.

## User-generated content (report and removal)
Members' written answers are visible to the rest of their group, so Google's user-generated-content policy applies.
CELLS has an in-app way to report an answer (Report button on every other member's answer, with a reason and an optional note)
and the room's leader can keep it or remove its text from Room > Reports. A removed answer shows "This answer was removed by the leader."
and still counts for participation and EXP. The reporter is never shown. Use this in the review notes and in the Play Console UGC question.
Reasons offered: Offensive or hateful, Harassment or bullying, Inappropriate or explicit, Spam or not related to the passage, Something else.
Limits to be honest about:
- Reports go to the room's leader, not to an administrator, and there is no per-user block.
- Closed groups only: nobody can read a room's answers without an invitation, and a member sees others' answers only after sharing their own.
- Leaders who sign up themselves agree to short community rules (no hateful, harassing or sexual content; review reports) on the
  "Create your group" page, and an unverified group is limited to 3 rooms and 30 members until an administrator verifies it.
- **Remaining gap:** Google's UGC policy expects user terms that forbid objectionable content, accepted before posting. Members
  (who write the answers) and invited leaders do not see or accept the rules yet. The cheapest fix is the same rules text and an accept
  checkbox on the accept-invitation page. Do this before production if the review asks for it.
- The typed pastor name and optional pastor email of a self-registered group belong to a third person. They are only used to show who
  the group answers to; the email is visible to administrators only. No new Data safety category is needed (name and email are already declared).

## Release checklist
1. Create app in Play Console (name, default language, free).
2. Fill App content: privacy policy, app access, ads, content rating, target audience, Data safety, account deletion URL.
3. Store listing text and graphics above.
4. Upload signed `.aab` to the closed test track, add 12+ testers (a Google Group or email list), keep them opted in 14 days.
5. Apply for production once the test requirement is met.
