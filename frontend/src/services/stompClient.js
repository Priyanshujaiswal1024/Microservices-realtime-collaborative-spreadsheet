import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class StompCollaborationClient {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.statusListeners = new Set();
    this.workbookListeners = new Set();
    this.localBus = null;
    this.currentSheetId = null;
    this.currentWorkbookId = 'wb-default-1';
    this.pendingSubscription = null;
    this.outbox = [];

    // Initialize local bus and storage sync immediately
    if (typeof window !== 'undefined') {
      if (window.BroadcastChannel) {
        this.initLocalBus();
      }
      this.initStorageSync();
    }
  }

  onWorkbookUpdate(listener) {
    this.workbookListeners.add(listener);
    return () => this.workbookListeners.delete(listener);
  }

  notifyWorkbookUpdate(sheets) {
    if (!Array.isArray(sheets) || sheets.length === 0) return;
    this.workbookListeners.forEach((fn) => {
      try { fn(sheets); } catch (e) { console.error('Error in workbook listener:', e); }
    });
  }

  connect(token, onConnectCallback) {
    // Check if BroadcastChannel is available in browser for instant multi-tab sync
    if (typeof window !== 'undefined' && window.BroadcastChannel && !this.localBus) {
      this.initLocalBus();
    }

    if (this.client && this.connected) {
      if (onConnectCallback) onConnectCallback();
      return;
    }

    const socketUrl = '/ws';

    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: {
          Authorization: token ? `Bearer ${token}` : '',
        },
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
      });

      this.client.onConnect = (frame) => {
        console.log('[STOMP] Connected to collaboration broker');
        this.connected = true;
        this.subscriptions.clear();
        this.notifyStatus(true);

        // Auto-subscribe to pending sheet topics once socket is ready
        if (this.pendingSubscription) {
          const { sheetId, callbacks } = this.pendingSubscription;
          this._doStompSubscribe(sheetId, callbacks);
        }

        // Auto-subscribe to workbook topic if we have a workbook ID
        if (this.currentWorkbookId) {
          this.subscribeWorkbook(this.currentWorkbookId);
        }

        // Flush messages queued while socket was establishing
        if (this.outbox && this.outbox.length > 0) {
          const pending = [...this.outbox];
          this.outbox = [];
          pending.forEach(({ destination, body }) => {
            try {
              this.client.publish({ destination, body });
            } catch (e) {
              console.warn('[STOMP] Error flushing queued message:', e);
            }
          });
        }

        if (onConnectCallback) onConnectCallback();
      };

      this.client.onStompError = (frame) => {
        this.connected = false;
        // Even if server is offline, localBus keeps multi-window sync active
        this.notifyStatus(Boolean(this.localBus));
      };

      this.client.onWebSocketClose = () => {
        this.connected = false;
        this.notifyStatus(Boolean(this.localBus));
      };

      this.client.activate();
    } catch (err) {
      console.warn('STOMP connection failed, using local broadcast channel', err);
      this.notifyStatus(Boolean(this.localBus));
    }
  }

  initLocalBus() {
    try {
      this.localBus = new BroadcastChannel('sheetforge_sync_bus');
      this.localBus.onmessage = (event) => {
        this.dispatchCrossSync(event.data);
      };
      this.notifyStatus(true);
    } catch (e) {
      console.warn('BroadcastChannel not supported');
    }
  }

  initStorageSync() {
    if (typeof window === 'undefined' || this.storageListenerAttached) return;
    this.storageListenerAttached = true;
    window.addEventListener('storage', (event) => {
      if (event.key === 'sheetforge_cross_sync' && event.newValue) {
        try {
          const data = JSON.parse(event.newValue);
          this.dispatchCrossSync(data);
        } catch (e) {}
      }
    });
  }

  dispatchCrossSync(data) {
    if (!data) return;
    let obj = data;
    if (typeof obj === 'string') {
      try { obj = JSON.parse(obj); } catch (e) {}
    }
    const { sheetId: msgSheetId, type, payload } = obj;
    const sheetsList = obj.sheets || payload?.sheets || (Array.isArray(obj) ? obj : null);

    if ((type === 'WORKBOOK_UPDATE' || obj.type === 'WORKBOOK_UPDATE') && Array.isArray(sheetsList)) {
      this.notifyWorkbookUpdate(sheetsList);
      return;
    }

    if (type === 'SHEET_LIFECYCLE' || obj.type === 'SHEET_LIFECYCLE' || type?.startsWith('SHEET_')) {
      import('../store/useWorkbookStore').then(({ useWorkbookStore }) => {
        useWorkbookStore.getState().handleRemoteSheetLifecycle(payload || obj);
      });
      return;
    }

    if (type === 'PROTECT') {
      import('../store/useWorkbookStore').then(({ useWorkbookStore }) => {
        useWorkbookStore.getState().handleRemoteProtectedRange(payload || data);
      });
      return;
    }

    if (type === 'CLEAR_SHEET' || payload?.type === 'CLEAR_SHEET' || payload?.action === 'CLEAR_SHEET' || obj.action === 'CLEAR_SHEET') {
      const targetSheet = msgSheetId || payload?.sheetId || obj.sheetId;
      import('../store/useGridStore').then(({ useGridStore }) => {
        useGridStore.getState().handleRemoteClearSheet(targetSheet);
      });
      return;
    }

    const { onCellUpdate, onPresenceUpdate, onCommentUpdate } = this.currentCallbacks || {};

    if (this.currentSheetId && msgSheetId === this.currentSheetId) {
      if ((type === 'CELL_EDIT' || type === 'CELL_UPDATE' || type === 'CELL_BATCH_UPDATE') && onCellUpdate) {
        onCellUpdate(payload || obj);
      } else if ((type === 'PRESENCE' || type === 'CURSOR_MOVE') && onPresenceUpdate) {
        onPresenceUpdate(payload);
      } else if (type === 'COMMENT' && onCommentUpdate) {
        onCommentUpdate(payload);
      }
    } else if (msgSheetId) {
      // Background sheet update: keep cache warm in memory and localStorage
      if (type === 'CLEAR_SHEET' || payload?.action === 'CLEAR_SHEET') {
        import('../store/useGridStore').then(({ useGridStore }) => {
          useGridStore.getState().handleRemoteClearSheet(msgSheetId);
        });
      } else if (type === 'CELL_BATCH_UPDATE' && Array.isArray(payload?.updates)) {
        import('../store/useGridStore').then(({ useGridStore }) => {
          payload.updates.forEach((u) => {
            if (u && u.row !== undefined && u.col !== undefined && u.cellState) {
              useGridStore.getState().updateCachedSheetCell(msgSheetId, u.row, u.col, u.cellState);
            }
          });
        });
      } else if ((type === 'CELL_EDIT' || type === 'CELL_UPDATE') && payload?.cellState) {
        import('../store/useGridStore').then(({ useGridStore }) => {
          useGridStore.getState().updateCachedSheetCell(msgSheetId, payload.row, payload.col, payload.cellState);
        });
      }
    }
  }

  _doStompSubscribe(sheetId, callbacks) {
    if (!this.client || !this.connected) return;
    const { onCellUpdate, onPresenceUpdate, onCommentUpdate } = callbacks || {};

    const cellSubKey = `sheet_${sheetId}_cells`;
    const presenceSubKey = `sheet_${sheetId}_presence`;
    const commentSubKey = `sheet_${sheetId}_comments`;
    const protectSubKey = `sheet_${sheetId}_protect`;

    // Clean prior subscriptions
    this.unsubscribe(cellSubKey);
    this.unsubscribe(presenceSubKey);
    this.unsubscribe(commentSubKey);
    this.unsubscribe(protectSubKey);

    console.log(`[STOMP] Subscribing to /topic/sheet/${sheetId}/*`);

    const cellSub = this.client.subscribe(`/topic/sheet/${sheetId}/cells`, (msg) => {
      try {
        const payload = JSON.parse(msg.body);
        if (onCellUpdate) onCellUpdate(payload);
      } catch (e) {
        console.error('Error parsing cell update message', e);
      }
    });
    this.subscriptions.set(cellSubKey, cellSub);

    const presenceSub = this.client.subscribe(`/topic/sheet/${sheetId}/presence`, (msg) => {
      try {
        const payload = JSON.parse(msg.body);
        if (onPresenceUpdate) onPresenceUpdate(payload);
      } catch (e) {
        console.error('Error parsing presence message', e);
      }
    });
    this.subscriptions.set(presenceSubKey, presenceSub);

    const commentSub = this.client.subscribe(`/topic/sheet/${sheetId}/comments`, (msg) => {
      try {
        let payload = JSON.parse(msg.body);
        if (typeof payload === 'string') {
          try { payload = JSON.parse(payload); } catch (e) {}
        }
        if (payload?.type === 'WORKBOOK_UPDATE') {
          const sheetsList = payload?.sheets || (Array.isArray(payload) ? payload : null);
          if (sheetsList) {
            this.notifyWorkbookUpdate(sheetsList);
          }
          return;
        }
        if (onCommentUpdate) onCommentUpdate(payload);
      } catch (e) {
        console.error('Error parsing comment message', e);
      }
    });
    this.subscriptions.set(commentSubKey, commentSub);

    const protectSub = this.client.subscribe(`/topic/sheet/${sheetId}/protected-ranges`, (msg) => {
      try {
        const payload = JSON.parse(msg.body);
        import('../store/useWorkbookStore').then(({ useWorkbookStore }) => {
          useWorkbookStore.getState().handleRemoteProtectedRange(payload);
        });
      } catch (e) {
        console.error('Error parsing protect message', e);
      }
    });
    this.subscriptions.set(protectSubKey, protectSub);

    // Ensure active workbook topic is subscribed
    this.subscribeWorkbook(this.currentWorkbookId || 'wb-default-1');
  }

  subscribeWorkbook(workbookId) {
    if (!workbookId) return;
    this.currentWorkbookId = workbookId;

    if (!this.client || !this.connected) return;

    const wbSubKey = `workbook_${workbookId}_update`;
    const sheetsSubKey = `workbook_${workbookId}_sheets`;
    this.unsubscribe(wbSubKey);
    this.unsubscribe(sheetsSubKey);

    const handleMsg = (msg) => {
      try {
        let payload = JSON.parse(msg.body);
        if (typeof payload === 'string') {
          try { payload = JSON.parse(payload); } catch (e) {}
        }
        const sheetsList = payload?.sheets || (Array.isArray(payload) ? payload : null);
        if (sheetsList) {
          this.notifyWorkbookUpdate(sheetsList);
          return;
        }

        if (payload?.type === 'SHEET_LIFECYCLE' || payload?.action?.startsWith('SHEET_')) {
          import('../store/useWorkbookStore').then(({ useWorkbookStore }) => {
            useWorkbookStore.getState().handleRemoteSheetLifecycle(payload);
          });
          return;
        }

        if (payload?.type === 'CLEAR_SHEET' || payload?.action === 'CLEAR_SHEET') {
          import('../store/useGridStore').then(({ useGridStore }) => {
            useGridStore.getState().handleRemoteClearSheet(payload.sheetId);
          });
          return;
        }
      } catch (e) {
        console.error('Error parsing workbook update message', e);
      }
    };

    console.log(`[STOMP] Subscribing to /topic/workbook/${workbookId}/update & /sheets`);
    try {
      const wbSub = this.client.subscribe(`/topic/workbook/${workbookId}/update`, handleMsg);
      this.subscriptions.set(wbSubKey, wbSub);
    } catch (e) {
      console.warn('[STOMP] Failed to subscribe to workbook update topic:', e);
    }

    try {
      const sheetsSub = this.client.subscribe(`/topic/workbook/${workbookId}/sheets`, handleMsg);
      this.subscriptions.set(sheetsSubKey, sheetsSub);
    } catch (e) {
      console.warn('[STOMP] Failed to subscribe to workbook sheets topic:', e);
    }

    // Also subscribe to global workbook channel for all sheets sync
    const globalKey = 'workbook_global_update';
    if (workbookId !== 'global') {
      this.unsubscribe(globalKey);
      try {
        const globalSub = this.client.subscribe('/topic/workbook/global/update', handleMsg);
        this.subscriptions.set(globalKey, globalSub);
      } catch (e) {
        console.warn('[STOMP] Failed to subscribe to global workbook topic:', e);
      }
    }
  }

  _publishOrQueue(destination, body) {
    if (this.client && this.connected) {
      try {
        this.client.publish({ destination, body });
      } catch (e) {
        console.warn('[STOMP] Publish failed:', e);
      }
    } else {
      if (!this.outbox) this.outbox = [];
      this.outbox.push({ destination, body });
    }
  }

  subscribeSheet(sheetId, callbacks) {
    this.currentSheetId = sheetId;
    this.currentCallbacks = callbacks;
    this.pendingSubscription = { sheetId, callbacks };
    this.initStorageSync();

    const cellSubKey = `sheet_${sheetId}_cells`;
    const presenceSubKey = `sheet_${sheetId}_presence`;
    const commentSubKey = `sheet_${sheetId}_comments`;
    const protectSubKey = `sheet_${sheetId}_protect`;

    // 1. WebSocket STOMP subscription (if backend is already connected)
    if (this.client && this.connected) {
      this._doStompSubscribe(sheetId, callbacks);
    }

    return () => {
      this.unsubscribe(cellSubKey);
      this.unsubscribe(presenceSubKey);
      this.unsubscribe(commentSubKey);
      this.unsubscribe(protectSubKey);
    };
  }

  sendCellEdit(sheetId, row, col, cellState) {
    const payload = {
      type: 'CELL_UPDATE',
      sheetId,
      row,
      col,
      userId: cellState?.userId || 'guest',
      cellState
    };

    // Broadcast across browser windows via BroadcastChannel
    if (this.localBus) {
      this.localBus.postMessage({
        sheetId,
        type: 'CELL_UPDATE',
        payload,
      });
    }

    // Broadcast via localStorage storage event for cross-profile / incognito
    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        sheetId,
        type: 'CELL_UPDATE',
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    // Send to Spring Boot backend (or queue if connecting)
    this._publishOrQueue(`/app/sheet/${sheetId}/edit`, JSON.stringify(payload));
  }

  sendBatchCellEdit(sheetId, updates, userId = 'guest') {
    if (!updates || !updates.length) return;
    const payload = {
      type: 'CELL_BATCH_UPDATE',
      sheetId,
      userId,
      updates
    };

    // Broadcast across browser windows via BroadcastChannel
    if (this.localBus) {
      this.localBus.postMessage({
        sheetId,
        type: 'CELL_BATCH_UPDATE',
        payload,
      });
    }

    // Broadcast via localStorage storage event for cross-profile / incognito
    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        sheetId,
        type: 'CELL_BATCH_UPDATE',
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    // Send to Spring Boot backend (or queue if connecting)
    this._publishOrQueue(`/app/sheet/${sheetId}/edit/batch`, JSON.stringify(payload));
  }

  sendClearSheet(sheetId) {
    const payload = {
      type: 'CLEAR_SHEET',
      sheetId,
      action: 'CLEAR_SHEET',
      userId: 'guest',
      timestamp: Date.now()
    };

    if (this.localBus) {
      try {
        this.localBus.postMessage({
          sheetId,
          type: 'CLEAR_SHEET',
          payload,
        });
      } catch (e) {}
    }

    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        sheetId,
        type: 'CLEAR_SHEET',
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    this._publishOrQueue(`/app/sheet/${sheetId}/edit`, JSON.stringify({
      type: 'CELL_UPDATE',
      sheetId,
      row: -1,
      col: -1,
      userId: 'guest',
      cellState: {
        value: 'CLEAR_SHEET',
        dataType: 'ACTION',
        timestamp: { physicalTime: Date.now(), logicalCounter: 0, clientId: 'client-local' },
        clientId: 'client-local',
        userId: 'guest',
        format: '{}'
      }
    }));

    // Also broadcast to workbook and sheet channels so all tabs receive it
    const wbId = this.currentWorkbookId || 'wb-default-1';
    this._publishOrQueue(`/app/workbook/${wbId}/update`, JSON.stringify(payload));
    this._publishOrQueue(`/app/sheet/${sheetId}/comment`, JSON.stringify(payload));
  }

  sendCursorMove(sheetId, row, col, userName, color, userId) {
    const payload = {
      type: 'CURSOR_MOVE',
      sheetId,
      row,
      col,
      userName,
      color,
      userId,
    };

    // Broadcast across browser windows via in-memory BroadcastChannel
    if (this.localBus) {
      this.localBus.postMessage({
        sheetId,
        type: 'CURSOR_MOVE',
        payload,
      });
    }

    // Send to Spring Boot backend (or queue if connecting)
    this._publishOrQueue(`/app/sheet/${sheetId}/cursor`, JSON.stringify(payload));
  }

  sendPresence(sheetId, action, userName, color, userId) {
    const payload = {
      type: 'PRESENCE',
      sheetId,
      action,
      userName,
      color,
      userId,
    };

    // Broadcast across browser windows via in-memory BroadcastChannel
    if (this.localBus) {
      this.localBus.postMessage({
        sheetId,
        type: 'PRESENCE',
        payload,
      });
    }

    // Send to Spring Boot backend (or queue if connecting)
    this._publishOrQueue(`/app/sheet/${sheetId}/presence`, JSON.stringify(payload));
  }

  sendComment(sheetId, commentPayload) {
    const payload = {
      type: 'COMMENT',
      sheetId,
      ...commentPayload,
    };

    // Broadcast across browser windows
    if (this.localBus) {
      try {
        this.localBus.postMessage({
          sheetId,
          type: 'COMMENT',
          payload,
        });
      } catch (e) {}
    }

    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        sheetId,
        type: 'COMMENT',
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    // Send to backend (queued if connecting)
    this._publishOrQueue(`/app/sheet/${sheetId}/comment`, JSON.stringify(payload));
  }

  sendWorkbookUpdate(workbookId, sheets, previousSheetId) {
    this.currentWorkbookId = workbookId;
    this.subscribeWorkbook(workbookId);

    const payload = {
      type: 'WORKBOOK_UPDATE',
      workbookId,
      sheets,
    };

    if (this.localBus) {
      try {
        this.localBus.postMessage({
          type: 'WORKBOOK_UPDATE',
          workbookId,
          sheets,
          payload,
        });
      } catch (e) {}
    }

    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        type: 'WORKBOOK_UPDATE',
        workbookId,
        sheets,
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    // 1. Primary workbook destination
    this._publishOrQueue(`/app/workbook/${workbookId}/update`, JSON.stringify(payload));

    // 2. Global workbook destination
    this._publishOrQueue('/app/workbook/global/update', JSON.stringify(payload));

    // 3. Current active sheet comment channel (guarantees delivery to all collaborators on current sheet)
    if (this.currentSheetId) {
      this._publishOrQueue(`/app/sheet/${this.currentSheetId}/comment`, JSON.stringify(payload));
    }

    // 4. Previous active sheet comment channel (ensures peers who haven't switched get it immediately)
    if (previousSheetId && previousSheetId !== this.currentSheetId) {
      this._publishOrQueue(`/app/sheet/${previousSheetId}/comment`, JSON.stringify(payload));
    }

    // 5. Also broadcast to all sheets in list so collaborators on ANY sheet tab receive it
    if (Array.isArray(sheets)) {
      sheets.forEach((s) => {
        if (s?.id && s.id !== this.currentSheetId && s.id !== previousSheetId) {
          this._publishOrQueue(`/app/sheet/${s.id}/comment`, JSON.stringify(payload));
        }
      });
    }
  }

  sendProtectRange(sheetId, protectPayload) {
    const payload = {
      type: 'PROTECT',
      sheetId,
      ...protectPayload,
    };

    if (this.localBus) {
      this.localBus.postMessage({
        sheetId,
        type: 'PROTECT',
        payload,
      });
    }

    try {
      localStorage.setItem('sheetforge_cross_sync', JSON.stringify({
        sheetId,
        type: 'PROTECT',
        payload,
        _ts: Date.now() + Math.random()
      }));
    } catch (e) {}

    if (this.client && this.connected) {
      this.client.publish({
        destination: `/app/sheet/${sheetId}/protect`,
        body: JSON.stringify(payload),
      });
    }
  }

  unsubscribe(key) {
    if (this.subscriptions.has(key)) {
      try {
        this.subscriptions.get(key).unsubscribe();
      } catch (e) {
        // ignore
      }
      this.subscriptions.delete(key);
    }
  }

  addStatusListener(listener) {
    this.statusListeners.add(listener);
    listener(this.connected || Boolean(this.localBus));
    return () => this.statusListeners.delete(listener);
  }

  notifyStatus(status) {
    this.statusListeners.forEach((fn) => fn(status));
  }

  disconnect() {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connected = false;
    this.notifyStatus(false);
  }
}

export const stompClient = new StompCollaborationClient();
