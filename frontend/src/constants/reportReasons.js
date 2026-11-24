export const REPORT_REASONS = [
  { value: 'OFFENSIVE', label: 'Offensive or hateful' },
  { value: 'HARASSMENT', label: 'Harassment or bullying' },
  { value: 'INAPPROPRIATE', label: 'Inappropriate or explicit' },
  { value: 'SPAM', label: 'Spam or not related to the passage' },
  { value: 'OTHER', label: 'Something else' },
]

export const reasonLabel = (value) => REPORT_REASONS.find((r) => r.value === value)?.label ?? value
