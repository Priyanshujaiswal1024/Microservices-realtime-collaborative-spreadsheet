import { create } from 'zustand';
import { stompClient } from '../services/stompClient';
import { useAuthStore } from './useAuthStore';
import { useGridStore } from './useGridStore';
import { useCommentStore } from './useCommentStore';
import { mergeCellState } from '../utils/crdt';

export const useCollabStore = create((set, get) => ({
  isConnected: true,
  activeUsers: [],
  remoteCursors: {},
  unsubscribeFn: null,

  initCollab: (sheetId) => {
    if (!sheetId) return;

    const { unsubscribeFn } = get();
    if (unsubscribeFn) unsubscribeFn();

    const auth = useAuthStore.getState();
    const token = auth.token;
    const currentUser = auth.user;
    const userColor = auth.userColor;
    const userId = currentUser?.id || auth.clientId;

    const handleStatus = (status) => {
      set({ isConnected: status });
    };

    const cleanupStatus = stompClient.addStatusListener(handleStatus);

    stompClient.connect(token, () => {});

    // Send initial join
    stompClient.sendPresence(sheetId, 'JOIN', currentUser?.username || 'Collaborator', userColor, userId);

    // Fetch initial presence list immediately from collab-service Redis
    const fetchPresence = () => {
      fetch(`/api/v1/collab/sheet/${sheetId}/presence`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      })
        .then((res) => (res.ok ? res.json() : []))
        .then((users) => {
          if (Array.isArray(users)) {
            const currentId = useAuthStore.getState().user?.id || useAuthStore.getState().clientId;
            const remoteActive = users.filter((u) => u.userId && u.userId !== currentId);
            set((state) => {
              const map = new Map();
              state.activeUsers.forEach((u) => map.set(u.userId, u));
              remoteActive.forEach((u) => {
                map.set(u.userId, { ...map.get(u.userId), ...u, lastActiveEpoch: Date.now() });
              });
              return { activeUsers: Array.from(map.values()) };
            });
          }
        })
        .catch(() => {});
    };

    fetchPresence();
    const presenceTimer = setInterval(fetchPresence, 10000);

    // Subscribe to sheet cell updates, presence & comments
    const unsub = stompClient.subscribeSheet(sheetId, {
      onCellUpdate: (msg) => {
        if (!msg) return;

        if (
          msg.action === 'CLEAR_SHEET' ||
          msg.type === 'CLEAR_SHEET' ||
          msg.cellState?.action === 'CLEAR_SHEET' ||
          msg.cellState?.value === 'CLEAR_SHEET' ||
          (msg.row === -1 && msg.col === -1)
        ) {
          useGridStore.getState().handleRemoteClearSheet(msg.sheetId || sheetId);
          return;
        }

        // Handle atomic batch update (e.g. range fill, bulk format, paste)
        if (msg.type === 'CELL_BATCH_UPDATE' || Array.isArray(msg.updates)) {
          const updates = msg.updates || [];
          if (!updates.length) return;

          // Skip self loopback (if originating from this tab)
          const firstState = updates[0]?.cellState || updates[0];
          const myClientId = useAuthStore.getState().clientId;
          if (firstState?.clientId && myClientId && firstState.clientId === myClientId) {
            return;
          }

          useGridStore.getState().updateBatchCells(msg.sheetId || sheetId, updates, true);
          return;
        }

        if (msg.row === undefined || msg.col === undefined || !msg.cellState) return;

        // Skip self loopback (only check unique tab clientId)
        const myClientId = useAuthStore.getState().clientId;
        if (msg.cellState.clientId && myClientId && msg.cellState.clientId === myClientId) return;

        const gridStore = useGridStore.getState();
        const incomingVal = msg.cellState.value !== undefined ? msg.cellState.value : '';

        let incomingFormat = null;
        if (typeof msg.cellState.format === 'string') {
          try {
            incomingFormat = JSON.parse(msg.cellState.format);
          } catch (e) {
            incomingFormat = {};
          }
        } else if (msg.cellState.format && typeof msg.cellState.format === 'object') {
          incomingFormat = msg.cellState.format;
        }

        gridStore.updateCellValue(
          msg.row,
          msg.col,
          incomingVal,
          sheetId,
          true,
          msg.cellState.timestamp,
          incomingFormat
        );
      },

      onCommentUpdate: (msg) => {
        if (msg) {
          useCommentStore.getState().handleRemoteComment(msg);
        }
      },

      onPresenceUpdate: (msg) => {
        if (!msg) return;

        const currentId = useAuthStore.getState().user?.id || useAuthStore.getState().clientId;
        if (msg.userId === currentId) return; // Don't treat self as remote

        // Handle Presence JOIN / LEAVE
        if (msg.type === 'PRESENCE') {
          const { activeUsers } = get();
          if (msg.action === 'JOIN') {
            const existingIdx = activeUsers.findIndex((u) => u.userId === msg.userId);
            let updated;
            if (existingIdx >= 0) {
              updated = [...activeUsers];
              updated[existingIdx] = { ...updated[existingIdx], ...msg, lastActiveEpoch: Date.now() };
            } else {
              updated = [...activeUsers, { ...msg, lastActiveEpoch: Date.now() }];
            }
            set({ activeUsers: updated });
          } else if (msg.action === 'LEAVE') {
            set({
              activeUsers: activeUsers.filter((u) => u.userId !== msg.userId),
              remoteCursors: { ...get().remoteCursors, [msg.userId]: undefined },
            });
          }
        }

        // Handle Remote Cursor Move
        if (msg.type === 'CURSOR_MOVE' && msg.userId !== currentId) {
          const { remoteCursors } = get();
          set({
            remoteCursors: {
              ...remoteCursors,
              [msg.userId]: {
                userId: msg.userId,
                userName: msg.userName,
                color: msg.color,
                row: msg.row,
                col: msg.col,
              }
            }
          });
        }
      }
    });

    set({
      unsubscribeFn: () => {
        clearInterval(presenceTimer);
        stompClient.sendPresence(sheetId, 'LEAVE', currentUser?.username || 'Collaborator', userColor, userId);
        unsub();
        cleanupStatus();
      }
    });
  },

  broadcastCursorMove: (sheetId, row, col) => {
    // Throttle cursor moves to max ~15/sec to prevent network and thread congestion
    const now = Date.now();
    if (!useCollabStore._lastCursorTime) useCollabStore._lastCursorTime = 0;
    if (now - useCollabStore._lastCursorTime < 70) return;
    useCollabStore._lastCursorTime = now;

    const auth = useAuthStore.getState();
    const userId = auth.user?.id || auth.clientId;
    stompClient.sendCursorMove(
      sheetId,
      row,
      col,
      auth.user?.username || 'Collaborator',
      auth.userColor,
      userId
    );
  },

  cleanupCollab: () => {
    const { unsubscribeFn } = get();
    if (unsubscribeFn) unsubscribeFn();
    set({ isConnected: false, activeUsers: [], remoteCursors: {}, unsubscribeFn: null });
  }
}));
