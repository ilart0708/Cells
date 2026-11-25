<script setup>
// Public page: no login needed. The contact details live in constants/contact.js.
// The same address goes into the Play Console "Privacy policy" field: https://<your site>/privacy
import { CONTACT_EMAIL, OPERATOR_NAME } from '../constants/contact'

const APP_NAME = 'CELLS'
const LAST_UPDATED = '25 November 2026'

const sections = [
  {
    title: 'What CELLS is',
    paragraphs: [
      `${APP_NAME} is a Christian devotion app for church cell groups. A cell group leader shares a Bible passage, members write what they understood, and pastors see participation statistics. ${APP_NAME} is meant for adults (18 and over).`,
    ],
  },
  {
    title: 'What we collect',
    list: [
      'Your name and email address, so you can have an account and sign in.',
      'Your password. We store only a scrambled (hashed) version, never the password itself.',
      'Your church, your cell group room and your place in its participation order.',
      'What you write: your understanding of each passage, and the summaries or reflections a leader writes.',
      'Participation records: whether you submitted on a given day, a day off granted by your leader, and the experience points you earned.',
      'Reports: if you report another member’s answer, we store the reason you chose, your optional note, and which answer it was about.',
      'If you turn on notifications: a device token, so your phone can receive reminders.',
      'Basic technical data, such as your network address, used to block repeated failed sign-in attempts.',
    ],
  },
  {
    title: 'What we do not collect',
    paragraphs: [
      `${APP_NAME} has no advertising, no analytics tools and no tracking of your location. We do not sell your data.`,
    ],
  },
  {
    title: 'How we use it',
    list: [
      'To run your account and your cell group room.',
      'To check whose turn it is and to record participation.',
      'To show leaders, pastors and administrators the statistics they need.',
      'To keep the service secure, for example by limiting sign-in attempts.',
    ],
  },
  {
    title: 'Who can see what',
    list: [
      'Your written understanding is shown to your cell group: your leader and the members of your room.',
      'If an answer is reported, only the leader of that room sees the answer, the reason and the note (if one was written). The leader never sees who made the report. The leader can keep the answer or remove it. A removed answer’s text is replaced with “This answer was removed by the leader.” and still counts for participation and experience points.',
      'Pastors see participation statistics for their leaders and rooms, not individual written reflections.',
      'Platform administrators manage accounts and can see participation records.',
      'We do not share your data with advertisers or other companies. We use service providers to run the app (hosting, database and, if turned on, notifications and email). They process data for us and may not use it for their own purposes.',
    ],
  },
  {
    title: 'How long we keep it',
    paragraphs: [
      'We keep your data while your account exists, so your participation history stays available to you.',
      'A leader can archive a room that is no longer in use. An archived room and its history stay stored, but nobody can change it, and members no longer see it on their boards.',
    ],
  },
  {
    title: 'Security',
    paragraphs: [
      'All connections use HTTPS. Passwords are stored hashed. Repeated failed sign-ins are limited. No system is perfectly secure, so please use a password you do not use elsewhere.',
    ],
  },
  {
    title: 'Your choices and deleting your account',
    paragraphs: [
      `You can delete your account yourself at any time: sign in, open Account (under More on a phone), and choose Delete my account. If you cannot sign in, email ${CONTACT_EMAIL} from the address on your account with the subject “Delete my CELLS account”. Deleting removes your name, email, password, what you wrote, your experience points, the reports you made and the reports about your answers, your notifications and your phone registrations. A leader must archive all their rooms before deleting their account. Participation counts in your group's history stay, without your name. Details: /delete-account.`,
      'You can also ask us for a copy of your data, or to correct it, at the same address.',
    ],
  },
  {
    title: 'Children',
    paragraphs: [
      `${APP_NAME} is not meant for people under 18. If you think a child has an account, email us and we will remove it.`,
    ],
  },
  {
    title: 'Changes to this policy',
    paragraphs: [
      'If we change this policy we will update the date at the top of this page.',
    ],
  },
  {
    title: 'Contact',
    paragraphs: [`${OPERATOR_NAME} · ${CONTACT_EMAIL}`],
  },
]
</script>

<template>
  <main class="bg-brand-backdrop min-h-screen px-4 py-10">
    <div class="mx-auto w-full max-w-2xl">
      <p class="text-center font-heading text-[28px] font-extrabold uppercase tracking-[6px] text-white">
        {{ APP_NAME }}
      </p>

      <article
        class="mt-8 rounded-[18px] border border-line bg-white p-6 text-navy-900 shadow-[0_40px_90px_rgba(0,0,0,.55)] sm:p-8"
      >
        <h1 class="font-heading text-2xl font-extrabold">Privacy Policy</h1>
        <p class="mt-1 text-sm text-muted">Last updated: {{ LAST_UPDATED }}</p>

        <section v-for="section in sections" :key="section.title" class="mt-7">
          <h2 class="font-heading text-lg font-bold">{{ section.title }}</h2>
          <p
            v-for="text in section.paragraphs"
            :key="text"
            class="mt-2 text-base leading-relaxed text-muted"
          >
            {{ text }}
          </p>
          <ul v-if="section.list" class="mt-2 list-disc space-y-1.5 pl-5 text-base leading-relaxed text-muted">
            <li v-for="item in section.list" :key="item">{{ item }}</li>
          </ul>
        </section>

        <p class="mt-8">
          <RouterLink
            :to="{ name: 'login' }"
            class="inline-flex min-h-11 items-center font-bold text-navy-900 underline decoration-gold-500 decoration-2 underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-900"
          >
            Back to sign in
          </RouterLink>
        </p>
      </article>
    </div>
  </main>
</template>
