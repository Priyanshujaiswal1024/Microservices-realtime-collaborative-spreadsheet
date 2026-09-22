import React, { useEffect, useCallback, useState } from 'react';
import Header from './components/Header';
import Toolbar from './components/Toolbar';
import FormulaBar from './components/FormulaBar';
import SpreadsheetGrid from './components/SpreadsheetGrid';
import AGSpreadsheetGrid from './components/AGSpreadsheetGrid';
import SheetTabs from './components/SheetTabs';
import StatusBar from './components/StatusBar';
import ShareModal from './components/ShareModal';
import HistoryModal from './components/HistoryModal';
import CommentsDrawer from './components/CommentsDrawer';
import NotificationsDrawer from './components/NotificationsDrawer';
import AuthModal from './components/AuthModal';
import ProtectedRangeModal from './components/ProtectedRangeModal';
import WorkbooksModal from './components/WorkbooksModal';
import SavePromptModal from './components/SavePromptModal';
import { consumePendingAfterLogin } from './utils/pendingSave';
import SharedLinkLoginGate from './components/SharedLinkLoginGate';
import GoogleConfirmModal from './components/GoogleConfirmModal';
import GoogleToast from './components/GoogleToast';

import { stompClient } from './services/stompClient';
import { useAuthStore } from './store/useAuthStore';
import { useWorkbookStore } from './store/useWorkbookStore';
import { useGridStore } from './store/useGridStore';
import { useCollabStore } from './store/useCollabStore';
import { useUIStore } from './store/useUIStore';

