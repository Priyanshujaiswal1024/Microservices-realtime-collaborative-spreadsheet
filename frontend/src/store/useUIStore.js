import { create } from 'zustand';

export const useUIStore = create((set) => ({
  activeModal: null, // 'share' | 'history' | 'comments' | 'notifications' | 'auth' | 'protect' | 'workbooks' | 'savePrompt' | null
  activeCellCommentPos: null, // { row, col }
  gridEngine: 'classic', // 'classic' | 'ag-grid'
  pendingAction: null, // 'share' | 'exit' — what to do after saving

  openModal: (modalName) => set({ activeModal: modalName }),
  closeModal: () => set({ activeModal: null, activeCellCommentPos: null }),
  openCellComment: (row, col) => set({ activeModal: 'comments', activeCellCommentPos: { row, col } }),
  setGridEngine: (engine) => set({ gridEngine: engine }),
  toggleGridEngine: () => set((state) => ({ gridEngine: state.gridEngine === 'ag-grid' ? 'classic' : 'ag-grid' })),

  // Save-before-action workflow
  triggerSavePrompt: (pendingAction) => set({ activeModal: 'savePrompt', pendingAction }),
  clearPendingAction: () => set({ pendingAction: null }),

  // Custom Google-style Confirm Dialog (replaces browser's localhost:5173 confirm)
  confirmDialog: null, // { title, message, confirmText, cancelText, isDestructive, onConfirm, onCancel }
  showConfirm: ({ title, message, confirmText = 'OK', cancelText = 'Cancel', isDestructive = false, onConfirm, onCancel }) =>
    set({ confirmDialog: { title, message, confirmText, cancelText, isDestructive, onConfirm, onCancel } }),
  closeConfirm: () => set({ confirmDialog: null }),

  // Custom Google-style Toast / Snackbar (replaces browser's localhost:5173 alert)
  toast: null, // { message, type: 'info' | 'warning' | 'error' | 'success', id }
  showToast: (message, type = 'info') => {
    const id = Date.now();
    set({ toast: { message, type, id } });
    setTimeout(() => {
      set((state) => (state.toast?.id === id ? { toast: null } : {}));
    }, 4500);
  },
  closeToast: () => set({ toast: null }),
}));
