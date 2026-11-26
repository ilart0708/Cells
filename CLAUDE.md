# Project Instructions
    Always base your response on the Context, CELLS - Development Roadmanp.md and CELLS - Product Concept and Application Documentation and When doin the frontend design make sure you check the CELLS Design System Guide.md And if there are new endpoint introduced make sure to create a CURL for postman (Folder->Requests). Let's skip all the test for backend and frontend
## Context 
# CELLS Design System Guide

A frontend design reference extracted from the CELLS promotional campaign (Images 1–13). Use it to keep the real app, landing pages and marketing assets visually consistent.

> Note: the app screens in the campaign are illustrative mockups built from the feature brief, not final product UI. Treat them as a visual direction and confirm details against the real app.

## 1. Brand Principles

- **Welcoming, trustworthy, inspirational.** Navy gives stability, gold gives warmth and emphasis, white gives clarity.
- **The app is the hero.** Every composition centers a phone or dashboard.
- **Premium SaaS, not slide deck.** Generous negative space, soft shadows, one clear message per screen.
- **Gold is an accent, never a background fill for large areas.** Use it for emphasis, progress, primary actions and key words in headlines.

## 2. Color Tokens

| Token | Hex | Use |
| --- | --- | --- |
| navy-950 | #071433 | Deepest background edge |
| navy-900 | #0E2250 | Primary brand navy, headers, secondary buttons, sidebars |
| navy-700 | #1B3A78 | Gradient mid-tone, hero cards |
| navy-500 | #3D5FA5 | Hierarchy and chart secondary |
| gold-500 | #D4AF37 | Primary accent, primary buttons, progress fill |
| gold-700 | #8A6D12 | Small gold labels on light backgrounds (better contrast) |
| gold-100 | #FBEFC4 | Status pills, Optional tags |
| gold-50 | #FBF7E8 | Scripture / passage panels (border #E8DDB0) |
| app-bg | #F4F6FB | App screen background |
| surface | #FFFFFF | Cards and inputs |
| border | #DCE3F2 | Card borders |
| border-input | #C5CFE6 | Input borders |
| track | #E3E8F4 | Progress track, Pending pill |
| text-muted | #47557A | Secondary text on light |
| text-on-navy | #E6ECFA | Body text on dark |
| text-on-navy-soft | #C9D4F0 | Subtitles on dark cards |
| device | #0A0F1E | Phone and laptop bezel (ring #3A4A78) |

**Contrast rule:** on white, use navy-900 for text. Use gold-700, not gold-500, for small gold text. Gold-500 on white is for fills and large elements only.

**Backdrop gradient (hero ads and landing sections):**

```css
background: radial-gradient(ellipse at 50% 62%, #1B3A78 0%, #0E2250 45%, #071433 100%);
```

**Card gradient (room card):** `linear-gradient(135deg, #0E2250, #1B3A78)`

## 3. Typography

- **Headings and brand:** Montserrat, weights 700 and 800.
- **Body and UI:** Inter, weights 400 to 700.
- Always provide fallbacks: `'Montserrat', 'Inter', system-ui, sans-serif`.

| Role | Font | Size | Weight | Notes |
| --- | --- | --- | --- | --- |
| Wordmark | Montserrat | 28px | 800 | letter-spacing 6px, uppercase |
| Eyebrow | Montserrat | 22px | 700 | letter-spacing 8px, gold, uppercase |
| Headline | Montserrat | 68–96px | 800 | line-height 1.05, letter-spacing -1px, uppercase, key phrase in gold |
| Supporting text | Inter | 30–34px | 500 | line-height 1.35, color text-on-navy |
| Slogan footer | Montserrat | 22px | 700 | letter-spacing 1px, gold |
| App title | Inter | 22–26px | 700 | navy-900 |
| App body | Inter | 14–17px | 400–600 | text-muted for secondary |
| Micro label | Inter | 12–13px | 700 | letter-spacing 1–2px, uppercase |

**Headline pattern:** two lines. Line one in white, line two (the payoff) in gold.

## 4. Layout and Spacing

- **Artboard:** 1080 × 1350 (portrait). Story: 1080 × 1920. Landscape: 1920 × 1080.
- **Left margin:** 84px. Logo lockup at top 72px, eyebrow and headline begin at about 170px.
- **Vertical order:** logo, eyebrow, headline, supporting text, device mockup, slogan footer.
- **Device mockups** sit in the lower half and may bleed off the bottom edge.
- **Spacing scale:** 4, 8, 12, 14, 16, 18, 24, 26. Use 12–16px between stacked cards.
- **Radii:** inputs 12px, buttons 12–14px, cards 14–20px, pills 20px, phone 62–68px outer and 49–54px screen.
- **Shadows:** `0 40px 90px rgba(0,0,0,.55)` for devices, plus a soft gold glow `0 0 120px rgba(212,175,55,.2)` on the hero device.

## 5. Components

### Logo lockup

A 44px rounded square (radius 12px, 2px gold border) containing a 16px gold dot, followed by the CELLS wordmark.

### Role badge

Pill at the top right of every app screen. Navy background, gold text, 12px / 600, uppercase. Values: ADMINISTRATOR, PASTOR, LEADER, MEMBER. Each screen must show only the role's own features.

### Phone mockup

- Width 400–460px, bezel padding 13–14px, background device color.
- Screen: app-bg, radius 49–54px, overflow hidden.
- Notch: 96–104px wide, 26–28px tall, centered, 12px from top.
- App header row: CELLS wordmark (Montserrat 800, 20px, letter-spacing 4px) on the left, role badge on the right.
- Bottom tab bar (when shown): 78px, white, top border, 12px / 600 labels, active tab in navy.

### Laptop dashboard

Bezel padding 14px with top radius 22px, a sidebar of 170–190px in navy-900 (active item in gold on a translucent gold fill), and a metallic base `linear-gradient(#C9D1E6, #8F9BB8)`.

### Buttons

- **Primary:** gold-500 background, navy-900 text, 700, height 44–54px, radius 12–14px.
- **Secondary:** navy-900 background, white text, same size.

### Inputs

White background, 1px border-input, radius 12px, padding 14px 16px. **Focused or active:** 2px gold-500 border. Label above in 13px / 700, uppercase, letter-spacing 1px, text-muted.

### Cards

White, 1px border, radius 16–20px, padding 14–18px. Hero room card uses the navy gradient with a gold micro label (YOUR ROOM).

### Room name format

Always `Church Name – Room Name` (en dash), with the leader shown as the subtitle: `Leader: Name`.

### Scripture panel

gold-50 background, 1px #E8DDB0 border, radius 14–16px, italic Inter 18px for the verse, reference above in 700.

### Progress bar

Height 10px, radius 6px, track #E3E8F4, fill gold-500. Show a label and value above (`Participation today — 8 of 12`). Ring version uses a 14–16px stroke, round caps, rotated -90°.

### Participation number badge

28–34px navy circle with a gold bold number. Pair with the member name and a `No. N` label.

### Status pills

12px / 700, radius 20px. Submitted and Optional use gold-100. Pending uses track color. Text in navy-900.

### Avatars

