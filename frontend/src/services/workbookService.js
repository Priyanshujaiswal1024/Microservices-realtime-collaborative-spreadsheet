import api from './api';
import * as XLSX from 'xlsx';

export const workbookService = {
  async listWorkbooks() {
    const res = await api.get('/api/v1/workbooks');
    return res.data;
  },

  async createWorkbook(data) {
    const res = await api.post('/api/v1/workbooks', data);
    return res.data;
  },

  async getWorkbook(id) {
    const res = await api.get(`/api/v1/workbooks/${id}`);
    return res.data;
  },

  async updateWorkbook(id, data) {
    const res = await api.put(`/api/v1/workbooks/${id}`, data);
    return res.data;
  },

  async deleteWorkbook(id) {
    const res = await api.delete(`/api/v1/workbooks/${id}`);
    return res.data;
  },

  async createSheet(workbookId, data) {
    const res = await api.post(`/api/v1/workbooks/${workbookId}/sheets`, data);
    return res.data;
  },

  async deleteSheet(sheetId) {
    const res = await api.delete(`/api/v1/sheets/${sheetId}`);
    return res.data;
  },

  async getSheetCells(sheetId) {
    const res = await api.get(`/api/v1/sheets/${sheetId}/cells`);
    return res.data;
  },

  async getLiveSheetState(sheetId) {
    const res = await api.get(`/api/v1/collab/sheet/${sheetId}/state`);
    return res.data;
  },

  async shareWorkbook(workbookId, targetUserOrEmail, role = 'EDITOR') {
    const isEmail = typeof targetUserOrEmail === 'string' && targetUserOrEmail.includes('@');
    const payload = typeof targetUserOrEmail === 'object' ? targetUserOrEmail : {
      userId: targetUserOrEmail,
      userEmail: isEmail ? targetUserOrEmail : null,
      role: role || 'EDITOR',
    };
    const res = await api.post(`/api/v1/workbooks/${workbookId}/permissions`, payload);
    return res.data;
  },

  async getPermissions(workbookId) {
    const res = await api.get(`/api/v1/workbooks/${workbookId}/permissions`);
    return res.data;
  },

  async revokePermission(workbookId, userId) {
    const res = await api.delete(`/api/v1/workbooks/${workbookId}/permissions/${userId}`);
    return res.data;
  },

  async getProtectedRanges(sheetId) {
    const res = await api.get(`/api/v1/sheets/${sheetId}/protected-ranges`);
    return res.data;
  },

  async createProtectedRange(sheetId, data) {
    const res = await api.post(`/api/v1/sheets/${sheetId}/protected-ranges`, data);
    return res.data;
  },

  async exportXlsx(workbookId, title = 'spreadsheet') {
    try {
      const { useWorkbookStore } = await import('../store/useWorkbookStore');
      const { useGridStore } = await import('../store/useGridStore');
      const { toA1Notation } = await import('../utils/coordinate');

      const wbStore = useWorkbookStore.getState();
      const gridStore = useGridStore.getState();
      const sheets = wbStore.workbook?.sheets || [{ id: wbStore.activeSheetId || 'sheet-1', name: 'Sheet1' }];

      const newWb = XLSX.utils.book_new();

      sheets.forEach((sheet) => {
        const sheetCells = gridStore.sheetCache?.[sheet.id] || (sheet.id === wbStore.activeSheetId ? gridStore.cells : {});
        const wsData = {};

        Object.entries(sheetCells).forEach(([key, cell]) => {
          if (cell && (cell.value !== undefined || cell.rawValue !== undefined)) {
            const [r, c] = key.split(':').map(Number);
            const a1 = toA1Notation(r, c);
            const val = cell.value !== undefined ? cell.value : cell.rawValue;
            const isNum = !isNaN(Number(val)) && String(val).trim() !== '';
            wsData[a1] = {
              t: isNum ? 'n' : 's',
              v: isNum ? Number(val) : String(val),
            };
          }
        });

        // Compute ref range
        const keys = Object.keys(wsData);
        let ref = 'A1:Z60';
        if (keys.length > 0) {
          const rows = Object.keys(sheetCells).map((k) => Number(k.split(':')[0]));
          const cols = Object.keys(sheetCells).map((k) => Number(k.split(':')[1]));
          const maxR = Math.max(0, ...rows);
          const maxC = Math.max(0, ...cols);
          ref = `A1:${toA1Notation(maxR, maxC)}`;
        }
        wsData['!ref'] = ref;

        XLSX.utils.book_append_sheet(newWb, wsData, (sheet.name || 'Sheet').replace(/[\\/?*[\]]/g, ''));
      });

      XLSX.writeFile(newWb, `${title || 'Spreadsheet'}.xlsx`);
    } catch (err) {
      console.error('Client XLSX export failed, attempting API fallback', err);
      try {
        const res = await api.get(`/api/v1/workbooks/${workbookId}/export/xlsx`, {
          responseType: 'blob',
        });
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `${title}.xlsx`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      } catch (apiErr) {
        console.error('Export XLSX failed', apiErr);
      }
    }
  },

  async exportCsv(sheetId, sheetName = 'sheet') {
    try {
      const { useWorkbookStore } = await import('../store/useWorkbookStore');
      const { useGridStore } = await import('../store/useGridStore');
      const { toA1Notation } = await import('../utils/coordinate');

      const wbStore = useWorkbookStore.getState();
      const gridStore = useGridStore.getState();
      const targetSheetId = sheetId || wbStore.activeSheetId;
      const sheetCells = gridStore.sheetCache?.[targetSheetId] || (targetSheetId === wbStore.activeSheetId ? gridStore.cells : {});

      const newWb = XLSX.utils.book_new();
      const wsData = {};

      Object.entries(sheetCells).forEach(([key, cell]) => {
        if (cell && (cell.value !== undefined || cell.rawValue !== undefined)) {
          const [r, c] = key.split(':').map(Number);
          const a1 = toA1Notation(r, c);
          const val = cell.value !== undefined ? cell.value : cell.rawValue;
          const isNum = !isNaN(Number(val)) && String(val).trim() !== '';
          wsData[a1] = {
            t: isNum ? 'n' : 's',
            v: isNum ? Number(val) : String(val),
          };
        }
      });

      const keys = Object.keys(wsData);
      let ref = 'A1:Z60';
      if (keys.length > 0) {
        const rows = Object.keys(sheetCells).map((k) => Number(k.split(':')[0]));
        const cols = Object.keys(sheetCells).map((k) => Number(k.split(':')[1]));
        const maxR = Math.max(0, ...rows);
        const maxC = Math.max(0, ...cols);
        ref = `A1:${toA1Notation(maxR, maxC)}`;
      }
      wsData['!ref'] = ref;

      XLSX.utils.book_append_sheet(newWb, wsData, 'Sheet');
      XLSX.writeFile(newWb, `${sheetName || 'sheet'}.csv`, { bookType: 'csv' });
    } catch (err) {
      console.error('Client CSV export failed, attempting API fallback', err);
      try {
        const res = await api.get(`/api/v1/sheets/${sheetId}/export/csv`, {
          responseType: 'blob',
        });
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `${sheetName}.csv`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      } catch (apiErr) {
        console.error('Export CSV failed', apiErr);
      }
    }
  },

  async importFile(file) {
    const data = await file.arrayBuffer();
    const workbook = XLSX.read(data, { type: 'array' });

    const { useWorkbookStore } = await import('../store/useWorkbookStore');
    const { useGridStore } = await import('../store/useGridStore');

    const wbStore = useWorkbookStore.getState();
    const gridStore = useGridStore.getState();

    const importedSheets = workbook.SheetNames.map((name, index) => ({
      id: `sheet-${index + 1}`,
      name,
      position: index,
      rowCount: 100,
      colCount: 26,
    }));

    const sheetCache = { ...gridStore.sheetCache };

    workbook.SheetNames.forEach((name, index) => {
      const sheetId = `sheet-${index + 1}`;
      const ws = workbook.Sheets[name];
      const json = XLSX.utils.sheet_to_json(ws, { header: 1 });
      const cellMap = {};

      json.forEach((rowArray, r) => {
        if (Array.isArray(rowArray)) {
          rowArray.forEach((val, c) => {
            if (val !== null && val !== undefined && String(val).trim() !== '') {
              cellMap[`${r}:${c}`] = {
                row: r,
                col: c,
                value: String(val),
                rawValue: String(val),
                dataType: !isNaN(Number(val)) ? 'NUMBER' : 'TEXT',
                format: {},
              };
            }
          });
        }
      });

      sheetCache[sheetId] = cellMap;
    });

    const newWb = {
      id: wbStore.workbook?.id || 'wb-imported',
      title: file.name.replace(/\.[^/.]+$/, ''),
      visibility: 'PRIVATE',
      userRole: 'OWNER',
      sheets: importedSheets,
      permissions: [],
    };

    wbStore.setWorkbook(newWb);
    gridStore.setSheetCache(sheetCache);
    gridStore.loadSheetCells(importedSheets[0].id);

    return newWb;
  }
};
