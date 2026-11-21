/** True when the leader changed the note after first writing it (more than a second apart). */
export function isEdited(note) {
  if (!note) return false
  return Date.parse(note.updatedAt) - Date.parse(note.createdAt) > 1000
}
