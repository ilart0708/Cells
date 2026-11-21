/** How far through the current level a member is, for the progress bar. Null at the top level. */
export function levelProgress(summary) {
  if (!summary || !summary.nextLevel) return null
  return {
    value: summary.totalPoints - summary.level.threshold,
    max: summary.nextLevel.threshold - summary.level.threshold,
  }
}
