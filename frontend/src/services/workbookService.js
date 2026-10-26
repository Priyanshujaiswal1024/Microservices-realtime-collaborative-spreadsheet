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

  async bulkUpdateCells(sheetId, updates) {
    const res = await api.post(`/api/v1/sheets/${sheetId}/cells/batch`, { updates });
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
    const fileName = `${title || 'Spreadsheet'}.xlsx`;
    try {
      const { useWorkbookStore } = await import('../store/useWorkbookStore');
      const { useGridStore } = await import('../store/useGridStore');

      const wbStore = useWorkbookStore.getState();
      const gridStore = useGridStore.getState();
      const sheets = wbStore.workbook?.sheets || [{ id: wbStore.activeSheetId || 'sheet-1', name: 'Sheet1' }];

      const liveSheets = sheets.map((sheet) => {
        const sheetCells = gridStore.sheetCache?.[sheet.id] || (sheet.id === wbStore.activeSheetId ? gridStore.cells : {});
        const cellsList = Object.entries(sheetCells).map(([key, cell]) => {
          const [r, c] = key.split(':').map(Number);
          return {
            row: r,
            col: c,
            value: cell.value !== undefined ? String(cell.value) : (cell.rawValue || ''),
            dataType: cell.dataType || 'TEXT',
            format: cell.format || {}
          };
        });
        return {
          name: sheet.name || 'Sheet1',
          cells: cellsList
        };
      });

      // 1. Primary: Generate authentic styled Excel via Apache POI with 100% cell background colors & fonts
      const res = await api.post('/api/v1/workbooks/export/xlsx', {
        title: title || 'Spreadsheet',
        sheets: liveSheets
      }, {
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.warn('Backend POI styled export failed, attempting database export...', err);
      try {
        const res = await api.get(`/api/v1/workbooks/${workbookId}/export/xlsx`, {
          responseType: 'blob',
        });
        const url = window.URL.createObjectURL(new Blob([res.data], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } catch (apiErr) {
        console.warn('Server export failed, falling back to client SheetJS...', apiErr);
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

        XLSX.writeFile(newWb, fileName);
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
    const { useWorkbookStore } = await import('../store/useWorkbookStore');
    const { useGridStore } = await import('../store/useGridStore');
    const { useCollabStore } = await import('../store/useCollabStore');
    const { stompClient } = await import('./stompClient');

    const wbStore = useWorkbookStore.getState();
    const gridStore = useGridStore.getState();

    // 1. Primary: Server-side import (creates real Postgres Workbook, Sheets, and persists all Cells)
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/api/v1/workbooks/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const importedWb = res.data;
      if (importedWb && importedWb.id && importedWb.sheets && importedWb.sheets.length > 0) {
        const firstSheetId = importedWb.sheets[0].id;
        wbStore.setWorkbook(importedWb);
        wbStore.setActiveSheetId(firstSheetId);

        // Update URL query ?wb=... so link sharing works directly
        try {
          const url = new URL(window.location.href);
          url.searchParams.set('wb', importedWb.id);
          url.searchParams.delete('sheet');
          url.searchParams.delete('sheetId');
          window.history.pushState({}, '', url.pathname + url.search);
        } catch (e) {}

        // Subscribe & load cells for first sheet
        try {
          stompClient.subscribeWorkbook(importedWb.id);
          await gridStore.loadSheetCells(firstSheetId, false);
          useCollabStore.getState().initCollab(firstSheetId);
        } catch (e) {}

        // Cache locally
        try {
          localStorage.setItem(`sheetforge_wb_${importedWb.id}`, JSON.stringify(importedWb));
        } catch (e) {}

        return importedWb;
      }
    } catch (serverErr) {
      console.warn('Server import endpoint failed, falling back to client-side parse & persist:', serverErr);
    }

    // 2. Client-side fallback if server import was unavailable
    const data = await file.arrayBuffer();
    const workbook = XLSX.read(data, { type: 'array' });

    const clientPrefix = 'sheet-' + Date.now();
    const importedSheets = workbook.SheetNames.map((name, index) => ({
      id: `${clientPrefix}-${index + 1}`,
      name,
      position: index,
      rowCount: 100,
      colCount: 26,
    }));

    const sheetCache = { ...gridStore.sheetCache };

    workbook.SheetNames.forEach((name, index) => {
      const sheetId = importedSheets[index].id;
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

    const clientWbId = 'wb-' + (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 10));
    const newWb = {
      id: clientWbId,
      title: file.name.replace(/\.[^/.]+$/, ''),
      visibility: 'PRIVATE',
      userRole: 'OWNER',
      sheets: importedSheets,
      permissions: [],
    };

    wbStore.setWorkbook(newWb);
    gridStore.setSheetCache(sheetCache);
    wbStore.setActiveSheetId(importedSheets[0].id);
    gridStore.loadSheetCells(importedSheets[0].id, false);

    return newWb;
  }
};
