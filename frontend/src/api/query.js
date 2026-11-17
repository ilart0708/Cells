/** Builds "path?a=1&b=2", skipping empty values. Page 0 is kept (it is not empty). */
export function withQuery(path, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value).trim())
    }
  }
  const text = search.toString()
  return text ? `${path}?${text}` : path
}
