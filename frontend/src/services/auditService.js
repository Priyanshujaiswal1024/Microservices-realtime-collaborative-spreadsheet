import api from './api';

export const auditService = {
  async getWorkbookHistory(workbookId, page = 0, size = 20) {
    const res = await api.get(`/api/v1/audit/workbooks/${workbookId}/history?page=${page}&size=${size}`);
    return res.data;
  },

  async getSnapshots(workbookId) {
    const res = await api.get(`/api/v1/audit/workbooks/${workbookId}/snapshots`);
    return res.data;
  },

  async createSnapshot(workbookId, data) {
    const res = await api.post(`/api/v1/audit/workbooks/${workbookId}/snapshots`, data);
    return res.data;
  },

  async restoreWorkbook(workbookId, data) {
    const res = await api.post(`/api/v1/audit/workbooks/${workbookId}/restore`, data);
    return res.data;
  },

  async getReplayOps(sheetId, lastOpId = '-') {
    const res = await api.get(`/api/v1/collab/sheet/${sheetId}/replay?lastOpId=${lastOpId}`);
    return res.data;
  }
};
