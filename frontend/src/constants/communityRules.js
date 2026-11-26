// Shown on the "Create your group" page. A leader must agree before the account is created.
export const COMMUNITY_RULES = [
  'I will use CELLS only for Bible devotion with my own group, and I will keep it respectful.',
  'I will not allow hateful, harassing, sexual or abusive content, and I will review reported answers and remove what breaks these rules.',
  'I will only invite people I know, and I will keep their answers private to the group.',
  'I understand that my church is shown as “not verified” until an administrator verifies it, and that the administrator can disable my account if these rules are broken.',
]

// Shown on the accept-invitation page, by the role being joined.
export const MEMBER_RULES = [
  'I will use CELLS only for Bible devotion with my group, and I will keep it respectful.',
  'I will not write hateful, harassing, sexual or abusive content, and I will not share what others write outside the group.',
  'I understand that my leader can remove an answer that breaks these rules, and that any member can report an answer.',
]

export const INVITED_LEADER_RULES = [
  'I will use CELLS only for Bible devotion with my own group, and I will keep it respectful.',
  'I will not allow hateful, harassing, sexual or abusive content, and I will review reported answers and remove what breaks these rules.',
  'I will only invite people I know, and I will keep their answers private to the group.',
]

export const PASTOR_RULES = [
  'I will use CELLS only to support the cell groups I oversee, and I will keep it respectful.',
  'I will not share participation information outside the church, and I will tell my leaders to remove content that breaks these rules.',
]

export const RULES_BY_ROLE = {
  MEMBER: MEMBER_RULES,
  LEADER: INVITED_LEADER_RULES,
  PASTOR: PASTOR_RULES,
}
