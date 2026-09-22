/**
 * Spreadsheet coordinate helpers (A1 notation, column letters, row numbers)
 */

export function indexToColLetter(col) {
  let result = '';
  let n = col;
  while (n >= 0) {
    result = String.fromCharCode((n % 26) + 65) + result;
    n = Math.floor(n / 26) - 1;
  }
  return result;
}

export function colLetterToIndex(str) {
  let result = 0;
  for (let i = 0; i < str.length; i++) {
    result = result * 26 + (str.charCodeAt(i) - 64);
  }
  return result - 1;
}

export function toA1Notation(row, col) {
  return `${indexToColLetter(col)}${row + 1}`;
}

export function parseA1Notation(a1) {
  if (!a1) return null;
  const match = a1.trim().toUpperCase().match(/^([A-Z]+)(\d+)$/);
  if (!match) return null;
  const col = colLetterToIndex(match[1]);
  const row = parseInt(match[2], 10) - 1;
  return { row, col };
}

export const USER_COLORS = [
  '#3b82f6', // blue
  '#10b981', // emerald
  '#f59e0b', // amber
  '#ef4444', // red
  '#8b5cf6', // purple
  '#ec4899', // pink
  '#06b6d4', // cyan
  '#f97316', // orange
];

export function getRandomUserColor() {
  return USER_COLORS[Math.floor(Math.random() * USER_COLORS.length)];
}
