import { create } from 'zustand';
import { workbookService } from '../services/workbookService';
import { HybridLogicalClock } from '../utils/crdt';
import { stompClient } from '../services/stompClient';
import { useAuthStore } from './useAuthStore';
import { useWorkbookStore } from './useWorkbookStore';
import { useUIStore } from './useUIStore';
import { parseA1Notation, toA1Notation } from '../utils/coordinate';

function evaluateCellFormula(rawVal, cells) {
  if (!rawVal || !rawVal.startsWith('=')) {
    return rawVal;
  }

  const formula = rawVal.substring(1).trim().toUpperCase();

  // Helper to extract cell values
  const getVal = (r, c) => {
    const key = `${r}:${c}`;
    const cell = cells[key];
    if (!cell || !cell.value) return 0;
    const num = parseFloat(cell.value);
    return isNaN(num) ? cell.value : num;
  };

  // SUM(A1:B5) or SUM(A1, B2)
  const sumMatch = formula.match(/^SUM\((.*)\)$/);
  if (sumMatch) {
    const rangeStr = sumMatch[1];
    let total = 0;
    if (rangeStr.includes(':')) {
      const [startStr, endStr] = rangeStr.split(':');
      const start = parseA1Notation(startStr);
      const end = parseA1Notation(endStr);
      if (start && end) {
        for (let r = Math.min(start.row, end.row); r <= Math.max(start.row, end.row); r++) {
          for (let c = Math.min(start.col, end.col); c <= Math.max(start.col, end.col); c++) {
            const v = getVal(r, c);
            if (typeof v === 'number') total += v;
          }
        }
      }
    } else {
      const parts = rangeStr.split(',');
      for (const p of parts) {
        const coord = parseA1Notation(p.trim());
        if (coord) {
          const v = getVal(coord.row, coord.col);
          if (typeof v === 'number') total += v;
        }
      }
    }
    return String(total);
  }

  // AVERAGE(A1:A5)
  const avgMatch = formula.match(/^AVERAGE\((.*)\)$/);
  if (avgMatch) {
    const rangeStr = avgMatch[1];
    let total = 0;
    let count = 0;
    if (rangeStr.includes(':')) {
      const [startStr, endStr] = rangeStr.split(':');
      const start = parseA1Notation(startStr);
      const end = parseA1Notation(endStr);
      if (start && end) {
        for (let r = Math.min(start.row, end.row); r <= Math.max(start.row, end.row); r++) {
          for (let c = Math.min(start.col, end.col); c <= Math.max(start.col, end.col); c++) {
            const v = getVal(r, c);
            if (typeof v === 'number') {
              total += v;
              count++;
            }
          }
        }
      }
    }
    return count > 0 ? String(parseFloat((total / count).toFixed(4))) : '0';
  }

  // COUNT(A1:A5)
  const countMatch = formula.match(/^COUNT\((.*)\)$/);
  if (countMatch) {
    const rangeStr = countMatch[1];
    let count = 0;
    if (rangeStr.includes(':')) {
      const [startStr, endStr] = rangeStr.split(':');
      const start = parseA1Notation(startStr);
      const end = parseA1Notation(endStr);
      if (start && end) {
        for (let r = Math.min(start.row, end.row); r <= Math.max(start.row, end.row); r++) {
          for (let c = Math.min(start.col, end.col); c <= Math.max(start.col, end.col); c++) {
            const key = `${r}:${c}`;
            if (cells[key] && cells[key].value !== undefined && cells[key].value !== '') {
              count++;
            }
          }
        }
      }
    }
    return String(count);
  }

  // Simple arithmetic A1 + B1, A1 * 2, etc.
  try {
    const expression = formula.replace(/[A-Z]+[0-9]+/g, (match) => {
      const coord = parseA1Notation(match);
      if (coord) {
        const val = getVal(coord.row, coord.col);
        return typeof val === 'number' ? val : 0;
      }
      return 0;
    });

    // Safely evaluate simple mathematical expressions (+ - * / () numbers)
    if (/^[0-9+\-*/().\s]+$/.test(expression)) {
      // eslint-disable-next-line no-eval
      const res = Function(`'use strict'; return (${expression})`)();
      return String(res);
    }
  } catch (e) {
    return '#ERROR!';
  }

  return rawVal;
}

