<script setup>
import { RouterLink } from 'vue-router'
import { rateText } from '../utils/stats'
import StatusPill from './StatusPill.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  period: { type: String, required: true }, // 'last7' or 'last30'
  leaders: { type: Array, required: true }, // the backend's LeaderStatsRow list
  totals: { type: Object, required: true }, // the backend's DashboardTotalsResponse
})
</script>

<template>
  <section class="overflow-hidden rounded-[18px] border border-line bg-white">
    <div class="border-b border-line p-4">
      <h2 class="text-xs font-bold uppercase tracking-[2px] text-muted">{{ title }}</h2>
      <p v-if="subtitle" class="mt-1 text-sm text-muted">{{ subtitle }}</p>
    </div>

    <div class="overflow-x-auto">
      <table class="min-w-full text-left text-sm">
        <caption class="sr-only">{{ title }}, by leader</caption>
        <thead class="border-b border-line text-xs font-bold uppercase tracking-[1px] text-muted">
          <tr>
            <th scope="col" class="px-4 py-3">Leader</th>
            <th scope="col" class="px-4 py-3">Devotions posted</th>
            <th scope="col" class="px-4 py-3">Shared / expected</th>
            <th scope="col" class="px-4 py-3">Participation</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-line">
          <tr v-for="leader in leaders" :key="leader.leaderId">
            <th scope="row" class="px-4 py-1 font-semibold text-navy-900">
              <RouterLink
                :to="{ name: 'pastor-leader-stats', params: { leaderId: leader.leaderId } }"
                class="inline-flex min-h-11 items-center underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500"
              >
                {{ leader.fullName }}
              </RouterLink>
              <StatusPill v-if="leader.accountStatus === 'DISABLED'" status="DISABLED" class="ml-2" />
            </th>
            <td class="whitespace-nowrap px-4 py-3 text-muted">
              {{ leader[period].devotionDays }} of {{ leader[period].possibleDays }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-navy-900">
              {{ leader[period].shared }} / {{ leader[period].expected }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 font-semibold text-navy-900">
              {{ rateText(leader[period].ratePercent) }}
            </td>
          </tr>
        </tbody>
        <tfoot class="border-t border-line bg-app-bg">
          <tr>
            <th scope="row" class="px-4 py-3 font-bold text-navy-900">All my leaders</th>
            <td class="whitespace-nowrap px-4 py-3 text-muted">
              {{ totals[period].devotionDays }} of {{ totals[period].possibleDays }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-navy-900">
              {{ totals[period].shared }} / {{ totals[period].expected }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 font-bold text-navy-900">
              {{ rateText(totals[period].ratePercent) }}
            </td>
          </tr>
        </tfoot>
      </table>
    </div>

    <p class="border-t border-line p-4 text-sm text-muted">
      Devotions posted counts room-days: one room on one day is one room-day, so a leader with two rooms has two
      possible days for each calendar day.
    </p>
  </section>
</template>
