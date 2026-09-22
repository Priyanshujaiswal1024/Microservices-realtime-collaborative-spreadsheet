const GOOGLE_AVATAR_COLORS = [
  '#1a73e8', // Google Blue
  '#0f9d58', // Google Green
  '#d93025', // Google Red
  '#e37400', // Google Orange
  '#8e24aa', // Google Purple
  '#d81b60', // Google Pink
  '#00897b', // Google Teal
  '#3949ab', // Google Indigo
  '#5c6bc0', // Google Slate Blue
];

export function getAvatarColor(name) {
  if (!name) return '#5f6368';
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % GOOGLE_AVATAR_COLORS.length;
  return GOOGLE_AVATAR_COLORS[index];
}

export function getUserInitial(user) {
  if (!user) return '?';
  const name = user.fullName || user.username || user.name || user.email || '';
  if (!name) return '?';
  return name.trim().charAt(0).toUpperCase();
}