export const useGridStore = create((set, get) => ({
  cells: {},
  selectedCell: { row: 0, col: 0 },
  selectedRange: null, // { startRow, endRow, startCol, endCol }
  editMode: false,
  formulaBarValue: '',
  undoStack: [],
  redoStack: [],
  currentHlc: new HybridLogicalClock(Date.now(), 0, 'client-init'),

  // Wipe everything in memory
  clearGrid: (targetSheetId) => {
    const sheetId = targetSheetId;
    const newCache = { ...get().sheetCache };
    if (sheetId) {
      delete newCache[sheetId];
      try {
        localStorage.removeItem(`sheetforge_cells_${sheetId}`);
      } catch (e) {}
    }
    set({
      cells: {},
      selectedCell: { row: 0, col: 0 },
      selectedRange: null,
      formulaBarValue: '',
      editMode: false,
      undoStack: [],
      redoStack: [],
      sheetCache: newCache,
    });
  },

  // Permanent clear: memory + localStorage + backend database + realtime broadcast
  clearSheet: async (targetSheetId) => {
    let sheetId = targetSheetId;
    if (!sheetId) {
      try {
        sheetId = useWorkbookStore.getState().activeSheetId;
      } catch (e) {}
    }
    sheetId = sheetId || 'sheet-1';

    const activeSheetId = useWorkbookStore.getState().activeSheetId;
    const isActiveSheet = (!sheetId || sheetId === activeSheetId);

    // 1. Wipe in-memory active cells & cached cells
    const newCache = { ...get().sheetCache };
    delete newCache[sheetId];

    if (isActiveSheet) {
      set({
        cells: {},
        selectedCell: { row: 0, col: 0 },
        selectedRange: null,
        formulaBarValue: '',
        editMode: false,
        undoStack: [],
        redoStack: [],
        sheetCache: newCache,
      });
    } else {
      set({ sheetCache: newCache });
    }

    // 2. Wipe from localStorage
    try {
      localStorage.removeItem(`sheetforge_cells_${sheetId}`);
    } catch (e) {}

    // 3. Clear from server database via API
    try {
      const api = (await import('../services/api')).default;
      await api.delete(`/api/v1/sheets/${sheetId}/cells`);
    } catch (err) {
      console.warn('Failed to clear sheet cells on server:', err);
    }

    // 4. Broadcast sheet clear to collaborators so all windows wipe their grid instantly
    try {
      stompClient.sendClearSheet(sheetId);
    } catch (e) {
      console.warn('Failed to broadcast clearSheet:', e);
    }
  },

  handleRemoteClearSheet: (sheetId) => {
    const newCache = { ...get().sheetCache };
    if (sheetId) {
      delete newCache[sheetId];
      try {
        localStorage.removeItem(`sheetforge_cells_${sheetId}`);
      } catch (e) {}
    }

    const activeId = useWorkbookStore.getState().activeSheetId;
    if (!sheetId || sheetId === activeId) {
      set({
        cells: {},
        formulaBarValue: '',
        selectedCell: { row: 0, col: 0 },
        selectedRange: null,
        sheetCache: newCache,
        undoStack: [],
        redoStack: [],
      });
    } else {
      set({ sheetCache: newCache });
    }
  },

  setSelectedCell: (row, col) => {
    const { cells } = get();
    const key = `${row}:${col}`;
    const rawVal = cells[key]?.rawValue || cells[key]?.value || '';
    set({
      selectedCell: { row, col },
      formulaBarValue: rawVal,
      editMode: false,
      selectedRange: { startRow: row, endRow: row, startCol: col, endCol: col },
    });
  },

  setSelectedRange: (startRow, startCol, endRow, endCol) => {
    set({
      selectedRange: {
        startRow: Math.min(startRow, endRow),
        endRow: Math.max(startRow, endRow),
        startCol: Math.min(startCol, endCol),
        endCol: Math.max(startCol, endCol),
      }
    });
  },

  setFormulaBarValue: (val) => {
    set({ formulaBarValue: val });
  },

  setEditMode: (active) => {
    set({ editMode: active });
  },

  sheetCache: {},
  setSheetCache: (sheetCache) => set({ sheetCache }),

  saveActiveSheetToCache: (sheetId) => {
    if (!sheetId) return;
    const { cells, sheetCache } = get();
    if (!cells) return;
    const updatedCache = { ...sheetCache, [sheetId]: { ...cells } };
    set({ sheetCache: updatedCache });
    try {
      localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(cells));
    } catch (e) {}
  },

  updateCachedSheetCell: (sheetId, row, col, cellState) => {
    if (!sheetId || row === undefined || col === undefined || !cellState) return;
    const { sheetCache } = get();
    const sheetMap = { ...(sheetCache[sheetId] || {}) };
    const key = `${row}:${col}`;
    const rawVal = cellState.value !== undefined ? cellState.value : '';

    let cellFormat = {};
    if (typeof cellState.format === 'string') {
      try {
        cellFormat = JSON.parse(cellState.format);
      } catch (e) {
        cellFormat = {};
      }
    } else if (cellState.format && typeof cellState.format === 'object') {
      cellFormat = cellState.format;
    }

    sheetMap[key] = {
      row,
      col,
      rawValue: rawVal,
      value: rawVal,
      dataType: cellState.dataType || 'TEXT',
      format: cellFormat,
      lastModifiedTs: cellState.timestamp,
      lastModifiedBy: cellState.userId,
    };

    set({ sheetCache: { ...sheetCache, [sheetId]: sheetMap } });
    try {
      localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(sheetMap));
    } catch (e) {}
  },

  loadSheetCells: async (sheetId, forceRefresh = false) => {
    if (!sheetId) return;
    const { sheetCache } = get();

    // 1. Get cached cells from memory or localStorage
    let cachedCells = sheetCache[sheetId];
    if (!cachedCells || Object.keys(cachedCells).length === 0) {
      try {
        const raw = localStorage.getItem(`sheetforge_cells_${sheetId}`);
        if (raw) {
          cachedCells = JSON.parse(raw);
        }
      } catch (e) {}
    }

    // If cache exists and not forcing hard refresh from server, render cache immediately!
    if (!forceRefresh && cachedCells && Object.keys(cachedCells).length > 0) {
      set({
        cells: cachedCells,
        sheetCache: { ...sheetCache, [sheetId]: cachedCells },
        formulaBarValue: '',
        selectedCell: { row: 0, col: 0 },
      });
      return;
    }

    // Clear cells first so stale data never bleeds into new sheet
    set({ cells: {}, formulaBarValue: '', selectedCell: { row: 0, col: 0 } });

    try {
      // Always try server first; cache is secondary
      let data;
      try {
        data = await workbookService.getLiveSheetState(sheetId);
      } catch (e) {
        data = await workbookService.getSheetCells(sheetId);
      }

      const cellMap = {};
      if (Array.isArray(data) && data.length > 0) {
        data.forEach((c) => {
          const key = `${c.row}:${c.col}`;
          cellMap[key] = {
            row: c.row,
            col: c.col,
            value: c.value,
            rawValue: c.value,
            dataType: c.dataType || 'TEXT',
            format: c.format || {},
            lastModifiedTs: c.lastModifiedTs,
            lastModifiedBy: c.lastModifiedBy,
          };
        });
      } else if (data && typeof data === 'object' && Object.keys(data).length > 0) {
        Object.entries(data).forEach(([coord, state]) => {
          const [r, c] = coord.split(':').map(Number);
          cellMap[coord] = {
            row: r,
            col: c,
            value: state.value,
            rawValue: state.value,
            dataType: state.dataType || 'TEXT',
            format: state.format || {},
            lastModifiedTs: state.timestamp?.toCompactString ? state.timestamp.toCompactString() : state.timestamp,
            lastModifiedBy: state.userId,
          };
        });
      }

      if (Object.keys(cellMap).length > 0) {
        // Re-evaluate formulas
        Object.keys(cellMap).forEach((key) => {
          if (cellMap[key].rawValue?.startsWith('=')) {
            cellMap[key].value = evaluateCellFormula(cellMap[key].rawValue, cellMap);
          }
        });

        set({
          cells: cellMap,
          sheetCache: { ...sheetCache, [sheetId]: cellMap },
          formulaBarValue: '',
          selectedCell: { row: 0, col: 0 },
        });
        try {
          localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(cellMap));
        } catch (e) {}
        return;
      }
    } catch (err) {
      console.warn('Could not load sheet cells from server, checking local cache for sheet', sheetId);
    }

    // If server returned nothing/failed, but we have local cached cells, RESTORE THEM! NEVER WIPE!
    if (cachedCells && Object.keys(cachedCells).length > 0) {
      set({
        cells: cachedCells,
        sheetCache: { ...sheetCache, [sheetId]: cachedCells },
        formulaBarValue: '',
        selectedCell: { row: 0, col: 0 },
      });
      return;
    }

    // Clean blank spreadsheet for fresh input
    const cleanMap = {};

    set({
      cells: cleanMap,
      sheetCache: { ...sheetCache, [sheetId]: cleanMap },
      formulaBarValue: '',
      selectedCell: { row: 0, col: 0 },
    });
    try {
      localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(cleanMap));
    } catch (e) {}
  },

  updateCellValue: (row, col, rawVal, sheetId, isRemote = false, remoteHlc = null, remoteFormat = null) => {
    // Protection guard: prevent modifying protected cell unless authorized
    if (!isRemote) {
      try {
        const wbStore = useWorkbookStore.getState();
        const auth = useAuthStore.getState();
        const currentUserId = auth.user?.id || auth.user?.username || auth.clientId;
        const isOwner = (wbStore.workbook?.ownerId && auth.user?.id && wbStore.workbook.ownerId === auth.user.id)
          || (wbStore.workbook?.userRole === 'OWNER' && auth.isAuthenticated);

        if (wbStore.isCellProtected(sheetId, row, col, currentUserId, isOwner ? 'OWNER' : 'COLLABORATOR')) {
          useUIStore.getState().showToast('🔒 This cell is protected and cannot be edited by your account.', 'warning');
          return;
        }
      } catch (e) {}
    }

    const { cells, currentHlc } = get();
    const key = `${row}:${col}`;
    const previous = cells[key];

    let newHlc;
    if (isRemote && remoteHlc) {
      currentHlc.update(remoteHlc);
      newHlc = remoteHlc;
    } else {
      const clientId = useAuthStore.getState().clientId || 'client-local';
      newHlc = HybridLogicalClock.now(clientId);
    }

    const computedVal = evaluateCellFormula(rawVal, { ...cells, [key]: { value: rawVal } });
    const isNum = !isNaN(Number(computedVal)) && computedVal.trim() !== '';

    let cellFormat = previous?.format || {};
    if (remoteFormat !== null && remoteFormat !== undefined) {
      cellFormat = remoteFormat;
    }

    const updatedCell = {
      row,
      col,
      rawValue: rawVal,
      value: computedVal,
      dataType: isNum ? 'NUMBER' : 'TEXT',
      format: cellFormat,
      lastModifiedTs: newHlc.toCompactString ? newHlc.toCompactString() : String(newHlc),
      lastModifiedBy: useAuthStore.getState().user?.id || 'guest',
    };

    const newCells = { ...cells, [key]: updatedCell };

    // Re-evaluate dependent formulas across grid
    Object.keys(newCells).forEach((k) => {
      if (newCells[k].rawValue && newCells[k].rawValue.startsWith('=')) {
        newCells[k].value = evaluateCellFormula(newCells[k].rawValue, newCells);
      }
    });

    const { sheetCache, undoStack } = get();
    const nextUndoStack = !isRemote && rawVal !== (previous?.rawValue || '')
      ? [
          ...undoStack.slice(-49),
          {
            type: 'CELL_EDIT',
            sheetId,
            row,
            col,
            prevRawValue: previous?.rawValue || '',
            newRawValue: rawVal,
            prevFormat: previous?.format || {},
            newFormat: cellFormat,
          },
        ]
      : undoStack;

    set({
      cells: newCells,
      formulaBarValue: rawVal,
      currentHlc,
      undoStack: nextUndoStack,
      redoStack: !isRemote ? [] : get().redoStack,
      sheetCache: sheetId ? { ...sheetCache, [sheetId]: newCells } : sheetCache,
    });

    if (sheetId) {
      try {
        localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(newCells));
      } catch (e) {}
    }

    // If local edit, dispatch via STOMP WebSocket
    if (!isRemote && sheetId) {
      const cellStatePayload = {
        value: rawVal,
        timestamp: {
          physicalTime: newHlc.physicalTime,
          logicalCounter: newHlc.logicalCounter,
          clientId: newHlc.clientId,
        },
        clientId: newHlc.clientId,
        userId: useAuthStore.getState().user?.id || 'guest',
        dataType: updatedCell.dataType,
        format: typeof updatedCell.format === 'string' ? updatedCell.format : JSON.stringify(updatedCell.format || {}),
      };

      stompClient.sendCellEdit(sheetId, row, col, cellStatePayload);
    }
  },

  updateBatchCells: (sheetId, updates, isRemote = false) => {
    if (!updates || !updates.length) return;
    const { cells, sheetCache } = get();
    const activeSheetId = useWorkbookStore.getState().activeSheetId;
    const isActiveSheet = (!sheetId || sheetId === activeSheetId);
    const targetMap = isActiveSheet ? { ...cells } : { ...(sheetCache[sheetId] || {}) };

    updates.forEach((item) => {
      const row = item.row;
      const col = item.col;
      if (row === undefined || col === undefined) return;

      const state = item.cellState || item;
      const key = `${row}:${col}`;
      const previous = targetMap[key];

      const rawVal = state.value !== undefined ? state.value : (previous?.rawValue || '');
      let cellFormat = previous?.format || {};

      if (state.format !== null && state.format !== undefined) {
        if (typeof state.format === 'string') {
          try {
            cellFormat = JSON.parse(state.format);
          } catch (e) {
            cellFormat = {};
          }
        } else if (typeof state.format === 'object') {
          cellFormat = state.format;
        }
      }

      const computedVal = evaluateCellFormula(rawVal, { ...targetMap, [key]: { value: rawVal } });
      const isNum = !isNaN(Number(computedVal)) && String(computedVal).trim() !== '';

      targetMap[key] = {
        row,
        col,
        rawValue: rawVal,
        value: computedVal,
        dataType: isNum ? 'NUMBER' : 'TEXT',
        format: cellFormat,
        lastModifiedTs: state.timestamp?.toCompactString ? state.timestamp.toCompactString() : (state.timestamp || Date.now()),
        lastModifiedBy: state.userId || 'guest',
      };
    });

    // Re-evaluate dependent formulas across grid ONCE
    Object.keys(targetMap).forEach((k) => {
      if (targetMap[k].rawValue && targetMap[k].rawValue.startsWith('=')) {
        targetMap[k].value = evaluateCellFormula(targetMap[k].rawValue, targetMap);
      }
    });

    const newCache = sheetId ? { ...sheetCache, [sheetId]: targetMap } : sheetCache;

    if (isActiveSheet) {
      set({
        cells: targetMap,
        sheetCache: newCache,
      });
    } else {
      set({
        sheetCache: newCache,
      });
    }

    if (sheetId) {
      try {
        localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(targetMap));
      } catch (e) {}
    }
  },

  formatSelection: (formatPatch, sheetId, isPreview = false) => {
    const { cells, selectedCell, selectedRange, undoStack } = get();
    const r1 = selectedRange ? selectedRange.startRow : selectedCell.row;
    const r2 = selectedRange ? selectedRange.endRow : selectedCell.row;
    const c1 = selectedRange ? selectedRange.startCol : selectedCell.col;
    const c2 = selectedRange ? selectedRange.endCol : selectedCell.col;

    const startRow = Math.min(r1, r2);
    const endRow = Math.max(r1, r2);
    const startCol = Math.min(c1, c2);
    const endCol = Math.max(c1, c2);

    const newCells = { ...cells };
    const changes = [];
    const batchUpdates = [];
    const auth = useAuthStore.getState();
    const clientId = auth.clientId || 'client-local';
    const currentUserId = auth.user?.id || 'guest';
    const hlc = HybridLogicalClock.now(clientId);

    for (let r = startRow; r <= endRow; r++) {
      for (let c = startCol; c <= endCol; c++) {
        const key = `${r}:${c}`;
        const current = newCells[key] || { row: r, col: c, value: '', rawValue: '', format: {} };
        const updatedFormat = { ...current.format, ...formatPatch };

        if (!isPreview) {
          changes.push({
            row: r,
            col: c,
            prevFormat: current.format || {},
            newFormat: updatedFormat,
          });

          batchUpdates.push({
            row: r,
            col: c,
            cellState: {
              value: current.rawValue || current.value || '',
              timestamp: { physicalTime: hlc.physicalTime, logicalCounter: hlc.logicalCounter, clientId: hlc.clientId },
              clientId: hlc.clientId,
              userId: currentUserId,
              dataType: current.dataType || 'TEXT',
              format: JSON.stringify(updatedFormat),
            }
          });
        }

        newCells[key] = {
          ...current,
          format: updatedFormat,
        };
      }
    }

    const { sheetCache } = get();
    set({
      cells: newCells,
      undoStack: changes.length > 0 && !isPreview ? [...undoStack.slice(-49), { type: 'FORMAT_BATCH', sheetId, changes }] : undoStack,
      redoStack: isPreview ? get().redoStack : [],
      sheetCache: sheetId ? { ...sheetCache, [sheetId]: newCells } : sheetCache
    });

    if (sheetId && !isPreview) {
      try {
        localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(newCells));
      } catch (e) {}

      // Batch send ONE atomic message across STOMP, BroadcastChannel & LocalStorage
      if (batchUpdates.length > 0) {
        stompClient.sendBatchCellEdit(sheetId, batchUpdates, currentUserId);
      }
    }
  },

  undo: () => {
    const { undoStack, redoStack, cells } = get();
    if (undoStack.length === 0) return;

    const action = undoStack[undoStack.length - 1];
    const newUndoStack = undoStack.slice(0, -1);

    if (action.type === 'CELL_EDIT') {
      const { row, col, prevRawValue, prevFormat, sheetId } = action;
      const key = `${row}:${col}`;
      const currentCell = cells[key];

      const computedVal = evaluateCellFormula(prevRawValue, { ...cells, [key]: { value: prevRawValue } });
      const isNum = !isNaN(Number(computedVal)) && computedVal.trim() !== '';

      const revertedCell = {
        row,
        col,
        rawValue: prevRawValue,
        value: computedVal,
        dataType: isNum ? 'NUMBER' : 'TEXT',
        format: prevFormat,
        lastModifiedTs: Date.now(),
        lastModifiedBy: useAuthStore.getState().user?.id || 'guest',
      };

      const newCells = { ...cells, [key]: revertedCell };
      Object.keys(newCells).forEach((k) => {
        if (newCells[k].rawValue && newCells[k].rawValue.startsWith('=')) {
          newCells[k].value = evaluateCellFormula(newCells[k].rawValue, newCells);
        }
      });

      set({
        cells: newCells,
        formulaBarValue: prevRawValue,
        selectedCell: { row, col },
        undoStack: newUndoStack,
        redoStack: [...redoStack, action],
        sheetCache: sheetId ? { ...get().sheetCache, [sheetId]: newCells } : get().sheetCache,
      });

      if (sheetId) {
        try {
          localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(newCells));
        } catch (e) {}
      }

      if (sheetId) {
        const clientId = useAuthStore.getState().clientId || 'client-local';
        const hlc = HybridLogicalClock.now(clientId);
        stompClient.sendCellEdit(sheetId, row, col, {
          value: prevRawValue,
          timestamp: { physicalTime: hlc.physicalTime, logicalCounter: hlc.logicalCounter, clientId: hlc.clientId },
          clientId: hlc.clientId,
          userId: useAuthStore.getState().user?.id || 'guest',
          dataType: revertedCell.dataType,
          format: JSON.stringify(prevFormat || {}),
        });
      }
    } else if (action.type === 'FORMAT_BATCH') {
      const { changes, sheetId } = action;
      const activeSheetId = useWorkbookStore.getState().activeSheetId;
      const isActiveSheet = (!sheetId || sheetId === activeSheetId);
      const { cells, sheetCache } = get();
      const targetCells = isActiveSheet ? { ...cells } : { ...(sheetCache[sheetId] || {}) };
      const batchUpdates = [];
      const auth = useAuthStore.getState();
      const clientId = auth.clientId || 'client-local';
      const currentUserId = auth.user?.id || 'guest';
      const hlc = HybridLogicalClock.now(clientId);

      changes.forEach(({ row, col, prevFormat }) => {
        const key = `${row}:${col}`;
        if (targetCells[key]) {
          targetCells[key] = {
            ...targetCells[key],
            format: prevFormat || {},
          };
        }
        if (sheetId) {
          batchUpdates.push({
            row,
            col,
            cellState: {
              value: targetCells[key]?.rawValue || '',
              timestamp: { physicalTime: hlc.physicalTime, logicalCounter: hlc.logicalCounter, clientId: hlc.clientId },
              clientId: hlc.clientId,
              userId: currentUserId,
              dataType: targetCells[key]?.dataType || 'TEXT',
              format: JSON.stringify(prevFormat || {}),
            }
          });
        }
      });

      const newCache = sheetId ? { ...sheetCache, [sheetId]: targetCells } : sheetCache;
      if (isActiveSheet) {
        set({
          cells: targetCells,
          undoStack: newUndoStack,
          redoStack: [...redoStack, action],
          sheetCache: newCache,
        });
      } else {
        set({
          undoStack: newUndoStack,
          redoStack: [...redoStack, action],
          sheetCache: newCache,
        });
      }

      if (sheetId) {
        try {
          localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(targetCells));
        } catch (e) {}

        if (batchUpdates.length > 0) {
          stompClient.sendBatchCellEdit(sheetId, batchUpdates, currentUserId);
        }
      }
    }
  },

  redo: () => {
    const { undoStack, redoStack, cells } = get();
    if (redoStack.length === 0) return;

    const action = redoStack[redoStack.length - 1];
    const newRedoStack = redoStack.slice(0, -1);

    if (action.type === 'CELL_EDIT') {
      const { row, col, newRawValue, newFormat, sheetId } = action;
      const key = `${row}:${col}`;

      const computedVal = evaluateCellFormula(newRawValue, { ...cells, [key]: { value: newRawValue } });
      const isNum = !isNaN(Number(computedVal)) && computedVal.trim() !== '';

      const reappliedCell = {
        row,
        col,
        rawValue: newRawValue,
        value: computedVal,
        dataType: isNum ? 'NUMBER' : 'TEXT',
        format: newFormat,
        lastModifiedTs: Date.now(),
        lastModifiedBy: useAuthStore.getState().user?.id || 'guest',
      };

      const newCells = { ...cells, [key]: reappliedCell };
      Object.keys(newCells).forEach((k) => {
        if (newCells[k].rawValue && newCells[k].rawValue.startsWith('=')) {
          newCells[k].value = evaluateCellFormula(newCells[k].rawValue, newCells);
        }
      });

      set({
        cells: newCells,
        formulaBarValue: newRawValue,
        selectedCell: { row, col },
        undoStack: [...undoStack, action],
        redoStack: newRedoStack,
        sheetCache: sheetId ? { ...get().sheetCache, [sheetId]: newCells } : get().sheetCache,
      });

      if (sheetId) {
        try {
          localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(newCells));
        } catch (e) {}
      }

      if (sheetId) {
        const clientId = useAuthStore.getState().clientId || 'client-local';
        const hlc = HybridLogicalClock.now(clientId);
        stompClient.sendCellEdit(sheetId, row, col, {
          value: newRawValue,
          timestamp: { physicalTime: hlc.physicalTime, logicalCounter: hlc.logicalCounter, clientId: hlc.clientId },
          clientId: hlc.clientId,
          userId: useAuthStore.getState().user?.id || 'guest',
          dataType: reappliedCell.dataType,
          format: JSON.stringify(newFormat || {}),
        });
      }
    } else if (action.type === 'FORMAT_BATCH') {
      const { changes, sheetId } = action;
      const activeSheetId = useWorkbookStore.getState().activeSheetId;
      const isActiveSheet = (!sheetId || sheetId === activeSheetId);
      const { cells, sheetCache } = get();
      const targetCells = isActiveSheet ? { ...cells } : { ...(sheetCache[sheetId] || {}) };
      const batchUpdates = [];
      const auth = useAuthStore.getState();
      const clientId = auth.clientId || 'client-local';
      const currentUserId = auth.user?.id || 'guest';
      const hlc = HybridLogicalClock.now(clientId);

      changes.forEach(({ row, col, newFormat }) => {
        const key = `${row}:${col}`;
        if (targetCells[key]) {
          targetCells[key] = {
            ...targetCells[key],
            format: newFormat || {},
          };
        }
        if (sheetId) {
          batchUpdates.push({
            row,
            col,
            cellState: {
              value: targetCells[key]?.rawValue || '',
              timestamp: { physicalTime: hlc.physicalTime, logicalCounter: hlc.logicalCounter, clientId: hlc.clientId },
              clientId: hlc.clientId,
              userId: currentUserId,
              dataType: targetCells[key]?.dataType || 'TEXT',
              format: JSON.stringify(newFormat || {}),
            }
          });
        }
      });

      const newCache = sheetId ? { ...sheetCache, [sheetId]: targetCells } : sheetCache;
      if (isActiveSheet) {
        set({
          cells: targetCells,
          undoStack: [...undoStack, action],
          redoStack: newRedoStack,
          sheetCache: newCache,
        });
      } else {
        set({
          undoStack: [...undoStack, action],
          redoStack: newRedoStack,
          sheetCache: newCache,
        });
      }

      if (sheetId) {
        try {
          localStorage.setItem(`sheetforge_cells_${sheetId}`, JSON.stringify(targetCells));
        } catch (e) {}

        if (batchUpdates.length > 0) {
          stompClient.sendBatchCellEdit(sheetId, batchUpdates, currentUserId);
        }
      }
    }
  }
}));
