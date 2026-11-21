export function plural(count, one, many = `${one}s`) {
  return `${count} ${count === 1 ? one : many}`
}

/** "38%", or a dash when nothing was expected. */
export function rateText(percent) {
  return percent == null ? '–' : `${percent}%`
}

/** "5/6" for today, or null when no devotion was posted (so there is nothing to count). */
export function todayFraction(today) {
  if (!today || today.roomsWithDevotion === 0) return null
  return `${today.shared}/${today.expected}`
}
