// Marked passages of an answer: [{ start, end }] are character positions in the answer text.
export const MAX_HIGHLIGHTS = 5

// Splits the text into pieces: [{ text, marked }]. Ranges outside the text are ignored.
export function segments(content, highlights) {
  const text = content ?? ''
  const ranges = [...(highlights ?? [])]
    .filter((r) => r.start >= 0 && r.end > r.start && r.end <= text.length)
    .sort((a, b) => a.start - b.start)
  const pieces = []
  let position = 0
  for (const range of ranges) {
    if (range.start < position) continue
    if (range.start > position) pieces.push({ text: text.slice(position, range.start), marked: false })
    pieces.push({ text: text.slice(range.start, range.end), marked: true })
    position = range.end
  }
  if (position < text.length) pieces.push({ text: text.slice(position), marked: false })
  return pieces
}

// Adds a range from a selection. Surrounding spaces are left out. Returns { ranges } or { error }.
export function addHighlight(content, ranges, selStart, selEnd) {
  let start = selStart
  let end = selEnd
  while (start < end && /\s/.test(content[start])) start++
  while (end > start && /\s/.test(content[end - 1])) end--
  if (end <= start) return { error: 'Select some of your text first, then tap Highlight.' }
  if (ranges.length >= MAX_HIGHLIGHTS) return { error: `You can highlight at most ${MAX_HIGHLIGHTS} passages.` }
  if (ranges.some((r) => start < r.end && end > r.start)) {
    return { error: 'That part is already highlighted. Remove it first to change it.' }
  }
  return { ranges: [...ranges, { start, end }].sort((a, b) => a.start - b.start) }
}

// The answer is sent trimmed, so the positions move left by the spaces trimmed off the front.
export function shiftForTrim(draft, ranges) {
  const trimmed = draft.trim()
  const lead = draft.length - draft.trimStart().length
  return ranges
    .map((r) => ({ start: Math.max(0, r.start - lead), end: Math.min(trimmed.length, r.end - lead) }))
    .filter((r) => r.end > r.start)
}
