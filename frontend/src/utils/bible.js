/** "John 3", "John 3:16" or "John 3:16–18". The server builds the saved reference; this one drives the live preview. */
export function buildReference(bookName, chapter, verseStart, verseEnd) {
  if (!bookName) return ''
  if (!chapter) return bookName
  if (!verseStart) return `${bookName} ${chapter}`
  if (!verseEnd || verseEnd === verseStart) return `${bookName} ${chapter}:${verseStart}`
  return `${bookName} ${chapter}:${verseStart}\u2013${verseEnd}`
}

/** A link to read the passage on a free website. The app shows the reference only, never the text. */
export function readOnlineUrl(reference) {
  const search = reference.replaceAll('\u2013', '-')
  return `https://www.biblegateway.com/passage/?search=${encodeURIComponent(search)}`
}
