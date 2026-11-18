const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

export function formatDate(iso) {
  return iso ? dateFormat.format(new Date(iso)) : ''
}

export function memberLabel(count) {
  if (count === 0) return 'No members yet'
  return count === 1 ? '1 member' : `${count} members`
}
