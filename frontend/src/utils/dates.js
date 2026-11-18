// Days from the backend are plain "YYYY-MM-DD" strings (no time, no timezone). They are read as
// calendar days, never through new Date(string), which would treat them as UTC midnight.

const dayFormat = new Intl.DateTimeFormat(undefined, {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

function parts(iso) {
  const [year, month, day] = iso.split('-').map(Number)
  return { year, month, day }
}

export function formatDay(iso) {
  if (!iso) return ''
  const { year, month, day } = parts(iso)
  return dayFormat.format(new Date(year, month - 1, day))
}

export function addDays(iso, days) {
  const { year, month, day } = parts(iso)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
}
