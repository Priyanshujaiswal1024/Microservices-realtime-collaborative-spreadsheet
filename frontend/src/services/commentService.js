import api from './api';

export const commentService = {
  async getWorkbookComments(workbookId) {
    const res = await api.get(`/api/v1/workbooks/${workbookId}/comments`);
    return res.data;
  },

  async getSheetComments(sheetId) {
    const res = await api.get(`/api/v1/sheets/${sheetId}/comments`);
    return res.data;
  },

  async createComment(sheetId, data) {
    const res = await api.post(`/api/v1/sheets/${sheetId}/comments`, data);
    return res.data;
  },

  async addReply(threadId, data) {
    const res = await api.post(`/api/v1/comments/${threadId}/replies`, data);
    return res.data;
  },

  async resolveThread(threadId) {
    const res = await api.put(`/api/v1/comments/${threadId}/resolve`);
    return res.data;
  },

  async deleteThread(threadId) {
    const res = await api.delete(`/api/v1/comments/${threadId}`);
    return res.data;
  }
};
