const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

export function formatDate(iso) {
  return iso ? dateFormat.format(new Date(iso)) : ''
}
