import api from './api';

export const notificationService = {
  async getNotifications(page = 0, size = 20) {
    const res = await api.get(`/api/v1/notifications?page=${page}&size=${size}`);
    return res.data;
  },

  async getUnreadCount() {
    const res = await api.get('/api/v1/notifications/unread-count');
    return res.data?.unreadCount || 0;
  },

  async markAsRead(id) {
    const res = await api.put(`/api/v1/notifications/${id}/read`);
    return res.data;
  },

  async markAllAsRead() {
    const res = await api.put('/api/v1/notifications/read-all');
    return res.data;
  }
};