Circular, 57px radius in a 120 viewBox, navy fill, 3px gold ring, simple bust illustration. Replace with real photography where available.

### Background network

Thin gold lines (opacity .25–.4) joining gold nodes (opacity .5–.7), plus a faint white church silhouette at 5% opacity. Use sparingly on the sides so it never competes with the device.

## 6. Role-Based UI Rules

| Role | Allowed content in mockups |
| --- | --- |
| Administrator | Church accounts, pastor accounts, platform management, full network overview |
| Pastor | Assigned leaders, their rooms, participation progress |
| Cell Group Leader | Create rooms, manage members and participation numbers, devotions (passage and learning level), tracking, optional group summary and personal reflection |
| Member | Read the passage, submit personal understanding |

Do not invent features beyond these. Do not show leader tools on a member screen or admin tools on a pastor screen.

## 7. Content and Copy Rules

- Headlines are uppercase and end with a period.
- Supporting text is one sentence, sentence case.
- Primary slogan: **One Church. Connected Leaders. Growing Communities.**
- Closing line: **Bringing Every Cell Group Together.**
- Use the real church name (Jesus Holiness Alliance Church) in room names once finalized.
- Typographic apostrophes in visible text.

## 8. Accessibility

- Keep body text at 14px or larger in app UI and 30px or larger in ad supporting text.
- Do not rely on color alone. Pair status colors with text (Submitted, Pending).
- Give charts and rings an `aria-label` with the value, for example `8 of 12 members participated`.
- Keep tap targets at 44px or taller.

## 9. CSS Starter Tokens

```css
:root {
  --navy-950: #071433;
  --navy-900: #0E2250;
  --navy-700: #1B3A78;
  --navy-500: #3D5FA5;
  --gold-500: #D4AF37;
  --gold-700: #8A6D12;
  --gold-100: #FBEFC4;
  --gold-50: #FBF7E8;
  --app-bg: #F4F6FB;
  --surface: #FFFFFF;
  --border: #DCE3F2;
  --border-input: #C5CFE6;
  --track: #E3E8F4;
  --text-muted: #47557A;
  --text-on-navy: #E6ECFA;
  --device: #0A0F1E;
  --font-heading: 'Montserrat', 'Inter', system-ui, sans-serif;
  --font-body: 'Inter', system-ui, sans-serif;
  --radius-input: 12px;
  --radius-card: 18px;
  --radius-pill: 20px;
  --shadow-device: 0 40px 90px rgba(0, 0, 0, .55);
}
```

## 10. Do and Don't

**Do**

- Keep one gold accent per area: a headline phrase, a button or a progress fill.
- Keep devices large and centered, with plenty of negative space.
- Reuse the same layout skeleton across the whole campaign.

**Don't**

- Use gold as a large background fill or for small text on white.
- Mix role features on one screen.
- Add decorative elements that compete with the app UI.
- Use more than two typefaces.

## 11. Where to Find the Source

The 13 campaign boards are on the CELLS canvas artifact. Each board is a single HTML file with inline styles, so any value above can be inspected directly. Board titles: Meet CELLS, Administrator Management, Pastor Management, Creating Rooms, Adding Members, Bible Devotion, Member Participation, Participation Tracking, Group Summary and Reflection, Pastor Monitoring, Centralized Administration, Daily Reset, Final Advertisement.


# CELLS – Product Concept and Application Documentation

**Tagline:** Grow Together. Share the Word. Build a Stronger Community.

**Application type:** Web-based Christian community and devotion platform

**Technology stack:** Vue.js 3 (frontend) · Java Spring Boot (backend) · PostgreSQL (database)

**Document status:** Refined draft, updated with the strict sequential participation rules, the EXP rules, and the leader-controlled daily exemption. The product concept is unchanged from the original; this version adds structure, clarifies rules, and makes the document easier to build from.

> **Reading note:** Items marked **\[Clarified\]** restate an original idea more precisely. Items marked **\[Proposed\]** are suggestions that fill a gap without changing the concept. Items marked **\[Confirm\]** are ambiguities that need your decision. All **\[Confirm\]** items are collected in Section 16.

## Table of Contents

1. Project Overview
2. Glossary
3. User Roles and Access Levels
4. Room Structure and Organization
5. Core Feature: Daily Devotion System
6. Experience and Participation Tracking
7. Leader Summary and Personal Reflection
8. Pastor Dashboard
9. Administrator Dashboard
10. Complete Application Workflow
11. Application Pages
12. Database Concept
13. Technology Architecture
14. Business Rules
15. Product Value and Differentiation
16. Open Questions
17. Final Product Vision

## 1. Project Overview

### 1.1 Application Name

**CELLS**

CELLS is a web-based Christian community platform that helps churches organize their cell groups, encourage consistent Bible devotion, and monitor the spiritual participation of their members.

Pastors and cell group leaders create digital rooms where members receive Bible verses or chapters, share their personal understanding, and take part in daily devotion activities.

Unlike a traditional group chat, CELLS uses a structured participation system. Each member has an assigned participation number, which encourages everyone to contribute their understanding of the Word instead of leaving participation to a few members.

The platform also gives pastors an overview of their cell group leaders' participation statistics, and gives administrators a central dashboard for monitoring the whole platform.

### 1.2 Vision

To create a connected Christian community where pastors, cell group leaders, and members grow together through consistent engagement with God's Word.

### 1.3 Mission

To encourage daily Bible engagement, meaningful discussion, active participation, and stronger connections within church communities through an organized digital platform.

### 1.4 Main Concept

The whole application revolves around one continuous cycle:

1. A cell group leader shares a Bible verse or chapter.
2. Members read and reflect on the passage.
3. Members submit their personal understanding or devotion.
4. The participation system tracks who has contributed.
5. Once members have participated, the leader can optionally create a summary or share their own understanding.
6. The cycle repeats with another Bible verse or chapter.

The goal is not simply to read the Bible, but to understand it, reflect on it, and share what was learned.

### 1.5 Guiding Principles

- **Participation over performance.** CELLS rewards consistency, not the quality of anyone's faith.
- **Encouragement over competition.** Spiritual growth must not become a contest.
- **Monitoring without micromanaging.** Pastors and administrators see statistics; they do not manage individual daily devotions.
- **History is permanent.** Daily resets never delete submissions or records.

## 2. Glossary

| Term | Meaning |
| --- | --- |
| Church | An organization registered on the platform. |
| Pastor | A user who supervises cell group leaders. |
| Cell Group Leader (Devotion Leader) | A user who runs one or more cell group rooms. |
| Member | A user who participates in a cell group room. |
| Room | A cell group space owned by a leader, named *Church Name – Room Name*. |
| Devotion | A Bible passage shared by a leader, with a learning level and optional instructions. |
| Submission | A member's written understanding of a devotion. |
| Participation Number | A member's assigned position in the room's participation order. |
| Participation Cycle | One day of participation, which resets at 12:00 AM. |
| Experience (XP) | A points value reflecting participation and consistency. |
| Summary / Reflection | Optional leader content shown after members participate. |

## 3. User Roles and Access Levels

