import { create } from 'zustand';
import { authService } from '../services/authService';

export const DEMO_USERS = {
  admin: {
    id: 'usr-admin-alice',
    username: 'Alice Walker',
    email: 'admin@sheetforge.com',
    role: 'ADMIN',
    roleLabel: 'Admin / Owner',
    color: '#3b82f6', // Blue
  },
  editor: {
    id: 'usr-editor-bob',
    username: 'Bob Smith',
    email: 'editor@sheetforge.com',
    role: 'EDITOR',
    roleLabel: 'Senior Editor',
    color: '#10b981', // Emerald
  },
  analyst: {
    id: 'usr-analyst-charlie',
    username: 'Charlie Kim',
    email: 'analyst@sheetforge.com',
    role: 'COMMENTER',
    roleLabel: 'Financial Analyst',
    color: '#f59e0b', // Amber
  },
};

const sessionRandomSuffix = Math.random().toString(36).substring(2, 6).toUpperCase();

// ── Guest Identity Generator (Google Docs style: "Guest 🐯 Tiger") ──────────
const GUEST_ANIMALS = [
  ['🐯', 'Tiger'], ['🐼', 'Panda'], ['🦊', 'Fox'], ['🐸', 'Frog'],
  ['🦁', 'Lion'], ['🐧', 'Penguin'], ['🦋', 'Butterfly'], ['🐺', 'Wolf'],
  ['🦄', 'Unicorn'], ['🐬', 'Dolphin'], ['🐻', 'Bear'], ['🦅', 'Eagle'],
  ['🐙', 'Octopus'], ['🦜', 'Parrot'], ['🐠', 'Fish'], ['🦊', 'Raccoon'],
];
const GUEST_COLORS = [
  '#e53935', '#8e24aa', '#00897b', '#f4511e',
  '#3949ab', '#039be5', '#7cb342', '#c0ca33',
  '#fb8c00', '#d81b60', '#00acc1', '#43a047',
];

function getOrCreateGuestIdentity() {
  // Re-use same guest identity within the same browser session (tab)
  const existing = sessionStorage.getItem('guest_identity');
  if (existing) {
    try { return JSON.parse(existing); } catch (e) {}
  }
  const animal = GUEST_ANIMALS[Math.floor(Math.random() * GUEST_ANIMALS.length)];
  const color = GUEST_COLORS[Math.floor(Math.random() * GUEST_COLORS.length)];
  const id = 'guest-' + Math.random().toString(36).substring(2, 9);
  const identity = {
    id,
    username: `Guest ${animal[0]} ${animal[1]}`,
    fullName: `Guest ${animal[0]} ${animal[1]}`,
    color,
    colorHex: color,
    role: 'GUEST',
  };
  sessionStorage.setItem('guest_identity', JSON.stringify(identity));
  return identity;
}
// ─────────────────────────────────────────────────────────────────────────────

