import { create } from 'zustand';
import { commentService } from '../services/commentService';
import { stompClient } from '../services/stompClient';
import { useAuthStore } from './useAuthStore';

export const useCommentStore = create((set, get) => ({
  threads: [],
  activeThreadId: null,
  loading: false,
  isOpen: false,

  setIsOpen: (isOpen) => set({ isOpen }),
  setActiveThreadId: (id) => set({ activeThreadId: id }),

  loadComments: async (workbookId, sheetId) => {
    // Load local storage cache first
    let cached = [];
    try {
      const raw = localStorage.getItem(`sheetforge_comments_${sheetId}`);
      if (raw) cached = JSON.parse(raw);
    } catch (e) {}
    if (cached.length > 0) {
      set({ threads: cached });
    }

    set({ loading: true });
    try {
      let data;
      if (sheetId) {
        data = await commentService.getSheetComments(sheetId);
      } else if (workbookId) {
        data = await commentService.getWorkbookComments(workbookId);
      }
      if (data && Array.isArray(data) && data.length > 0) {
        set({ threads: data, loading: false });
        try {
          localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(data));
        } catch (e) {}
      } else {
        set({ loading: false });
      }
    } catch (e) {
      set({ loading: false });
    }
  },

  createComment: async (sheetId, { row, col, content, workbookId }) => {
    const auth = useAuthStore.getState();
    const authorName = auth.user?.username || 'Collaborator';
    const authorId = auth.user?.id || auth.clientId;

    let thread;
    try {
      thread = await commentService.createComment(sheetId, {
        workbookId,
        row,
        col,
        content,
      });
      thread = {
        ...thread,
        row: thread.row !== undefined ? thread.row : row,
        col: thread.col !== undefined ? thread.col : col,
        sheetId: thread.sheetId || sheetId,
        authorName: thread.authorName || authorName,
      };
    } catch (e) {
      // Local fallback
      thread = {
        id: 'thread-' + Date.now(),
        workbookId,
        sheetId,
        row,
        col,
        authorId,
        authorName,
        initialContent: content,
        resolved: false,
        createdAt: new Date().toISOString(),
        replies: [],
      };
    }

    const updated = [thread, ...get().threads.filter((t) => t.id !== thread.id)];
    set({ threads: updated });

    try {
      localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
    } catch (err) {}

    stompClient.sendComment(sheetId, { action: 'CREATED', thread, sheetId });
    return thread;
  },

  addReply: async (threadId, content, sheetId) => {
    const auth = useAuthStore.getState();
    const authorName = auth.user?.username || 'Collaborator';
    const authorId = auth.user?.id || auth.clientId;

    let reply;
    try {
      reply = await commentService.addReply(threadId, { content });
    } catch (e) {
      reply = {
        id: 'reply-' + Date.now(),
        threadId,
        authorId,
        authorName,
        content,
        createdAt: new Date().toISOString(),
      };
    }

    const updated = get().threads.map((t) => {
      if (t.id === threadId) {
        return { ...t, replies: [...(t.replies || []), reply] };
      }
      return t;
    });
    set({ threads: updated });

    if (sheetId) {
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
      stompClient.sendComment(sheetId, { action: 'REPLY', threadId, reply, sheetId });
    }
  },

  resolveThread: async (threadId, sheetId) => {
    try {
      await commentService.resolveThread(threadId);
    } catch (e) {}
    const updated = get().threads.map((t) => (t.id === threadId ? { ...t, resolved: true } : t));
    set({ threads: updated });
    if (sheetId) {
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
      stompClient.sendComment(sheetId, { action: 'RESOLVED', threadId, sheetId });
    }
  },

  deleteThread: async (threadId, sheetId) => {
    try {
      await commentService.deleteThread(threadId);
    } catch (e) {}
    const updated = get().threads.filter((t) => t.id !== threadId);
    set({ threads: updated });
    if (sheetId) {
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
      stompClient.sendComment(sheetId, { action: 'DELETED', threadId, sheetId });
    }
  },

  handleRemoteComment: (msg) => {
    if (!msg) return;
    const { action, thread, threadId, reply, sheetId } = msg;

    if (action === 'CREATED' && thread) {
      const exists = get().threads.some((t) => t.id === thread.id);
      if (!exists) {
        const normalized = {
          ...thread,
          row: Number(thread.row),
          col: Number(thread.col),
        };
        const updated = [normalized, ...get().threads];
        set({ threads: updated });
        try {
          localStorage.setItem(`sheetforge_comments_${thread.sheetId || sheetId}`, JSON.stringify(updated));
        } catch (e) {}
      }
    } else if (action === 'REPLY' && threadId && reply) {
      const updated = get().threads.map((t) => {
        if (t.id === threadId) {
          const replyExists = t.replies?.some((r) => r.id === reply.id);
          if (!replyExists) {
            return { ...t, replies: [...(t.replies || []), reply] };
          }
        }
        return t;
      });
      set({ threads: updated });
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
    } else if (action === 'RESOLVED' && threadId) {
      const updated = get().threads.map((t) => (t.id === threadId ? { ...t, resolved: true } : t));
      set({ threads: updated });
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
    } else if (action === 'DELETED' && threadId) {
      const updated = get().threads.filter((t) => t.id !== threadId);
      set({ threads: updated });
      try {
        localStorage.setItem(`sheetforge_comments_${sheetId}`, JSON.stringify(updated));
      } catch (e) {}
    }
  }
}));