CELLS has four primary roles: **Administrator**, **Pastor**, **Cell Group Leader**, and **Member**.

### 3.1 Administrator

The Administrator manages and monitors the entire platform and has the highest level of access.

**Responsibilities**

- Manage registered pastors.
- Manage church information.
- Monitor pastors' rooms, cell group leaders' rooms, and members' rooms.
- View overall participation statistics.
- Monitor room activity and participation status.
- Manage user accounts and platform access.

### 3.2 Pastor

The Pastor supervises cell group leaders and monitors their cell groups.

**Responsibilities**

- Create a Pastor Room.
- Add or manage cell group leaders.
- Monitor leaders' rooms.
- View each leader's participation statistics.
- Monitor the total number of participating and non-participating members.
- Review group activity and participation history.

**Important:** The Pastor's primary purpose in CELLS is to monitor the participation statistics of cell group leaders and their members. The Pastor should not need to manually manage any member's daily devotion.

### 3.3 Cell Group Leader / Devotion Leader

The Cell Group Leader manages a cell group and facilitates its daily devotion activities.

**Responsibilities**

- Create a cell group room, entering the church name before the room name.
- Add members to the room.
- Assign and manage member participation numbers.
- Share Bible verses or chapters.
- Set the learning level of each passage.
- Monitor member participation and review submitted devotions.
- Grant or revoke a member's daily exemption (Exempt for Today).
- Optionally create a summary of members' understanding.
- Optionally share their personal understanding of the passage.

### 3.4 Member

Members are the primary participants in the devotion process.

**Responsibilities**

- Join their assigned cell group room.
- View the Bible verse or chapter shared by the leader.
- Read and understand the passage.
- Submit their personal understanding.
- Follow the assigned participation order.
- View their participation history.
- Track their personal participation progress and experience.

### 3.5 Role Permission Matrix **\[Proposed\]**

This table summarizes what each role can do, based on the responsibilities above.

| Capability | Admin | Pastor | Leader | Member |
| --- | :-: | :-: | :-: | :-: |
| Manage churches and pastor accounts | ✔ |  |  |  |
| Manage user accounts and platform access | ✔ |  |  |  |
| Add / manage cell group leaders | ✔ | ✔ |  |  |
| View platform-wide statistics | ✔ |  |  |  |
| View statistics of own leaders and rooms | ✔ | ✔ |  |  |
| Create rooms |  |  | ✔ |  |
| Add members and set participation numbers |  |  | ✔ |  |
| Grant or revoke a member's daily exemption |  |  | ✔ |  |
| Create devotions |  |  | ✔ |  |
| Review members' submissions | ✔\* |  | ✔ | Own only |
| Create summary / reflection |  |  | ✔ |  |
| Submit understanding |  |  |  | ✔ |
| View own participation history and XP |  |  |  | ✔ |

\* Administrator access to submission content is subject to **\[Confirm\]** item 3 in Section 16.

## 4. Room Structure and Organization

CELLS uses a hierarchical structure:

**Administrator → Pastor → Cell Group Leaders → Members**

### 4.1 Pastor Room

The Pastor Room is the pastor's supervision space. It contains the cell group leaders assigned to that pastor.

**Example – Pastor Room: Pastor John**

- Leader 1
  - Church Name – Youth Cell Group
  - Church Name – Family Cell Group
- Leader 2
  - Church Name – Young Adults
  - Church Name – Men's Cell Group
- Leader 3
  - Church Name – Women's Cell Group

The Pastor can view the participation statistics of all these leaders and their rooms.

### 4.2 Cell Group Leader Room

Each cell group leader can create multiple rooms for different groups.

**Room naming convention:** *Church Name – Room Name*

Examples:

- Victory Church – Youth Cell Group
- Victory Church – Young Adults
- Victory Church – Men's Cell Group
- Grace Community Church – Family Cell Group

**Room subtitle:** The leader's name is always displayed as the room subtitle.

- Room name: Victory Church – Youth Cell Group
- Subtitle: Led by John Doe

This ensures the church and the responsible leader are always clearly identified.

### 4.3 Member Room

Each member is assigned to a specific cell group room and participates according to that room's participation order.

Inside the room, members can:

- View the current devotion.
- Read the assigned Bible passage.
- Submit their understanding.
- See the participation order.
- Check which members have participated.
- View the leader's summary, if available.
- View previous devotion activities.

## 5. Core Feature: Daily Devotion System

This is the heart of CELLS. Leaders share Bible passages, and members submit their understanding.

### 5.1 Creating a Devotion

The leader creates a devotion by providing:

| Field | Required |
| --- | :-: |
| Bible Book | Yes |
| Chapter | Yes |
| Verse or Verse Range | Yes |
| Learning Level | Yes |
| Devotion Title | Optional |
| Additional Instructions | Optional |

**Example**

- **Devotion Title:** God's Love
- **Bible Passage:** John 3:16
- **Learning Level:** Beginner / Newbie
- **Instructions:** Read the passage and share what you understand about God's love.

The verse field also supports a whole chapter, since leaders may share "a Bible verse or chapter" **\[Clarified\]**.

### 5.2 Learning Levels

The leader categorizes each passage by the group's learning level:

1. Beginner / Newbie
2. Intermediate
3. Advanced

These levels help leaders choose passages that fit their group's learning journey. The leader remains responsible for choosing the passage and its level.

### 5.3 Member Devotion Submission

Every member can submit their personal understanding of the assigned passage. A submission may include:

- Personal understanding of the verse.
- What the passage teaches them.
- Lessons they learned.
- How they can apply the teaching in daily life.

**Example**

- **Bible Passage:** John 3:16
- **Member:** John
- **Understanding:** "This verse reminds me that God's love is unconditional. He gave His only Son so that we could receive eternal life. It teaches me to appreciate God's love and share that same love with others."

Each submission is saved and linked to its specific member, room, and devotion.

### 5.4 Participation Order System

Every member in a room receives an assigned participation number.

| Number | Member |
| :-: | --- |
| 1 | John |
| 2 | Esseck |
| 3 | Jabez |
| 4 | Mark |
| 5 | Peter |

The participation numbers determine the order in which members can submit their devotion.

#### Sequential Participation Rules

The participation system follows a strict sequential order. Every member after Member #1 must wait until the immediately preceding member has successfully submitted before becoming eligible. This replaces the earlier draft reading, in which Member #2 could submit without Member #1.

**Scenario 1 – All members participate**

