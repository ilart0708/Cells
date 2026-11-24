# Google Play Console answers for CELLS

Draft answers, written from the code and the privacy policy as of 24 Nov 2026. Re-check each one against the Play Console
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

For pastors and administrators
- See participation statistics for leaders and rooms
- Pastors see statistics, not members' written answers

CELLS is by invitation only: your leader, pastor or church administrator sends you a link to join. EXP shows that you took part. It is not a measure of faith and there is no public leaderboard.

Grow Together. Share the Word. Build a Stronger Community.

- **Category:** Lifestyle (or Books & Reference). **Tags:** religion/spirituality.
- **Contact email / website:** use the address in `frontend/src/constants/contact.js`; website `https://cells-alliance.netlify.app`.
- **Graphics needed:** icon 512x512, feature graphic 1024x500, at least 2 phone screenshots (take them from the real app, one per role).

## App access (reviewers cannot self-register)
Accounts are created only from an invitation, so give Google test logins:
- Create on the live system: one church, one **member** and one **leader** account, one active room with both in it, and a devotion for today.
- Enter the emails and passwords in Play Console > App content > App access. Do not reuse a real person's account.
- Keep the member first in the participation order so the reviewer can submit.

## Data safety form
Collected, encrypted in transit (HTTPS), user can request deletion (in app, and on the web page above). No data sold. Service
providers (hosting, database, Firebase push) process data for you and are not "sharing" under Google's definition.

| Data type | Collected | Purpose | Optional? |
| --- | --- | --- | --- |
| Name | Yes | App functionality, account management | No |
| Email address | Yes | App functionality, account management | No |
| User IDs (account id) | Yes | App functionality | No |
| Other in-app content (written answers, summaries, reflections) | Yes | App functionality | Members write answers as part of the app |
| Device or other IDs (push token) | Yes | App functionality (notifications) | Yes, only if notifications are allowed |
| IP address (brief, for sign-in rate limiting) | Treat as collected | Security | No |

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
CELLS has an in-app way to report an answer (Report button on every other member's answer, with a reason) and the room's
leader can keep it or remove its text from Room > Reports. The reporter is never shown. Use this in the review notes and
in the Play Console UGC question. Limits to be honest about: reports go to the room's leader (not to an administrator),
and there is no per-user block.

## Release checklist
1. Create app in Play Console (name, default language, free).
2. Fill App content: privacy policy, app access, ads, content rating, target audience, Data safety, account deletion URL.
3. Store listing text and graphics above.
4. Upload signed `.aab` to the closed test track, add 12+ testers (a Google Group or email list), keep them opted in 14 days.
5. Apply for production once the test requirement is met.
