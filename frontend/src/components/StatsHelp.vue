<script setup>
// audience: 'pastor' (counts only, no member named) or 'admin' (Member Management names members, never answers)
defineProps({ audience: { type: String, default: 'pastor' } })
</script>

<template>
  <details class="rounded-[18px] border border-line bg-white p-4">
    <summary class="flex min-h-11 cursor-pointer items-center font-semibold text-navy-900">
      How these numbers work
    </summary>
    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-muted">
      <li>A member is expected to share on a day when their leader posted a devotion, unless the leader exempted them that day.</li>
      <li>
        Today shows who has shared so far. Someone who hasn’t shared yet is “still to share”, not missing, because the
        day isn’t over.
      </li>
      <li>
        The last 7 and 30 days count completed days only, ending yesterday. A room with no devotion on a day isn’t
        counted for that day.
      </li>
      <li>A member counts from the day after they joined. Members whose accounts are disabled aren’t counted.</li>
      <li v-if="audience === 'admin'">
        These pages show counts only. Member Management names members and shows when they shared, but you never see
        the text of an answer, summary or reflection, and nothing here ranks people or groups.
      </li>
      <li v-else>You see counts only: no member is named, and no answer, note or ranking is shown.</li>
    </ul>
  </details>
</template>