1. John (#1) submits his understanding.
2. Esseck (#2) becomes eligible and submits his understanding.
3. Jabez (#3) becomes eligible and submits his understanding.
4. Mark (#4) becomes eligible and submits his understanding.
5. Peter (#5) becomes eligible and submits his understanding.

All members participate successfully and receive their experience points.

**Scenario 2 – Member #1 does not participate**

1. John (#1) does not submit his understanding.
2. Esseck (#2) cannot participate because John has not participated.
3. Jabez (#3) cannot participate because Esseck has not participated.
4. Mark (#4) cannot participate because Jabez has not participated.
5. Peter (#5) cannot participate because Mark has not participated.

The sequence stays blocked until John successfully participates.

**Scenario 3 – Members #1 and #2 do not participate**

1. John (#1) does not submit his understanding.
2. Esseck (#2) cannot participate because John has not participated.
3. Jabez (#3) cannot participate because Esseck has not participated.
4. All later members stay ineligible because the members before them have not participated.

#### Participation Logic

| Position | Eligibility |
| --- | --- |
| Member #1 | Can always participate independently. |
| Member #2 | Can participate only after Member #1 has successfully participated. |
| Member #3 | Can participate only after Member #2 has successfully participated. |
| Member #4 | Can participate only after Member #3 has successfully participated. |
| Member #5 | Can participate only after Member #4 has successfully participated. |

**General rule:** Every member after Member #1 waits until the immediately preceding member has successfully submitted. The only exception is a leader-granted daily exemption (Section 5.5).

The system must validate eligibility before accepting a submission. Members who cannot participate because the required preceding member has not participated receive no experience points for that devotion cycle.

#### Daily Participation Reset

The participation cycle resets every day at exactly **12:00 AM (midnight)**. When the new day begins:

- A new daily participation cycle starts, and daily participation statuses reset.
- The participation order becomes active again.
- Members can participate in the new devotion cycle.
- The assigned participation numbers remain unchanged.
- Daily exemptions expire (Section 5.5).
- Previous devotion submissions stay saved in the historical records.
- Historical experience records remain intact.
- A new opportunity to earn experience becomes available.

The reset only affects the current daily participation cycle. It must never delete historical submissions or experience records.

**Implementation note \[Proposed\]:** Each participation record carries a participation date in the configured timezone. "Today's status" is computed from the current date rather than by erasing data, so a missed or late scheduler run can never lose history. A scheduled backend job at midnight can still create the new day's cycle and notify clients.

### 5.5 Daily Member Exemption (Leader-Controlled)

The Cell Group Leader can temporarily exempt a member from the daily participation requirement, so a member who cannot participate neither blocks the group nor counts as a missed participant.

Typical reasons include:

- Going on vacation.
- Having no internet or Wi-Fi access.
- Being temporarily unavailable.
- Other valid personal circumstances.

The leader sees an **Exempt for Today** toggle beside each member's name.

#### How It Works

When the leader turns the toggle on for a member:

1. The member is exempt from the current day's participation requirement.
2. The member is not marked as a missed participant for that day.
3. The system skips the exempted member in the participation sequence. The next member becomes eligible once their preceding non-exempt member has successfully participated, or immediately if no non-exempt member comes before them.
4. The exempted member receives no experience points for that day, because they did not submit.
5. The exemption applies only to the current daily cycle.
6. The exemption expires automatically at 12:00 AM when the next cycle begins.

#### Example

John (#1) is on vacation without internet access, and the leader turns on his toggle.

| Number | Member | Status |
| :-: | --- | --- |
| 1 | John | Exempted |
| 2 | Esseck | Eligible |
| 3 | Jabez | Waiting |
| 4 | Mark | Waiting |

Esseck (#2) becomes eligible even though John did not submit. After Esseck submits, Jabez (#3) becomes eligible. After Jabez submits, Mark (#4) becomes eligible. John stays exempt for that day and earns no experience points.

#### Leader Interface

Each member row shows an exemption control beside the name. The leader can turn it on or off when necessary.

| Member | Participation | Exemption |
| --- | --- | :-: |
| John | Exempted | ON |
| Esseck | Eligible | OFF |
| Jabez | Waiting | OFF |
| Mark | Waiting | OFF |

#### Exemption Rules

- Only the Cell Group Leader can grant or revoke a member's daily exemption.
- Exemption is toggled for one member at a time (see Section 16, item 2).
- Exemption is valid only for the current daily cycle.
- Exemption does not award experience points.
- Exemption does not delete or modify previous participation records.
- Exemption resets automatically at midnight.
- An exempted member is skipped when the participation sequence is validated and must not block later members.
- The system records which leader granted the exemption and when.

#### Eligibility Check With Exemptions **\[Proposed\]**

The backend decides whether a member may submit with this rule, evaluated at submission time in the room's configured timezone:

1. Find the nearest member before them (by participation number) who is not exempt today.
2. If there is none, the member is eligible.
3. Otherwise, the member is eligible only if that member has submitted for today's devotion.

An exempt member neither waits on anyone nor blocks anyone. Each exemption is stored with its room, member, date, leader, and timestamp, so history is preserved.

#### Core Principle

The participation system encourages members to engage with God's Word consistently while recognizing that there are legitimate circumstances when someone cannot participate. CELLS promotes consistency, accountability, and spiritual growth without unfairly penalizing members who have been granted a valid exemption.

## 6. Experience and Participation Tracking

CELLS has an experience system that encourages consistent participation. Members gain experience by successfully submitting their understanding of the assigned Bible passage.

Experience points (EXP) are recorded automatically when a member successfully submits their understanding. Members who do not successfully participate receive no EXP for that day's devotion. This covers members blocked by the participation sequence and members with a daily exemption (Section 5.5). An exemption is neutral: it earns no EXP, and it is not counted as a missed participation.

**Statistics available**

- Total devotion submissions.
- Daily, weekly, and monthly participation.
- Participation history.
- Current participation progress.
- Total experience earned.

**Principle:** Experience reflects participation and consistency, not the quality of someone's faith or spiritual worth. The application should encourage members to participate rather than turn spiritual growth into a competition.

**Proposed design choices \[Proposed\] \[Confirm\]**

- Experience is awarded only for a verified, eligible submission (Business Rule 14).
- A fixed amount per submission, with no scoring of content quality.
- No public leaderboard comparing members. Members see their own progress; leaders and pastors see aggregate participation.

## 7. Leader Summary and Personal Reflection

After members have participated, the leader can provide additional guidance. This feature is optional, and the two options can be used independently.

### Option A – Group Summary

The leader reviews the members' submissions and writes a summary of the group's insights.

**Example – Leader's Summary, John 3:16**

"Based on everyone's understanding, we can see that the main message of this passage is God's unconditional love. We learned that salvation is a gift from God and that we should respond to His love through faith and by showing love to others."

### Option B – Personal Understanding

The leader shares their own interpretation or reflection on the passage.

**Example – Leader's Reflection**

"John 3:16 reminds us that God's love is not something we earn. It is freely given. As Christians, we should remember this love and allow it to influence how we treat other people."

## 8. Pastor Dashboard

The Pastor Dashboard gives an overview of participation statistics for the cell group leaders under the pastor's supervision. The pastor can see each leader's rooms without entering every room individually.

### 8.1 Pastor Overview

The dashboard displays:

- Total number of cell group leaders.
- Total number of rooms.
- Total number of members.
- Total participating members.
- Total non-participating members.
- Overall participation percentage.

### 8.2 Leader Participation Statistics

**Example – Pastor John's Dashboard**

| Cell Group Leader | Room | Participation |
| --- | --- | :-: |
| Leader 1 | Youth Cell Group | 5/6 |
| Leader 2 | Young Adults | 10/10 |
| Leader 3 | Men's Cell Group | 8/12 |

The pastor can immediately see which groups have completed their participation and which still have pending members.

### 8.3 Room-Level Statistics

When the pastor opens a leader's room statistics, the system shows:

- Room name, church name, and leader name.
- Total, participating, and non-participating members.
- Participation percentage.
- Current devotion.
- Daily participation status.

**Example – Victory Church – Youth Cell Group**

- Led by: Leader 1
- Total members: 6
- Participated: 5
- Not yet participated: 1
- Participation rate: 83.33%

The pastor monitors these statistics without directly managing members' devotion submissions.

## 9. Administrator Dashboard

The Administrator Dashboard is a centralized overview of the entire platform. The administrator can monitor three areas: Pastor Rooms, Cell Group Leader Rooms, and Member Rooms.

### 9.1 Overall Platform Statistics

- Total registered churches.
- Total pastors.
- Total cell group leaders.
- Total rooms.
- Total members.
- Total active devotion activities.
- Total participating members.
- Total pending participation.
- Overall participation percentage.

### 9.2 Pastor Room Monitoring

The administrator can view all registered pastors and their associated leaders.

**Example – Pastor John**

- 5 cell group leaders
- 12 rooms
- 85 total members
- 78 participating members

### 9.3 Cell Group Leader Room Monitoring

The administrator can view all rooms created by leaders. Each room shows its church name, room name, leader name, member count, and participation statistics.

### 9.4 Member Room Monitoring

The administrator can monitor individual members' room assignments and participation records, including:

- Member information.
- Assigned cell group.
- Participation status.
- Devotion submission history.
- Total experience.

The administrator dashboard is primarily for monitoring and platform management.

## 10. Complete Application Workflow

| Step | Actor | Action |
| :-: | --- | --- |
| 1 | Administrator | Creates or manages the platform's church and pastor accounts. |
| 2 | Pastor | Accesses the dashboard and manages assigned cell group leaders. |
| 3 | Leader | Creates a room named *Church Name – Room Name*; the leader's name appears as the subtitle. |
| 4 | Leader | Invites or assigns members to the room; each member receives a participation number. |
| 5 | Leader | Creates a devotion, selecting a Bible passage and learning level. |
| 6 | Leader | Optionally turns on Exempt for Today for any member who cannot participate that day. |
| 7 | Members | Read the passage and submit their understanding in sequential order; exempted members are skipped. |
| 8 | System | Records each member's participation and updates room statistics. |
| 9 | Leader | Optionally creates a group summary or shares a personal reflection. |
| 10 | Pastor | Checks participation statistics of leaders and rooms. |
| 11 | Administrator | Monitors all pastors, leaders, rooms, and members. |
| 12 | System | At 12:00 AM, the daily cycle and all daily exemptions reset, and the process begins again. |

## 11. Application Pages

### Public Pages

- Landing Page
- Login Page
- Registration / Invitation Page

### Administrator Pages

- Admin Dashboard
- Church Management
- Pastor Management
- Cell Group Leader Management
- Room Monitoring
- Member Management
- Participation Analytics

### Pastor Pages

- Pastor Dashboard
- My Cell Group Leaders
- Leader Statistics
- Room Statistics
- Participation Reports

### Cell Group Leader Pages

- Leader Dashboard
- My Rooms
- Create Room
- Manage Members
- Participation Order Management
- Daily Exemption Controls
- Create Devotion
- Devotion History
- Member Submissions
- Group Summary / Personal Reflection

### Member Pages

- Member Dashboard
- My Cell Group
- Current Devotion
- Bible Passage
- Submit Understanding
- Participation Order
- Participation History
- Experience Statistics

## 12. Database Concept

CELLS uses **PostgreSQL** as its primary database.

### 12.1 Core Entities

1. users
2. churches
3. pastors
4. cell\_group\_leaders
5. pastor\_leaders
6. rooms
7. room\_members
8. devotions
9. devotion\_submissions
10. devotion\_summaries
11. participation\_records
12. daily\_exemptions
13. experience\_records
14. invitations
15. audit\_logs

### 12.2 Key Relationships

- A church can have multiple pastors and cell group leaders, depending on its organizational structure.
- A pastor supervises multiple cell group leaders.
- A cell group leader creates multiple rooms.
- A room contains multiple members.
- A room has multiple devotion activities.
- A devotion has multiple member submissions.
- Each member has multiple participation records.
- Each completed participation can contribute to the member's experience records.

The database must preserve historical devotion submissions and daily participation records.

### 12.3 Suggested Key Fields **\[Proposed\]**

| Entity | Key fields |
| --- | --- |
| users | id, email, password\_hash, full\_name, role, status, created\_at |
| churches | id, name, status |
| rooms | id, church\_id, leader\_id, name, created\_at |
| room\_members | room\_id, member\_id, participation\_number (unique per room) |
| devotions | id, room\_id, bible\_book, chapter, verse\_range, title, learning\_level, instructions, devotion\_date |
| devotion\_submissions | id, devotion\_id, room\_id, member\_id, content, submitted\_at |
| devotion\_summaries | id, devotion\_id, type (SUMMARY or REFLECTION), content |
| participation\_records | id, room\_id, member\_id, devotion\_id, participation\_date, status |
| daily\_exemptions | id, room\_id, member\_id, exemption\_date, granted\_by, granted\_at, revoked\_at |
| experience\_records | id, member\_id, submission\_id, points, awarded\_at |
| audit\_logs | id, actor\_id, action, target, created\_at |

### 12.4 Data Integrity Constraints **\[Proposed\]**

- Unique participation number per room.
- One submission per member per devotion.
- One exemption per member, per room, per day.
- Use of `@Version` optimistic locking on submissions and room membership changes to guard against concurrent edits.
- Schema changes managed through migrations rather than automatic schema updates.

## 13. Technology Architecture

**Frontend (Vue.js 3) → REST API → Java Spring Boot backend → PostgreSQL database**

### 13.1 Frontend: Vue.js 3

The frontend is built with Vue.js 3 (not React).

**Proposed frontend stack \[Proposed\]**

- Vue 3 with the Composition API and `<script setup>`
- Vite as the build tool
- Vue Router with role-based route guards and lazy-loaded routes
- Pinia for state management (for example: auth, room, devotion, and UI stores)
- Tailwind CSS for styling, mobile-first
- Composables (`useXxx`) for reusable logic such as participation status and polling
- Vitest and Vue Test Utils for testing

**Responsibilities**

- User interface and role-based dashboards.
- Room management.
- Devotion creation.
- Submission forms.
- Participation status display.
- Statistics and charts.

**Frontend guidance**

- Route guards improve usability only; the backend always enforces permissions.
- Every screen handles loading, empty, and error states.
- Accessibility basics: labelled inputs, keyboard navigation, visible focus, semantic HTML.
- Mobile-first layouts, since most members will likely use phones.

### 13.2 Backend: Java Spring Boot

**Assumed versions \[Proposed\]:** Java 17 or 21, Spring Boot 3.x, Maven or Gradle.

**Responsibilities**

- Authentication and authorization.
- User and role management.
- Room creation and management.
- Devotion management.
- Participation order validation.
- Daily participation reset.
- Experience calculations.
- Pastor and administrator statistics.
- REST API development.

**Backend guidance \[Proposed\]**

- Layered structure: controller → service → repository. Business rules live in services.
- Spring Data JPA with lazy associations and explicit fetching to avoid N+1 queries on dashboards.
- DTOs for all API responses; never expose JPA entities.
- Bean Validation on request payloads.
- Central error handling returning consistent error bodies and correct status codes.
- Spring Security with default-deny, `@PreAuthorize` role checks, and ownership checks (for example, a leader can only manage their own rooms; a pastor can only view their own leaders).
- JWT authentication with short-lived access tokens and revocable refresh tokens; passwords hashed with BCrypt or Argon2.
- Flyway or Liquibase for database migrations.
- Rate limiting and lockout on login endpoints.
- Tests: JUnit 5 and Mockito for services, `@WebMvcTest` for controllers, and Testcontainers with PostgreSQL for repositories.

### 13.3 Database: PostgreSQL

Responsibilities:

- Store user accounts, church information, and room information.
- Store devotion activities and member submissions.
- Store participation history and experience records.
- Store dashboard statistics or supporting data.

### 13.4 Critical Backend Rules

- **The backend must validate participation eligibility, including exemptions.** Participation restrictions must never rely solely on frontend controls.
- **The eligibility check and the submission insert happen in one transaction**, backed by a unique constraint, so two simultaneous requests cannot both succeed **\[Proposed\]**.
- **A scheduled backend process handles the daily midnight reset** using the configured timezone. The timezone is a setting, not hard-coded (see Section 16).
- **Only the room's own leader may grant or revoke an exemption.** The backend enforces this with an ownership check and writes every change to the audit log **\[Proposed\]**.

### 13.5 API Outline **\[Proposed\]**

Versioned under `/api/v1`, with plural resource names.

| Area | Example endpoints |
| --- | --- |
| Auth | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Invitations | `POST /invitations`, `POST /invitations/{token}/accept` |
| Rooms | `GET/POST /rooms`, `GET /rooms/{id}`, `POST /rooms/{id}/members`, `PUT /rooms/{id}/participation-order` |
| Devotions | `POST /rooms/{id}/devotions`, `GET /rooms/{id}/devotions`, `GET /devotions/{id}` |
| Submissions | `POST /devotions/{id}/submissions`, `GET /devotions/{id}/submissions`, `GET /devotions/{id}/eligibility` |
| Exemptions | `PUT /rooms/{id}/members/{memberId}/exemption`, `DELETE /rooms/{id}/members/{memberId}/exemption` |
| Summaries | `POST /devotions/{id}/summary` |
| Pastor stats | `GET /pastor/dashboard`, `GET /pastor/leaders/{id}/stats` |
| Admin | `GET /admin/dashboard`, `GET /admin/churches`, `GET /admin/pastors` |

Collection endpoints are paginated with a capped page size.

## 14. Business Rules

1. Every room must be associated with a church.
2. Every cell group room displays its church name before its room name.
3. Every leader room displays the responsible leader's name as its subtitle.
4. Every member must belong to an assigned room.
5. Every room must have an assigned participation order.
6. Members must follow the defined participation eligibility rules.
7. Members who are not yet eligible cannot submit their devotion.
8. Every devotion submission is associated with a specific member, room, and devotion.
9. The daily participation cycle resets at 12:00 AM.
10. Resetting participation must not delete historical records.
11. Leaders can optionally create a group summary or personal reflection.
12. Pastors primarily monitor their leaders' participation statistics.
13. Administrators can monitor the overall platform.
14. Experience is awarded only for verified participation.
15. Historical statistics remain available after the daily reset.
16. Participation follows a strict sequential order: every member after Member #1 waits for the immediately preceding member to submit.
17. Only the Cell Group Leader can grant or revoke a member's daily exemption.
18. An exempted member is skipped in the sequence, does not block later members, and is not marked as missed.
19. An exemption applies only to the current daily cycle, expires at 12:00 AM, and awards no experience points.
20. The system records which leader granted each exemption and when.

## 15. Product Value and Differentiation

CELLS addresses a common challenge in cell group devotions: not everyone actively participates. In a traditional group chat, members can read messages without ever sharing their understanding. CELLS introduces a structured participation process that encourages every member to contribute.

**Structured Participation.** Members have assigned participation numbers and a defined process.

**Daily Bible Engagement.** Leaders consistently provide new passages for members to study.

**Collective Learning.** Members learn from one another's understanding and reflections.

**Leader Accountability.** Leaders can monitor the participation of their members.

**Pastor Visibility.** Pastors see how their leaders' rooms are progressing through centralized statistics.

**Centralized Administration.** Administrators monitor the whole church community from one platform.

## 16. Open Questions

These are the points where the original document left room for interpretation. None of them changes the concept; each just needs a decision before development.

1. **Exemption changes during the day.** If a leader turns an exemption off after later members have already submitted, may that member still submit, late and without blocking anyone? Can a member who has already submitted be exempted? Suggested default: no exemption after a member submits; a late removal lets the member submit that day.
2. **Several exemptions at once.** The rule "exemption applies to one member at a time" is read here as "the toggle is set per member", so several members can be exempt on the same day. Confirm this, or set a per-room limit.
3. **Exempt members in statistics.** How do dashboards count exempt members? Suggested default: show them as "Exempted" and leave them out of the participation-rate denominator, so they do not lower the group's rate.
4. **Experience values and levels.** The source calls this an "Experience and Leveling System" but defines no levels. How much EXP does a submission earn, are there member levels, and at what thresholds? Suggested default: a fixed amount per verified submission, and level names kept distinct from passage Learning Levels.
5. **Devotion length.** Is each devotion active for exactly one day (matching the daily reset), or can one devotion span several days? Can a leader post more than one devotion per day?
6. **Visibility of submissions.** Because submissions are personal spiritual reflections, who may read them? Suggested default: the room's members and leader see them; the pastor sees statistics only; the administrator sees participation records, with submission text access limited and audited.
7. **Timezone.** Is the midnight reset based on one platform-wide timezone or on each church's timezone?
8. **Member registration.** Do members join only by leader invitation, or can they also self-register and request to join a room?
9. **Members in multiple rooms.** Can one member belong to more than one room, and if so, is their participation tracked separately per room?
10. **Editing and deleting.** Can a member edit a submission after posting? Can a leader remove one? Are changes recorded in the audit log?
11. **Notifications.** Should members receive reminders (email or in-app) when a devotion is posted or their turn is available? This is not in the original concept and is optional.
12. **Bible text source.** Does the app display the Bible text itself (which requires choosing a translation and checking its license), or only the reference such as "John 3:16"?
13. **Language and localization.** Will the interface be English only, or multilingual?

## 17. Final Product Vision

CELLS is more than a digital room or a group messaging application. It is a structured devotion platform that connects pastors, cell group leaders, and members through daily engagement with the Word of God.

The platform creates an environment where leaders guide, members participate, pastors monitor, and administrators oversee.

Its central idea is simple:

**A leader shares the Word. Members share their understanding. Everyone grows together.**

CELLS aims to make devotion a consistent and participatory activity within every cell group.

# CELLS – Development Roadmap

Nov 16, 2026 · @John Clark

This roadmap takes CELLS from an empty computer to a deployed app in 11 phases, starting with the full development environment (Phase 0) and ending with production hardening (Phase 10). It assumes one developer working part-time, Windows as the main machine, IntelliJ IDEA for the Spring Boot backend, and VS Code for the Vue 3 frontend. Time estimates are approximate.

## Roadmap at a glance

| Phase | Goal | Main result | Approx. time |
| --- | --- | --- | --- |
| 0 | Environment setup | Java, Node, Docker, PostgreSQL and tools installed and verified | 1 day |
| 1 | Project scaffolding | Spring Boot and Vue projects running side by side, connected to PostgreSQL | 2–3 days |
| 2 | Authentication and roles | Login, JWT, four roles, route guards | 1–2 weeks |
| 3 | Organization | Churches, pastors, leaders, invitations, admin pages | 1–2 weeks |
| 4 | Rooms and members | Rooms named *Church – Room*, leader subtitle, participation numbers | 1 week |
| 5 | Devotions | Create devotions with Bible passage and learning level | 1 week |
| 6 | Participation engine | Sequential order, daily exemption, eligibility validation, submissions | 2 weeks |
| 7 | EXP, summary and reflection | EXP records, history, leader summary or reflection | 1 week |
| 8 | Dashboards | Pastor and administrator statistics | 1–2 weeks |
| 9 | Daily cycle and notifications | Midnight cycle job, timezone setting, optional reminders | 3–5 days |
| 10 | Hardening and deployment | Security review, tests, CI, Docker, hosting | 1–2 weeks |

Build order follows the product: foundation first, then the people structure, then the devotion cycle, then the statistics that sit on top of it. Phase 6 is the heart of CELLS, so it gets the most testing.

## Phase 0 – Environment setup

Install everything once, then verify it before writing any code. Versions below were checked on 16 November 2026; always take the current release of the same line from the official site.

### What to install

| Tool | Version | Why you need it |
| --- | --- | --- |
| Git | Latest | Version control; required by most tooling |
| JDK (Temurin) | 21 LTS (25 LTS also works) | Runs and compiles the Spring Boot backend. Spring Boot 4 needs Java 17 at minimum and supports 21 and 25 |
| Maven | Not installed separately | Spring Initializr projects include the Maven Wrapper (`mvnw`), and IntelliJ bundles Maven |
| Node.js | 26 LTS (24 LTS as fallback) | Runs Vite, npm and the Vue toolchain. Node 26 became LTS on 28 October 2026 |
| Vite | Installed per project | Dev server and build tool for Vue; created by the Vue scaffolder, not installed globally |
| Docker Desktop | Latest | Runs PostgreSQL in a container so you do not install it natively |
| PostgreSQL | 18 (in Docker) | CELLS database |
| DBeaver Community | Latest | Browse and query the database (pgAdmin also works) |
| Bruno or Postman | Latest | Test the REST API before the frontend exists |

If you are on macOS or Linux, use Homebrew or SDKMAN for Java and Node (see the end of this phase).

### Install on Windows

Open **PowerShell** and run these one at a time. Close and reopen PowerShell after installing, so the new commands are found.

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Schniz.fnm -e
winget install --id Docker.DockerDesktop -e
winget install --id DBeaver.DBeaver.Community -e
winget install --id Bruno.Bruno -e
```

Then install Node through fnm, which lets you switch Node versions per project:

```powershell
fnm install 26
fnm default 26
```

If `node` is not found afterwards, add fnm to your PowerShell profile as its documentation describes, then reopen the terminal.

Docker Desktop needs virtualization enabled in your BIOS and WSL 2. Docker's installer guides you through WSL 2; restart when it asks.

### Set your identity in Git

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
```

### IDE setup

**IntelliJ IDEA (backend).** Ultimate includes Spring Boot support, the Spring Initializr project wizard and Database Tools. Community does not, so with Community, generate the project at start.spring.io, open the folder in IntelliJ, and use DBeaver for the database. In both editions set the project SDK to the Temurin JDK under *File → Project Structure → SDK*, and turn on *Enable annotation processing* if you use Lombok.

**VS Code (frontend).** Install these extensions:

```powershell
code --install-extension Vue.volar
code --install-extension bradlc.vscode-tailwindcss
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension vitest.explorer
code --install-extension EditorConfig.EditorConfig
code --install-extension ms-azuretools.vscode-docker
```

Use **Vue (Official)** only. If the older Vetur extension is installed, disable it, because the two conflict.

### Verify the installation

```powershell
git --version
java -version
javac -version
echo $env:JAVA_HOME
node -v
npm -v
docker --version
docker run --rm hello-world
```

If `java -version` is correct but `JAVA_HOME` is empty, set it under *System Properties → Environment Variables* to the Temurin folder, for example `C:\Program Files\Eclipse Adoptium\jdk-21...`.

- [ ] `git --version` prints a version
- [ ] `java -version` and `javac -version` both show 21
- [ ] `node -v` shows v26 (or v24)
- [ ] `docker run --rm hello-world` completes
- [ ] VS Code opens and the Vue (Official) extension is active
- [ ] IntelliJ shows the Temurin JDK as its SDK

### On macOS or Linux

Use Homebrew: `brew install git node@26 --cask docker`, and `sdk install java 21-tem` through SDKMAN for Java. The rest of the roadmap is identical.

Official references: [Node.js 26 LTS release](https://nodejs.org/en/blog/release/v26.13.0), [Vite guide](https://vite.dev/guide/), and the Spring Boot, Adoptium and PostgreSQL sites for their current versions.

## Phase 1 – Project scaffolding

One repository with two folders keeps the whole app in one place:

```
cells/
  backend/      Spring Boot (opened in IntelliJ)
  frontend/     Vue 3 + Vite (opened in VS Code)
  docker-compose.yml
  README.md
```

### 1. Create the repository and the database

```powershell
mkdir cells; cd cells
git init
```

Create `docker-compose.yml` in the `cells` folder:

```yaml
services:
  db:
    image: postgres:18
    container_name: cells-db
    environment:
      POSTGRES_DB: cells
      POSTGRES_USER: cells
      POSTGRES_PASSWORD: change_me_local_only
    ports:
      - "5432:5432"
    volumes:
      - cells-db-data:/var/lib/postgresql
volumes:
  cells-db-data:
```

Start it with `docker compose up -d`, then connect DBeaver to `localhost:5432`, database `cells`, user `cells`. The password is for local development only; never reuse it in production.

### 2. Create the backend

Generate the project with Spring Initializr (in IntelliJ Ultimate: *New Project → Spring Initializr*; otherwise start.spring.io):

| Setting | Value |
| --- | --- |
| Project / language | Maven, Java |
| Spring Boot | Latest stable 4.x (not a snapshot or milestone) |
| Group / artifact | `com.cells` / `backend` |
| Java | 21 |

Dependencies to add: Spring Web, Spring Security, Spring Data JPA, Validation, PostgreSQL Driver, Flyway Migration, OAuth2 Resource Server (for JWT), Spring Boot Actuator, Spring Boot DevTools, and Testcontainers. Let Initializr pick the exact starter names, because Spring Boot 4 split its starters into smaller modules.

Put the generated project in `cells/backend`, then configure `src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: local
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

and `application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cells
    username: cells
    password: ${DB_PASSWORD:change_me_local_only}
  flyway:
    enabled: true
```

With `ddl-auto: validate`, Flyway migrations own the schema, and Hibernate only checks it matches your entities. Create `db/migration/V1__init.sql` as an empty placeholder, then run the app. It should start on port 8080 and connect to PostgreSQL. By default Spring Security protects every endpoint, so a first visit to `http://localhost:8080` asks for a login; that is expected until Phase 2.

### 3. Create the frontend

```powershell
npm create vue@latest frontend
```

Answer the prompts: **No** to TypeScript, JSX and experimental features. **Yes** to Vue Router, Pinia, Vitest, ESLint and Prettier. Then:

```powershell
cd frontend
npm install
npm install tailwindcss @tailwindcss/vite
```

Add the Tailwind plugin and an API proxy in `vite.config.js`:

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
```

Replace the contents of `src/assets/main.css` with `@import "tailwindcss";`, then run `npm run dev`. The app opens at `http://localhost:5173`. Because of the proxy, the frontend calls `/api/...` and avoids cross-origin problems during development.

### 4. Commit the starting point

Add a `.gitignore` covering `target/`, `node_modules/`, `.env*` and IDE folders, then make the first commit.

- [ ] Database container is running and DBeaver connects
- [ ] Backend starts on port 8080 with no errors
- [ ] Frontend shows the Vue welcome page on port 5173
- [ ] First commit is made

## Phases 2–10 – Building the product

Each phase ends with working, tested screens, not half-built layers. Backend work happens in IntelliJ, frontend work in VS Code.

### Phase 2 – Authentication and roles

- **Backend:** `users` table and Flyway migration, BCrypt password hashing, login and refresh endpoints, JWT with short-lived access tokens, role checks for Administrator, Pastor, Leader and Member, default-deny security, central error handling.
- **Frontend:** Login page, Pinia auth store, Vue Router guards by role, a layout per role.
- **Done when:** each of the four roles can log in and sees only its own area, and a direct call to a forbidden API returns 403.

### Phase 3 – Organization

- **Backend:** `churches`, `pastors`, `cell_group_leaders`, `pastor_leaders`, `invitations`. Admin manages churches and pastors; pastors add leaders.
- **Frontend:** Church Management, Pastor Management, My Cell Group Leaders, Registration / Invitation page.
- **Done when:** an administrator can create a church and pastor, and a pastor can invite a leader who accepts and logs in.

### Phase 4 – Rooms and members

- **Backend:** `rooms`, `room_members`; room name stored with its church; unique participation number per room; leader ownership checks.
- **Frontend:** My Rooms, Create Room, Manage Members, Participation Order Management. Rooms show *Church Name – Room Name* with *Led by …* as the subtitle.
- **Done when:** a leader creates rooms, adds members and reorders participation numbers, and cannot touch another leader's room.

### Phase 5 – Devotions

- **Backend:** `devotions` with book, chapter, verse range, title, learning level and instructions.
- **Frontend:** Create Devotion, Devotion History, and the member-side Current Devotion and Bible Passage pages.
- **Done when:** a leader posts a devotion and room members see it.

### Phase 6 – Participation engine

This phase implements the rules from Sections 5.4 and 5.5 of the product documentation. Build and test it before any dashboards.

- **Backend:** `devotion_submissions`, `participation_records`, `daily_exemptions`. One service decides eligibility: a member can submit when no non-exempt member is before them, or when the nearest non-exempt member before them has submitted today. The check and the insert run in one transaction, backed by unique constraints.
- **Frontend:** Submit Understanding, Participation Order (status per member: Eligible, Waiting, Submitted, Exempted), and the leader's Exempt for Today toggle beside each name.
- **Tests to write first:** all three scenarios from the documentation, exemption of Member #1, several exempt members in a row, two simultaneous submissions, a late exemption change, and a request after midnight.
- **Done when:** the backend rejects an out-of-turn submission even if the frontend is bypassed, and the leader's toggle immediately unlocks the next member.

### Phase 7 – EXP, summary and reflection

- **Backend:** `experience_records` awarded only for verified, eligible submissions; no EXP for blocked or exempted members; `devotion_summaries` for the leader's group summary or personal reflection.
- **Frontend:** Experience Statistics, Participation History, Group Summary / Personal Reflection.
- **Done when:** a member's history shows submissions and EXP, and history stays intact after the daily cycle changes.

### Phase 8 – Dashboards

- **Backend:** aggregate queries using DTO projections and fetch joins, so dashboards avoid N+1 queries. Pagination with a capped page size.
- **Frontend:** Pastor Dashboard, Leader Statistics, Room Statistics, Admin Dashboard, Room Monitoring, Participation Analytics, with loading, empty and error states.
- **Done when:** pastors see only their own leaders' statistics, administrators see the platform, and numbers match the database.

### Phase 9 – Daily cycle and notifications

- **Backend:** timezone as a configuration setting, today's status computed from the participation date, a scheduled midnight job that opens the new cycle, and exemptions expiring automatically.
- **Optional:** in-app or email reminders when a devotion is posted or a member's turn opens.
- **Done when:** crossing midnight starts a fresh cycle without deleting any history.

### Phase 10 – Hardening and deployment

- **Security:** login rate limiting and lockout, CORS restricted to your real domain, secrets in environment variables, audit log for exemptions and admin actions, review who can read submission text.
- **Quality:** JUnit 5 and Mockito for services, `@WebMvcTest` for controllers, Testcontainers with PostgreSQL for repositories, Vitest for components, a basic CI pipeline.
- **Deploy:** Dockerfiles for both apps, a managed PostgreSQL database, HTTPS, and database backups.
- **Done when:** a clean checkout builds and passes tests in CI, and the app runs on a public URL.

## Decisions to make before coding

These open questions from Section 16 of the product documentation block specific phases, so answer them before you reach those phases.

| Question | Needed before |
| --- | --- |
| Timezone for the midnight reset (platform-wide or per church) | Phase 6 |
| How long a devotion stays active, and how many per day | Phase 5 |
| Exemption edge cases: late changes, several at once, counting in statistics | Phase 6 |
| Who can read submission text (members, leader, administrator) | Phase 6 |
| EXP amount per submission, and whether member levels exist | Phase 7 |
| Invitation-only or self-registration, and one room or many per member | Phase 3 |

## Working agreement

- One short branch per feature, merged to `main` only when its tests pass.
- Write the Flyway migration first, then the entity, service, controller and finally the Vue page.
- Enforce every rule in the backend; the frontend only reflects it.
- Never commit passwords, tokens or `.env` files.
- Update the product documentation whenever a rule changes.

## Handoff rule
- At the end of every piece of work, update `docs/HANDOFF.md` (what changed, branch name, what is open) so another session or account can continue. Keep the Claude Project copy "CELLS - Handoff Status" in sync.
