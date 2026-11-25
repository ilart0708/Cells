<script setup>
import { RouterLink } from 'vue-router'
import { CONTACT_EMAIL } from '../constants/contact'
</script>

<template>
  <main class="bg-brand-backdrop min-h-screen px-4 py-10">
    <div class="mx-auto w-full max-w-2xl">
      <p class="text-center font-heading text-[28px] font-extrabold uppercase tracking-[6px] text-white">CELLS</p>

      <article
        class="mt-8 rounded-[18px] border border-line bg-white p-6 text-navy-900 shadow-[0_40px_90px_rgba(0,0,0,.55)] sm:p-8"
      >
        <h1 class="font-heading text-2xl font-extrabold">Delete your CELLS account</h1>

        <section class="mt-6">
          <h2 class="font-heading text-lg font-bold">In the app or on the website</h2>
          <ol class="mt-2 list-decimal space-y-1 pl-5 text-base text-muted">
            <li>Sign in to CELLS.</li>
            <li>Open Account (under More on a phone).</li>
            <li>Choose Delete my account and enter your password.</li>
          </ol>
        </section>

        <section class="mt-6">
          <h2 class="font-heading text-lg font-bold">If you cannot sign in</h2>
          <p class="mt-2 text-base text-muted">
            Email {{ CONTACT_EMAIL }} from the address on your account with the subject “Delete my CELLS account”.
          </p>
        </section>

        <section class="mt-6">
          <h2 class="font-heading text-lg font-bold">What is deleted</h2>
          <ul class="mt-2 list-disc space-y-1 pl-5 text-base text-muted">
            <li>Your name, email address and password.</li>
            <li>Your written answers and your experience points.</li>
            <li>The reports you made, and the reports about your answers.</li>
            <li>Group summaries and reflections you wrote as a leader.</li>
            <li>Your notifications, phone registrations and sign-in sessions.</li>
          </ul>
          <p class="mt-3 text-base text-muted">
            Your group’s participation history stays, shown as “Deleted user”, so the group’s records stay accurate.
            Leaders must archive their rooms first, and pastors must have no leaders assigned.
          </p>
        </section>

        <p class="mt-8 text-sm">
          <RouterLink :to="{ name: 'privacy' }" class="font-semibold underline underline-offset-4">Privacy Policy</RouterLink>
        </p>
      </article>
    </div>
  </main>
</template>
