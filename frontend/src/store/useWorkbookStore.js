import { create } from 'zustand';
import { workbookService } from '../services/workbookService';
import { stompClient } from '../services/stompClient';
import { useGridStore } from './useGridStore';
import { useCollabStore } from './useCollabStore';
import { useUIStore } from './useUIStore';

const getFreshId = () => 'sheet-' + (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID().substring(0, 8) : Math.random().toString(36).substring(2, 8));
const DEFAULT_SHEET_ID = getFreshId();
const DEFAULT_WORKBOOK = {
  id: null,
  title: 'Untitled Spreadsheet',
  visibility: 'PRIVATE',
  userRole: 'OWNER',
  sheets: [
    { id: DEFAULT_SHEET_ID, name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 },
  ],
  permissions: [],
};

export const useWorkbookStore = create((set, get) => ({
  workbook: DEFAULT_WORKBOOK,
  activeSheetId: DEFAULT_SHEET_ID,
  loading: false,
  error: null,
  isSaving: false,

  setWorkbook: (workbook) => {
    const activeSheetId = workbook?.sheets?.[0]?.id || DEFAULT_SHEET_ID;
    set({ workbook: workbook || DEFAULT_WORKBOOK, activeSheetId });
    if (workbook?.id) {
      stompClient.subscribeWorkbook(workbook.id);
    }
  },

  setActiveSheetId: (sheetId) => {
    set({ activeSheetId: sheetId });
  },

  fetchWorkbook: async (id) => {
    set({ loading: true, error: null });
    if (id) {
      stompClient.subscribeWorkbook(id);
    }

    // 1. Check local storage cache first
    let cachedWb = null;
    try {
      const raw = localStorage.getItem(`sheetforge_wb_${id}`);
      if (raw) cachedWb = JSON.parse(raw);
    } catch (e) {}

    try {
      const data = await workbookService.getWorkbook(id);
      if (data && data.sheets?.length > 0) {
        const currentActiveId = get().activeSheetId;
        const activeStillExists = data.sheets.some((s) => s.id === currentActiveId);
        const activeSheetId = activeStillExists ? currentActiveId : data.sheets[0].id;

        set({ workbook: data, activeSheetId, loading: false });
        stompClient.subscribeWorkbook(data.id);
        try {
          localStorage.setItem(`sheetforge_wb_${id}`, JSON.stringify(data));
        } catch (e) {}
        return data;
      }
    } catch (err) {}

    if (cachedWb) {
      set({ workbook: cachedWb, activeSheetId: cachedWb.sheets?.[0]?.id || 'sheet-1', loading: false });
      stompClient.subscribeWorkbook(cachedWb.id);
      return cachedWb;
    }

    // Clean default workbook
    const freshSheetId = 'sheet-' + (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID().substring(0, 8) : Math.random().toString(36).substring(2, 8));
    const cleanWb = {
      id: id || ('wb-' + freshSheetId),
      title: 'Untitled Spreadsheet',
      visibility: 'PRIVATE',
      userRole: 'OWNER',
      sheets: [
        { id: freshSheetId, name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 },
      ],
      permissions: []
    };
    set({ workbook: cleanWb, activeSheetId: freshSheetId, loading: false });
    stompClient.subscribeWorkbook(cleanWb.id);
    return cleanWb;
  },

  userWorkbooks: [],

  fetchUserWorkbooks: async () => {
    try {
      const list = await workbookService.listWorkbooks();
      if (Array.isArray(list)) {
        set({ userWorkbooks: list });
      }
      return list;
    } catch (e) {
      console.warn('Failed to fetch user workbooks from server', e);
      return [];
    }
  },

  resetToDefault: () => {
    const freshSheetId = 'sheet-' + (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID().substring(0, 8) : Math.random().toString(36).substring(2, 8));
    const cleanWb = {
      id: null,
      title: 'Untitled spreadsheet',
      visibility: 'PRIVATE',
      userRole: 'OWNER',
      sheets: [
        { id: freshSheetId, name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 },
      ],
      permissions: []
    };
    set({ workbook: cleanWb, activeSheetId: freshSheetId, userWorkbooks: [], loading: false, error: null });
  },

  createWorkbook: async (title = 'Untitled Spreadsheet') => {
    set({ isSaving: true });
    try {
      const data = await workbookService.createWorkbook({
        title,
        visibility: 'LINK_SHARED',
        initialSheetName: 'Sheet1',
      });
      const activeSheetId = data.sheets?.[0]?.id || 'sheet-1';
      set({ workbook: data, activeSheetId, isSaving: false });
      stompClient.subscribeWorkbook(data.id);
      useCollabStore.getState().initCollab(activeSheetId);

      // Update URL with newly generated UUID
      const url = new URL(window.location);
      url.searchParams.set('wb', data.id);
      window.history.pushState({}, '', url);

      // Cache locally
      try {
        localStorage.setItem(`sheetforge_wb_${data.id}`, JSON.stringify(data));
      } catch (e) {}

      // Refresh list
      get().fetchUserWorkbooks();
      return data;
    } catch (err) {
      // Offline fallback: generate client UUID
      const clientWbId = 'wb-' + (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 10));
      const freshSheetId = 'sheet-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);
      const newWb = {
        id: clientWbId,
        title: title || 'Untitled Spreadsheet',
        visibility: 'PRIVATE',
        userRole: 'OWNER',
        sheets: [{ id: freshSheetId, name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 }],
        permissions: []
      };
      set({ workbook: newWb, activeSheetId: freshSheetId, isSaving: false });
      stompClient.subscribeWorkbook(newWb.id);
      const url = new URL(window.location);
      url.searchParams.set('wb', newWb.id);
      window.history.pushState({}, '', url);
      try {
        localStorage.setItem(`sheetforge_wb_${newWb.id}`, JSON.stringify(newWb));
      } catch (e) {}
      return newWb;
    }
  },

  deleteWorkbook: async (id) => {
    const isCurrentActive = get().workbook?.id === id;

    // Clean localStorage for this workbook & its sheets
    try {
      localStorage.removeItem(`sheetforge_wb_${id}`);
      const currentWb = get().workbook;
      if (currentWb?.sheets) {
        currentWb.sheets.forEach((s) => {
          try { localStorage.removeItem(`sheetforge_cells_${s.id}`); } catch (e) {}
        });
      }
    } catch (e) {}

    try {
      await workbookService.deleteWorkbook(id);
    } catch (err) {
      console.warn('Failed to delete workbook on server:', err);
    }

    set((state) => {
      const remainingWorkbooks = state.userWorkbooks.filter((w) => w.id !== id);

      if (isCurrentActive) {
        // Remove ?wb= parameter from URL
        try {
          const url = new URL(window.location);
          url.searchParams.delete('wb');
          url.searchParams.delete('workbookId');
          window.history.replaceState({}, '', url.pathname + (url.search ? url.search : ''));
        } catch (e) {}

        const cleanWb = {
          id: 'wb-default-1',
          title: 'Untitled Spreadsheet',
          visibility: 'PRIVATE',
          userRole: 'OWNER',
          sheets: [
            { id: 'sheet-1', name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 },
          ],
          permissions: []
        };

        return {
          userWorkbooks: remainingWorkbooks,
          workbook: cleanWb,
          activeSheetId: 'sheet-1',
        };
      }

      return {
        userWorkbooks: remainingWorkbooks,
      };
    });

    if (isCurrentActive) {
      // 1. Wipe the grid state completely so no stale cells remain
      try {
        useGridStore.getState().clearGrid();
      } catch (e) {}
      // 2. Disconnect existing collab session
      try {
        useCollabStore.getState().cleanupCollab();
      } catch (e) {}
    }
  },

  updateTitle: async (newTitle) => {
    const { workbook } = get();
    if (!workbook) return;
    const updated = { ...workbook, title: newTitle };
    set({ workbook: updated });

    try {
      localStorage.setItem(`sheetforge_wb_${workbook.id}`, JSON.stringify(updated));
      await workbookService.updateWorkbook(workbook.id, {
        title: newTitle,
        visibility: workbook.visibility,
      });
    } catch (err) {
      console.warn('Failed to persist title on server', err);
    }
  },

  addSheet: async (name) => {
    let { workbook, activeSheetId } = get();
    const prevSheetId = activeSheetId;
    if (!workbook) {
      workbook = DEFAULT_WORKBOOK;
    }

    // Save currently active sheet cells to cache before switching!
    if (activeSheetId) {
      useGridStore.getState().saveActiveSheetToCache(activeSheetId);
    }

    const currentSheets = workbook.sheets && workbook.sheets.length > 0
      ? workbook.sheets
      : [{ id: activeSheetId || DEFAULT_SHEET_ID, name: 'Sheet1', position: 0, rowCount: 100, colCount: 26 }];

    // Determine unique sheet name (Sheet1, Sheet2, Sheet3...)
    let counter = currentSheets.length + 1;
    let sheetName = name || `Sheet${counter}`;
    while (currentSheets.some((s) => s.name === sheetName)) {
      counter++;
      sheetName = `Sheet${counter}`;
    }

    const clientSheetId = 'sheet-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);
    let newSheet = {
      id: clientSheetId,
      workbookId: workbook.id,
      name: sheetName,
      position: currentSheets.length,
      rowCount: 100,
      colCount: 26,
    };

    // If workbook has a real UUID on server, persist sheet to database
    const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    if (UUID_REGEX.test(workbook.id)) {
      try {
        const serverSheet = await workbookService.createSheet(workbook.id, {
          name: sheetName,
          rowCount: 100,
          colCount: 26,
        });
        if (serverSheet && serverSheet.id) {
          newSheet = serverSheet;
        }
      } catch (err) {
        console.warn('Could not persist new sheet to server, keeping locally:', err);
      }
    }

    const updatedSheets = [...currentSheets.filter((s) => s.id !== newSheet.id), newSheet];
    const updatedWorkbook = { ...workbook, sheets: updatedSheets };

    set({
      workbook: updatedWorkbook,
      activeSheetId: newSheet.id,
    });

    try {
      localStorage.setItem(`sheetforge_wb_${workbook.id}`, JSON.stringify(updatedWorkbook));
    } catch (e) {}

    // Load clean blank grid for the newly added sheet
    try {
      await useGridStore.getState().loadSheetCells(newSheet.id, false);
    } catch (e) {}

    try {
      useCollabStore.getState().initCollab(newSheet.id);
    } catch (e) {}

    // Broadcast workbook structure to other collaborators
    try {
      stompClient.sendWorkbookUpdate(workbook.id, updatedSheets, prevSheetId);
    } catch (e) {}

    return newSheet;
  },

  deleteSheet: async (sheetId) => {
    const { workbook, activeSheetId } = get();
    if (!workbook || (workbook.sheets?.length || 0) <= 1) {
      useUIStore.getState().showToast('A workbook must contain at least one sheet.', 'warning');
      return;
    }

    const prevSheetId = activeSheetId;
    const isDeletingActive = activeSheetId === sheetId;
    const updatedSheets = workbook.sheets.filter((s) => s.id !== sheetId);
    let nextActiveId = activeSheetId;
    if (isDeletingActive) {
      const deletedIndex = workbook.sheets.findIndex((s) => s.id === sheetId);
      const fallbackIndex = Math.max(0, deletedIndex - 1);
      nextActiveId = updatedSheets[fallbackIndex]?.id || updatedSheets[0].id;
    }

    const updatedWorkbook = { ...workbook, sheets: updatedSheets };
    set({ workbook: updatedWorkbook, activeSheetId: nextActiveId });

    try {
      localStorage.setItem(`sheetforge_wb_${workbook.id}`, JSON.stringify(updatedWorkbook));
    } catch (e) {}

    // Clean up cache of the deleted sheet so it doesn't linger
    const gridStore = useGridStore.getState();
    const currentCache = { ...gridStore.sheetCache };
    delete currentCache[sheetId];
    gridStore.setSheetCache(currentCache);
    try {
      localStorage.removeItem(`sheetforge_cells_${sheetId}`);
    } catch (e) {}

    // CRITICAL: If the deleted sheet was active, load the fallback sheet's cells and re-init collab!
    if (isDeletingActive) {
      await gridStore.loadSheetCells(nextActiveId, false);
      useCollabStore.getState().initCollab(nextActiveId);
    }

    // Persist deletion to server DB
    try {
      await workbookService.deleteSheet(sheetId);
    } catch (err) {
      console.warn('Server sheet deletion log:', err);
    }

    // Broadcast to all peers
    stompClient.sendWorkbookUpdate(workbook.id, updatedSheets, prevSheetId);
  },

  renameSheet: async (sheetId, newName) => {
    const { workbook, activeSheetId } = get();
    if (!workbook || !newName?.trim()) return;

    const updatedSheets = workbook.sheets.map((s) => (s.id === sheetId ? { ...s, name: newName.trim() } : s));
    const updatedWorkbook = { ...workbook, sheets: updatedSheets };
    set({ workbook: updatedWorkbook });

    try {
      localStorage.setItem(`sheetforge_wb_${workbook.id}`, JSON.stringify(updatedWorkbook));
    } catch (e) {}

    stompClient.sendWorkbookUpdate(workbook.id, updatedSheets, activeSheetId);
  },

  handleRemoteWorkbookUpdate: (sheets) => {
    const { workbook, activeSheetId } = get();
    if (!Array.isArray(sheets) || sheets.length === 0) return;

    if (workbook?.sheets && JSON.stringify(workbook.sheets) === JSON.stringify(sheets)) {
      return;
    }

    console.log('[WorkbookStore] Applying remote workbook sheets update:', sheets);

    const currentWb = workbook || DEFAULT_WORKBOOK;
    const updated = { ...currentWb, sheets };

    // Check if the currently viewed sheet was deleted by remote peer
    const sheetStillExists = sheets.some((s) => s.id === activeSheetId);
    let nextActiveId = activeSheetId;

    if (!sheetStillExists) {
      nextActiveId = sheets[0].id;
      set({ workbook: updated, activeSheetId: nextActiveId });
      // Load fallback sheet cells and re-init collab
      useGridStore.getState().loadSheetCells(nextActiveId, false);
      useCollabStore.getState().initCollab(nextActiveId);
    } else {
      set({ workbook: updated });
    }

    try {
      localStorage.setItem(`sheetforge_wb_${updated.id}`, JSON.stringify(updated));
    } catch (e) {}
  },

  handleRemoteSheetLifecycle: (event) => {
    if (!event || !event.action) return;
    const { action, sheetId, name, workbookId } = event;
    const { workbook, activeSheetId } = get();

    // Ignore if meant for a different workbook (if we are currently in a real workbook)
    if (workbook?.id && workbookId && workbook.id !== workbookId && !workbook.id.startsWith('wb-')) {
      return;
    }

    console.log('[WorkbookStore] Real-time sheet lifecycle event received:', action, sheetId, name);

    const currentSheets = workbook?.sheets ? [...workbook.sheets] : [];

    if (action === 'SHEET_CREATED') {
      const exists = currentSheets.some((s) => s.id === sheetId);
      if (!exists && sheetId) {
        const newSheet = {
          id: sheetId,
          name: name || `Sheet${currentSheets.length + 1}`,
          position: currentSheets.length,
          rowCount: 100,
          colCount: 26,
        };
        const updatedSheets = [...currentSheets, newSheet];
        const updated = { ...workbook, sheets: updatedSheets };
        set({ workbook: updated });
        try {
          localStorage.setItem(`sheetforge_wb_${updated.id}`, JSON.stringify(updated));
        } catch (e) {}
      }
      // Silently fetch fresh workbook in background if possible to sync complete metadata
      if (workbookId && !workbookId.startsWith('wb-')) {
        workbookService.getWorkbook(workbookId).then((fullWb) => {
          if (fullWb && fullWb.sheets?.length > 0) {
            const currentWb = get().workbook;
            set({ workbook: { ...currentWb, ...fullWb } });
          }
        }).catch(() => {});
      }
    } else if (action === 'SHEET_DELETED') {
      const exists = currentSheets.some((s) => s.id === sheetId);
      if (exists) {
        const updatedSheets = currentSheets.filter((s) => s.id !== sheetId);
        let nextActiveId = activeSheetId;
        const isDeletingActive = activeSheetId === sheetId;

        if (isDeletingActive) {
          const deletedIndex = currentSheets.findIndex((s) => s.id === sheetId);
          const fallbackIndex = Math.max(0, deletedIndex - 1);
          nextActiveId = updatedSheets[fallbackIndex]?.id || updatedSheets[0]?.id || 'sheet-1';
        }

        const updated = { ...workbook, sheets: updatedSheets };
        if (isDeletingActive) {
          set({ workbook: updated, activeSheetId: nextActiveId });
          useGridStore.getState().loadSheetCells(nextActiveId, false);
          useCollabStore.getState().initCollab(nextActiveId);
        } else {
          set({ workbook: updated });
        }

        try {
          localStorage.setItem(`sheetforge_wb_${updated.id}`, JSON.stringify(updated));
        } catch (e) {}

        // Clean cache of deleted sheet
        const gridStore = useGridStore.getState();
        const currentCache = { ...gridStore.sheetCache };
        delete currentCache[sheetId];
        gridStore.setSheetCache(currentCache);
        try {
          localStorage.removeItem(`sheetforge_cells_${sheetId}`);
        } catch (e) {}
      }
    } else if (action === 'SHEET_UPDATED') {
      const updatedSheets = currentSheets.map((s) => (s.id === sheetId ? { ...s, name: name || s.name } : s));
      const updated = { ...workbook, sheets: updatedSheets };
      set({ workbook: updated });
      try {
        localStorage.setItem(`sheetforge_wb_${updated.id}`, JSON.stringify(updated));
      } catch (e) {}
    }
  },

  // Range Protection Management
  protectedRanges: [],

  loadProtectedRanges: async (sheetId) => {
    let cached = [];
    try {
      const raw = localStorage.getItem(`sheetforge_protect_${sheetId}`);
      if (raw) cached = JSON.parse(raw);
    } catch (e) {}
    if (cached.length > 0) {
      set({ protectedRanges: cached });
    }

    try {
      const data = await workbookService.getProtectedRanges(sheetId);
      if (data && Array.isArray(data)) {
        set({ protectedRanges: data });
        try {
          localStorage.setItem(`sheetforge_protect_${sheetId}`, JSON.stringify(data));
        } catch (e) {}
      }
    } catch (err) {}
  },

  addProtectedRange: async (sheetId, rangeData) => {
    let newRange = {
      id: 'prot-' + Date.now(),
      sheetId,
      ...rangeData,
    };

    try {
      const serverRange = await workbookService.createProtectedRange(sheetId, rangeData);
      if (serverRange && serverRange.id) newRange = serverRange;
    } catch (e) {}

    const updated = [...get().protectedRanges.filter((r) => r.id !== newRange.id), newRange];
    set({ protectedRanges: updated });

    try {
      localStorage.setItem(`sheetforge_protect_${sheetId}`, JSON.stringify(updated));
    } catch (e) {}

    stompClient.sendProtectRange(sheetId, { action: 'CREATED', range: newRange });
    return newRange;
  },

  deleteProtectedRange: async (sheetId, rangeId) => {
    const updated = get().protectedRanges.filter((r) => r.id !== rangeId);
    set({ protectedRanges: updated });

    try {
      localStorage.setItem(`sheetforge_protect_${sheetId}`, JSON.stringify(updated));
    } catch (e) {}

    stompClient.sendProtectRange(sheetId, { action: 'DELETED', rangeId });
  },

  handleRemoteProtectedRange: (msg) => {
    if (!msg) return;
    const { action, range, rangeId, sheetId } = msg;

    if (action === 'CREATED' && range) {
      const exists = get().protectedRanges.some((r) => r.id === range.id);
      if (!exists) {
        const updated = [...get().protectedRanges, range];
        set({ protectedRanges: updated });
        try {
          localStorage.setItem(`sheetforge_protect_${range.sheetId || sheetId}`, JSON.stringify(updated));
        } catch (e) {}
      }
    } else if (action === 'DELETED' && rangeId) {
      const updated = get().protectedRanges.filter((r) => r.id !== rangeId);
      set({ protectedRanges: updated });
      try {
        localStorage.setItem(`sheetforge_protect_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
    }
  },

  isCellProtected: (sheetId, row, col, userId, isOwnerOrRole) => {
    if (isOwnerOrRole === 'OWNER' || isOwnerOrRole === true) return false; // Owner can always edit
    const { protectedRanges } = get();
    for (const r of protectedRanges) {
      if (!r.sheetId || r.sheetId === sheetId) {
        if (row >= r.startRow && row <= r.endRow && col >= r.startCol && col <= r.endCol) {
          const allowed = r.allowedUserIds || [];
          if (allowed.length === 0) return true; // Only owner allowed
          if (userId && allowed.some((u) => u === userId || String(u).toLowerCase() === String(userId).toLowerCase())) {
            return false;
          }
          return true; // Protected from unauthorized editing
        }
      }
    }
    return false;
  }
}));

// Register remote workbook update listener on stompClient
stompClient.onWorkbookUpdate((sheets) => {
  useWorkbookStore.getState().handleRemoteWorkbookUpdate(sheets);
});
