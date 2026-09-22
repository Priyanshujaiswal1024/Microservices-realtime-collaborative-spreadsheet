import api from './api';

export const authService = {
  async register(data) {
    const res = await api.post('/api/v1/auth/register', data);
    return res.data;
  },

  async login(data) {
    const payload = {
      emailOrUsername: data.emailOrUsername || data.email || data.username,
      password: data.password
    };
    const res = await api.post('/api/v1/auth/login', payload);
    if (res.data.accessToken) {
      localStorage.setItem('access_token', res.data.accessToken);
      localStorage.setItem('refresh_token', res.data.refreshToken);
      localStorage.setItem('user_info', JSON.stringify(res.data.user));
    }
    return res.data;
  },

  async getCurrentUser() {
    const res = await api.get('/api/v1/users/me');
    return res.data;
  },

  async searchUsers(query) {
    const res = await api.get(`/api/v1/users/search?q=${encodeURIComponent(query)}`);
    return res.data;
  },

  async verifyOtp(data) {
    const res = await api.post('/api/v1/auth/verify-otp', data);
    if (res.data.accessToken) {
      localStorage.setItem('access_token', res.data.accessToken);
      localStorage.setItem('refresh_token', res.data.refreshToken);
      localStorage.setItem('user_info', JSON.stringify(res.data.user));
    }
    return res.data;
  },

  async resendOtp(data) {
    const res = await api.post('/api/v1/auth/resend-otp', data);
    return res.data;
  },

  async forgotPassword(email) {
    const res = await api.post('/api/v1/auth/forgot-password', { email });
    return res.data;
  },

  async resetPassword(data) {
    const res = await api.post('/api/v1/auth/reset-password', data);
    return res.data;
  },

  async logout() {
    const refreshToken = localStorage.getItem('refresh_token');
    try {
      if (refreshToken) {
        await api.post('/api/v1/auth/logout', { refreshToken });
      }
    } finally {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user_info');
    }
  }
};
