import { create } from 'zustand';
import { auditService } from '../services/auditService';

export const useAuditStore = create((set, get) => ({
  historyEvents: [],
  snapshots: [],
  loading: false,
  isOpen: false,

  setIsOpen: (isOpen) => set({ isOpen }),

  loadHistory: async (workbookId) => {
    set({ loading: true });
    try {
      const res = await auditService.getWorkbookHistory(workbookId);
      set({ historyEvents: res.content || [], loading: false });
    } catch (e) {
      set({ loading: false });
    }
  },

  loadSnapshots: async (workbookId) => {
    try {
      const data = await auditService.getSnapshots(workbookId);
      set({ snapshots: data || [] });
    } catch (e) {
      // ignore
    }
  },

  createSnapshot: async (workbookId, label, description, snapshotData) => {
    try {
      const snap = await auditService.createSnapshot(workbookId, {
        label,
        description,
        snapshotData,
      });
      set({ snapshots: [snap, ...get().snapshots] });
      return snap;
    } catch (e) {
      const localSnap = {
        id: 'snap-' + Date.now(),
        workbookId,
        label,
        description,
        createdBy: 'You',
        createdAt: new Date().toISOString(),
        snapshotData,
      };
      set({ snapshots: [localSnap, ...get().snapshots] });
      return localSnap;
    }
  },

  restoreWorkbook: async (workbookId, snapshotId, targetTimestamp) => {
    try {
      const res = await auditService.restoreWorkbook(workbookId, {
        snapshotId,
        targetTimestamp,
      });
      return res;
    } catch (e) {
      console.error('Failed to restore snapshot', e);
      throw e;
    }
  }
}));