export default function App() {
  // Global replacement for window.alert to prevent "localhost:5173 says" native popups
  useEffect(() => {
    window.alert = (msg) => {
      useUIStore.getState().showToast(String(msg || ''));
    };
  }, []);
  const { initAuth, isAuthenticated, user } = useAuthStore();
  const { fetchWorkbook, activeSheetId, workbook, fetchUserWorkbooks } = useWorkbookStore();
  const { loadSheetCells, cells, clearGrid } = useGridStore();
  const { initCollab, cleanupCollab } = useCollabStore();
  const { gridEngine, triggerSavePrompt } = useUIStore();

  // Gate state: show login screen when shared link opened without auth
  const [showLoginGate, setShowLoginGate] = useState(false);
  const [sharedWbTitle, setSharedWbTitle] = useState('');

  // Detect shared link on mount — before auth resolves
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const urlWbId = (params.get('wb') || params.get('workbookId') || '').trim();
    if (urlWbId) {
      // Check auth synchronously from storage
      const hasToken = !!(sessionStorage.getItem('access_token') || localStorage.getItem('access_token'));
      const isGuestDismissed = sessionStorage.getItem(`guest_dismissed_${urlWbId}`) === 'true' || sessionStorage.getItem('guest_mode') === 'true';

      if (!hasToken && !isGuestDismissed) {
        // Try to get workbook title for the gate screen
        setShowLoginGate(true);
        // Attempt to peek at workbook title (best-effort)
        try {
          const cached = localStorage.getItem(`sheetforge_wb_${urlWbId}`);
          if (cached) {
            const parsed = JSON.parse(cached);
            if (parsed?.title) setSharedWbTitle(parsed.title);
          }
        } catch (e) {}
      }
    }
  }, []);

  // Initialize Auth on mount
  useEffect(() => {
    initAuth();
  }, [initAuth]);

  // Tab close / navigate-away guard: warn user if workbook is unsaved and has data
  useEffect(() => {
    const isUnsaved = !workbook?.id
      || workbook.id === 'wb-default-1'
      || workbook.id.startsWith('wb-');

    const hasCellData = cells && Object.keys(cells).length > 0;

    const handleBeforeUnload = (e) => {
      if (isUnsaved && hasCellData) {
        // Native browser dialog (Chrome shows generic text regardless of returnValue)
        e.preventDefault();
        e.returnValue = 'You have unsaved data. Save before leaving?';
        return e.returnValue;
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [workbook, cells]);

  // On mount and auth changes: load URL workbook (or user's latest) and connect real-time collaboration
  useEffect(() => {
    let isCancelled = false;

    const initApp = async () => {
      // 1. Check URL for workbook ID & sheet ID
      const params = new URLSearchParams(window.location.search);
      const urlWbId = (params.get('wb') || params.get('workbookId') || '').trim();
      const urlSheetId = (params.get('sheet') || params.get('sheetId') || '').trim();

      // Priority 1: If URL specifies a workbook ID, ALWAYS load that workbook!
      if (urlWbId) {
        try {
          stompClient.subscribeWorkbook(urlWbId);
          const wb = await fetchWorkbook(urlWbId);
          if (isCancelled) return;
          if (wb && wb.sheets && wb.sheets.length > 0) {
            const targetSheet = (urlSheetId && wb.sheets.find((s) => s.id === urlSheetId)) || wb.sheets[0];
            const targetSheetId = targetSheet.id;
            await loadSheetCells(targetSheetId, true);
            if (isCancelled) return;
            initCollab(targetSheetId);
            return;
          }
        } catch (e) {
          console.warn('Failed to load workbook from URL parameter:', e);
        }
      }

      // Priority 2: If user just came back from login (triggered by SavePromptModal), auto-resume save
      if (isAuthenticated && !urlWbId) {
        const pending = consumePendingAfterLogin();
        if (pending.title) {
          try {
            const currentCells = useGridStore.getState().cells;
            const savedWb = await useWorkbookStore.getState().createWorkbook(pending.title);
            if (isCancelled) return;
            if (savedWb?.id && savedWb.sheets?.[0]?.id) {
              const sheetId = savedWb.sheets[0].id;
              if (currentCells) {
                Object.values(currentCells).forEach((c) => {
                  if (c && c.rawValue !== undefined && c.rawValue !== '') {
                    useGridStore.getState().updateCellValue(c.row, c.col, c.rawValue, sheetId);
                  }
                });
              }
              await loadSheetCells(sheetId, true);
              initCollab(sheetId);
              // If original intent was to share, open share modal
              if (pending.action === 'saveAndShare') {
                setTimeout(() => useUIStore.getState().openModal('share'), 300);
              }
              return;
            }
          } catch (e) {
            console.warn('Could not resume pending save after login:', e);
          }
        }
      }

      // Priority 3: If authenticated and no URL param, load file list in background but keep a clean new sheet!
      if (isAuthenticated && !urlWbId) {
        try {
          await fetchUserWorkbooks();
        } catch (e) {
          console.warn('Could not fetch user workbooks', e);
        }
      }

      // Priority 4: Default fallback (Root URL without ?wb= param)
      // Always start with a fresh, clean, blank spreadsheet!
      try {
        localStorage.removeItem('sheetforge_cells_sheet-1');
      } catch (e) {}
      useWorkbookStore.getState().resetToDefault();
      const freshSheetId = useWorkbookStore.getState().activeSheetId;
      useGridStore.getState().clearGrid(freshSheetId);
      await loadSheetCells(freshSheetId, false);
      initCollab(freshSheetId);
    };

    initApp();

    return () => {
      isCancelled = true;
      cleanupCollab();
    };
  }, [isAuthenticated]);
  // eslint-disable-next-line — intentionally only re-run on auth change


  // NOTE: sheet loading is handled explicitly by:
  //   - WorkbooksModal (open/create workbook)
  //   - SheetTabs (tab click)
  //   - The auth useEffect above (initial page load)
  // Do NOT add a useEffect on activeSheetId here — it causes stale cache to overwrite fresh data.


  // If gate is active and user just logged in → dismiss gate and let app load sheet
  useEffect(() => {
    if (showLoginGate && isAuthenticated) {
      setShowLoginGate(false);
    }
  }, [isAuthenticated, showLoginGate]);

  return (
    <div className="app-container">
      {/* Shared Link Login Gate — shown when unauthenticated user opens a shared link */}
      {showLoginGate && (
        <SharedLinkLoginGate
          workbookTitle={workbook?.title || sharedWbTitle}
          onContinueAsGuest={() => {
            try {
              const params = new URLSearchParams(window.location.search);
              const urlWbId = (params.get('wb') || params.get('workbookId') || '').trim();
              if (urlWbId) {
                sessionStorage.setItem(`guest_dismissed_${urlWbId}`, 'true');
              }
              sessionStorage.setItem('guest_mode', 'true');
            } catch (e) {}
            setShowLoginGate(false);
          }}
        />
      )}
      {/* Top Application Header */}
      <Header />

      {/* Formatting Ribbon Toolbar */}
      <Toolbar />

      {/* Formula Bar with Coordinate Box & fx Input */}
      <FormulaBar />

      {/* Main Grid Viewport (AG Grid Community or Classic Canvas) */}
      {gridEngine === 'ag-grid' ? <AGSpreadsheetGrid /> : <SpreadsheetGrid />}

      {/* Bottom Sheet Tabs */}
      <SheetTabs />

      {/* Status Bar */}
      <StatusBar />

      {/* Modals & Drawers */}
      <ShareModal />
      <HistoryModal />
      <CommentsDrawer />
      <NotificationsDrawer />
      <AuthModal />
      <ProtectedRangeModal />
      <WorkbooksModal />
      <SavePromptModal />

      {/* Global Google-styled Confirm & Toast Modals (Zero "localhost:5173 says" popups) */}
      <GoogleConfirmModal />
      <GoogleToast />
    </div>
  );
}
