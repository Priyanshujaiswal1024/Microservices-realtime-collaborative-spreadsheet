import { create } from 'zustand';
import { notificationService } from '../services/notificationService';

export const useNotificationStore = create((set, get) => ({
  notifications: [],
  unreadCount: 0,
  isOpen: false,
  loading: false,

  setIsOpen: (isOpen) => set({ isOpen }),

  loadNotifications: async () => {
    set({ loading: true });
    try {
      const res = await notificationService.getNotifications();
      const count = await notificationService.getUnreadCount();
      set({
        notifications: res.content || [],
        unreadCount: count,
        loading: false,
      });
    } catch (e) {
      set({ loading: false });
    }
  },

  markAsRead: async (id) => {
    try {
      await notificationService.markAsRead(id);
      const updated = get().notifications.map((n) => (n.id === id ? { ...n, read: true } : n));
      const unread = updated.filter((n) => !n.read).length;
      set({ notifications: updated, unreadCount: unread });
    } catch (e) {
      // ignore
    }
  },

  markAllAsRead: async () => {
    try {
      await notificationService.markAllAsRead();
      const updated = get().notifications.map((n) => ({ ...n, read: true }));
      set({ notifications: updated, unreadCount: 0 });
    } catch (e) {
      // ignore
    }
  }
}));
