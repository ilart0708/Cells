export const LEARNING_LEVELS = [
  { value: 'BEGINNER', label: 'Beginner / Newbie' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
]

export const LEVEL_LABEL = Object.fromEntries(LEARNING_LEVELS.map((level) => [level.value, level.label]))