export const useAuthStore = create((set, get) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  clientId: 'client-' + Math.random().toString(36).substring(2, 9),
  userColor: '#3b82f6',
  loading: false,
  error: null,

  initAuth: () => {
    // Check sessionStorage (per-tab) or localStorage
    const userJson = sessionStorage.getItem('user_info') || localStorage.getItem('user_info');
    const token = sessionStorage.getItem('access_token') || localStorage.getItem('access_token');
    const savedColor = sessionStorage.getItem('user_color');

    if (userJson && token) {
      try {
        const user = JSON.parse(userJson);
        set({
          user,
          token,
          isAuthenticated: true,
          userColor: savedColor || user.color || '#3b82f6',
        });
        return true;
      } catch (e) {
        sessionStorage.removeItem('user_info');
        localStorage.removeItem('user_info');
      }
    }

    // No login found — assign a unique guest identity so presence/cursors show a name
    const guestIdentity = getOrCreateGuestIdentity();
    set({
      user: guestIdentity,
      token: null,
      isAuthenticated: false,
      userColor: guestIdentity.color,
    });
    return false;
  },

  quickLoginAs: (userKey) => {
    const demoUser = DEMO_USERS[userKey] || DEMO_USERS.admin;
    const token = 'jwt-token-' + demoUser.id;

    sessionStorage.setItem('user_info', JSON.stringify(demoUser));
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('user_color', demoUser.color);
    localStorage.setItem('user_info', JSON.stringify(demoUser));
    localStorage.setItem('access_token', token);

    set({
      user: demoUser,
      token,
      isAuthenticated: true,
      userColor: demoUser.color,
      error: null,
    });
  },

  updateProfile: (name, color) => {
    const { user, userColor, clientId } = get();
    const cleanName = name?.trim() || 'User';
    const chosenColor = color || userColor || '#1a73e8';
    const updated = {
      ...(user || {}),
      id: user?.id || 'usr-' + clientId,
      username: cleanName,
      fullName: cleanName,
      color: chosenColor,
      colorHex: chosenColor,
    };
    try {
      sessionStorage.setItem('user_info', JSON.stringify(updated));
      sessionStorage.setItem('access_token', 'token-' + updated.id);
      sessionStorage.setItem('user_color', chosenColor);
      localStorage.setItem('user_info', JSON.stringify(updated));
      localStorage.setItem('access_token', 'token-' + updated.id);
    } catch (e) {}

    set({
      user: updated,
      token: 'token-' + updated.id,
      isAuthenticated: true,
      userColor: chosenColor,
    });
  },

  login: async (email, password) => {
    set({ loading: true, error: null });

    // Check pre-seeded accounts
    const match = Object.values(DEMO_USERS).find((u) => u.email.toLowerCase() === email.toLowerCase());
    if (match) {
      const token = 'jwt-token-' + match.id;
      sessionStorage.setItem('user_info', JSON.stringify(match));
      sessionStorage.setItem('access_token', token);
      sessionStorage.setItem('user_color', match.color);
      localStorage.setItem('user_info', JSON.stringify(match));
      localStorage.setItem('access_token', token);

      set({
        user: match,
        token,
        isAuthenticated: true,
        userColor: match.color,
        loading: false,
      });
      return { user: match, accessToken: token };
    }

    try {
      const data = await authService.login({ emailOrUsername: email, email, password });
      sessionStorage.setItem('user_info', JSON.stringify(data.user));
      sessionStorage.setItem('access_token', data.accessToken);
      localStorage.setItem('user_info', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
      set({
        user: data.user,
        token: data.accessToken,
        isAuthenticated: true,
        userColor: data.user?.colorHex || '#3b82f6',
        loading: false,
      });
      return data;
    } catch (err) {
      // Always show generic message for login — don't reveal which field is wrong (security)
      const msg = 'Username or password is incorrect. Please try again.';
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  register: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authService.register(data);
      if (res.accessToken && res.user) {
        sessionStorage.setItem('access_token', res.accessToken);
        sessionStorage.setItem('user_info', JSON.stringify(res.user));
        localStorage.setItem('access_token', res.accessToken);
        localStorage.setItem('user_info', JSON.stringify(res.user));
        set({
          user: res.user,
          token: res.accessToken,
          isAuthenticated: true,
          userColor: res.user.colorHex || '#3b82f6',
          loading: false,
        });
      }
      return res;
    } catch (err) {
      // Parse backend validation errors into user-friendly messages
      const raw = err.response?.data?.message || err.message || '';
      let msg;
      const lower = raw.toLowerCase();
      if (lower.includes('username') && (lower.includes('exist') || lower.includes('taken') || lower.includes('already'))) {
        msg = '⚠️ This username is already taken. Please choose a different one.';
      } else if (lower.includes('email') && (lower.includes('exist') || lower.includes('taken') || lower.includes('already'))) {
        msg = '⚠️ This email is already registered. Please sign in instead.';
      } else if (lower.includes('password') && (lower.includes('weak') || lower.includes('short') || lower.includes('length'))) {
        msg = '⚠️ Password is too weak. Use at least 8 characters with a number.';
      } else if (lower.includes('email') && lower.includes('valid')) {
        msg = '⚠️ Please enter a valid email address.';
      } else if (raw) {
        msg = raw;
      } else {
        msg = '⚠️ Registration failed. Please try again.';
      }
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  verifyOtp: async ({ email, otp, purpose = 'EMAIL_VERIFICATION' }) => {
    set({ loading: true, error: null });
    try {
      const res = await authService.verifyOtp({ email, otp, purpose });
      if (res.accessToken) {
        sessionStorage.setItem('access_token', res.accessToken);
        sessionStorage.setItem('user_info', JSON.stringify(res.user));
        set({
          user: res.user,
          token: res.accessToken,
          isAuthenticated: true,
          loading: false,
        });
      }
      return res;
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid or expired OTP code';
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  resendOtp: async (email, purpose = 'EMAIL_VERIFICATION') => {
    set({ loading: true, error: null });
    try {
      const res = await authService.resendOtp({ email, purpose });
      set({ loading: false });
      return res;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to resend OTP';
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  forgotPassword: async (email) => {
    set({ loading: true, error: null });
    try {
      const res = await authService.forgotPassword(email);
      set({ loading: false });
      return res;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to send reset code';
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  resetPassword: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authService.resetPassword(data);
      set({ loading: false });
      return res;
    } catch (err) {
      const msg = err.response?.data?.message || 'Password reset failed';
      set({ error: msg, loading: false });
      throw new Error(msg);
    }
  },

  logout: () => {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('user_info');
    sessionStorage.removeItem('user_color');
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_info');
    localStorage.removeItem('refresh_token');

    // 1. Remove ?wb= and ?sheet= from browser URL
    try {
      if (typeof window !== 'undefined') {
        const url = new URL(window.location);
        url.searchParams.delete('wb');
        url.searchParams.delete('workbookId');
        url.searchParams.delete('sheet');
        url.searchParams.delete('sheetId');
        window.history.replaceState({}, '', url.pathname);
      }
    } catch (e) {}

    // 2. Disconnect WebSocket collaboration session
    try {
      import('./useCollabStore').then(({ useCollabStore }) => {
        useCollabStore.getState().cleanupCollab();
      });
    } catch (e) {}

    // 3. Clear all cells from grid
    try {
      import('./useGridStore').then(({ useGridStore }) => {
        useGridStore.getState().clearGrid();
      });
    } catch (e) {}

    // 4. Reset workbook to clean empty state
    try {
      import('./useWorkbookStore').then(({ useWorkbookStore }) => {
        useWorkbookStore.getState().resetToDefault();
      });
    } catch (e) {}

    set({ user: null, token: null, isAuthenticated: false });
  }
}));
