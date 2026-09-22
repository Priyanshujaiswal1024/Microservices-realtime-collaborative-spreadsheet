import React, { useState, useEffect, useRef } from 'react';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useCollabStore } from '../store/useCollabStore';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { useNotificationStore } from '../store/useNotificationStore';
import { useGridStore } from '../store/useGridStore';
import { workbookService } from '../services/workbookService';
import {
  History,
  MessageSquare,
  Bell,
  Download,
  Upload,
  Lock,
  ChevronDown,
  FolderOpen,
  Plus,
  Star,
  Cloud,
  Check,
  ShieldCheck,
  RotateCcw,
  RotateCw,
  User
} from 'lucide-react';
import { getAvatarColor, getUserInitial } from '../utils/avatar';

export default function Header() {
  const { workbook, updateTitle, isSaving, createWorkbook, resetToDefault, activeSheetId } = useWorkbookStore();
  const { isConnected, disconnectCollab } = useCollabStore();
  const { user, isAuthenticated } = useAuthStore();
  const { openModal, triggerSavePrompt, toggleGridEngine, gridEngine, showToast } = useUIStore();
  const { unreadCount } = useNotificationStore();
  const { undo, redo, clearGrid, clearSheet } = useGridStore();

  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [titleInput, setTitleInput] = useState(workbook?.title || 'Untitled spreadsheet');
  const [activeMenu, setActiveMenu] = useState(null); // 'file' | 'edit' | 'view' | 'insert' | 'data' | null
  const [isStarred, setIsStarred] = useState(false);
  const menuRef = useRef(null);

  // Sync titleInput whenever active workbook's title changes
  useEffect(() => {
    setTitleInput(workbook?.title || 'Untitled spreadsheet');
  }, [workbook?.title]);

  // Close menus on outside click
  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setActiveMenu(null);
      }
    };
    window.addEventListener('mousedown', handleOutsideClick);
    return () => window.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const isUnsaved = !workbook?.id
    || workbook.id === 'wb-default-1'
    || workbook.id.startsWith('wb-');

  const handleShareClick = () => {
    if (isUnsaved) {
      triggerSavePrompt('share');
    } else {
      openModal('share');
    }
  };

  const handleTitleSubmit = () => {
    setIsEditingTitle(false);
    if (titleInput.trim() && titleInput !== workbook?.title) {
      updateTitle(titleInput.trim());
    }
  };

  const handleExportXlsx = async () => {
    if (workbook?.id) {
      await workbookService.exportXlsx(workbook.id, workbook.title);
      setActiveMenu(null);
    }
  };

  const handleExportCsv = async () => {
    const activeSheetId = useWorkbookStore.getState().activeSheetId;
    if (activeSheetId) {
      await workbookService.exportCsv(activeSheetId, workbook?.title || 'sheet');
      setActiveMenu(null);
    }
  };

  const handleImportFile = async (e) => {
    const file = e.target.files?.[0];
    if (file) {
      try {
        const imported = await workbookService.importFile(file);
        useWorkbookStore.getState().setWorkbook(imported);
        setActiveMenu(null);
      } catch (err) {
        showToast('Failed to import file: ' + err.message, 'error');
      }
    }
  };

  const handleNewSpreadsheet = () => {
    setActiveMenu(null);
    disconnectCollab();
    clearGrid();
    resetToDefault();
    // Clean URL query parameters
    const url = new URL(window.location.href);
    url.searchParams.delete('wb');
    url.searchParams.delete('sheet');
    window.history.replaceState({}, '', url.pathname);
  };

  const userInitial = getUserInitial(user);
  const avatarColor = user?.colorHex || user?.color || getAvatarColor(user?.username || user?.fullName);

  return (
    <header className="app-header" ref={menuRef}>
      {/* Left: Google Sheets Green Icon + Title & Clean Real Menus */}
      <div className="header-left">
        {/* Authentic Google Sheets Green File Logo */}
        <div
          className="google-sheets-logo"
          onClick={() => openModal('workbooks')}
          title="Sheets Home — View all spreadsheets"
        >
          <svg width="32" height="38" viewBox="0 0 32 38" fill="none">
            <path d="M21 0H3.5C1.57 0 0 1.57 0 3.5V34.5C0 36.43 1.57 38 3.5 38H28.5C30.43 38 32 36.43 32 34.5V11L21 0Z" fill="#0F9D58"/>
            <path d="M21 0V11H32L21 0Z" fill="#87CEAC"/>
            <rect x="7" y="16" width="18" height="15" rx="1.5" fill="white"/>
            <line x1="7" y1="21" x2="25" y2="21" stroke="#0F9D58" strokeWidth="1.5"/>
            <line x1="7" y1="26" x2="25" y2="26" stroke="#0F9D58" strokeWidth="1.5"/>
            <line x1="13.5" y1="16" x2="13.5" y2="31" stroke="#0F9D58" strokeWidth="1.5"/>
          </svg>
        </div>

        {/* 2-Row Layout: Top = Title + Icons; Bottom = Real Active Menus */}
        <div className="header-meta-col">
          {/* Top Row: Title, Star, Cloud Sync */}
          <div className="header-title-row">
            {isEditingTitle ? (
              <input
                type="text"
                value={titleInput}
                onChange={(e) => setTitleInput(e.target.value)}
                onBlur={handleTitleSubmit}
                onKeyDown={(e) => e.key === 'Enter' && handleTitleSubmit()}
                autoFocus
                className="workbook-title-input"
              />
            ) : (
              <button
                onClick={() => {
                  setTitleInput(workbook?.title || 'Untitled spreadsheet');
                  setIsEditingTitle(true);
                }}
                className="workbook-title-btn"
                title="Rename"
              >
                {workbook?.title || 'Untitled spreadsheet'}
              </button>
            )}

            {/* Star Icon */}
            <button
              onClick={() => setIsStarred(!isStarred)}
              className={`header-icon-ghost ${isStarred ? 'starred' : ''}`}
              title={isStarred ? 'Starred' : 'Star spreadsheet'}
            >
              <Star size={15} fill={isStarred ? '#fbbc04' : 'none'} color={isStarred ? '#fbbc04' : '#5f6368'} />
            </button>

            {/* Cloud Sync Status Icon */}
            <div
              className="cloud-sync-status"
              title={isSaving ? 'Saving...' : isConnected ? 'Document status: Saved to Cloud' : 'Reconnecting...'}
            >
              {isSaving ? (
                <span className="sync-label">Saving...</span>
              ) : isConnected ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '3px', color: '#5f6368' }}>
                  <Cloud size={14} />
                  <Check size={10} style={{ marginLeft: '-9px', marginTop: '3px' }} />
                </div>
              ) : (
                <span className="sync-label offline">Offline</span>
              )}
            </div>
          </div>

          {/* Bottom Row: Real Functional Menus Only */}
          <div className="header-menu-bar">
            {/* File Menu */}
            <div className="menu-dropdown-container">
              <button
                onClick={() => setActiveMenu(activeMenu === 'file' ? null : 'file')}
                className={`menu-tab-btn ${activeMenu === 'file' ? 'active' : ''}`}
              >
                File
              </button>
              {activeMenu === 'file' && (
                <div className="google-menu-dropdown">
                  <button onClick={handleNewSpreadsheet} className="menu-dropdown-item">
                    <Plus size={14} color="#0f9d58" />
                    <span>New spreadsheet</span>
                  </button>
                  <button
                    onClick={() => {
                      setActiveMenu(null);
                      openModal('workbooks');
                    }}
                    className="menu-dropdown-item"
                  >
                    <FolderOpen size={14} color="#1a73e8" />
                    <span>My Spreadsheets</span>
                  </button>
                  <div className="menu-dropdown-divider" />
                  <button onClick={handleExportXlsx} className="menu-dropdown-item">
                    <Download size={14} color="#0f9d58" />
                    <span>Download Excel (.xlsx)</span>
                  </button>
                  <button onClick={handleExportCsv} className="menu-dropdown-item">
                    <Download size={14} color="#0284c7" />
                    <span>Download CSV (.csv)</span>
                  </button>
                  <div className="menu-dropdown-divider" />
                  <label className="menu-dropdown-item" style={{ cursor: 'pointer' }}>
                    <Upload size={14} color="#f59e0b" />
                    <span>Import File...</span>
                    <input
                      type="file"
                      accept=".xlsx,.xls,.csv"
                      onChange={handleImportFile}
                      style={{ display: 'none' }}
                    />
                  </label>
                </div>
              )}
            </div>

            {/* Edit Menu */}
            <div className="menu-dropdown-container">
              <button
                onClick={() => setActiveMenu(activeMenu === 'edit' ? null : 'edit')}
                className={`menu-tab-btn ${activeMenu === 'edit' ? 'active' : ''}`}
              >
                Edit
              </button>
              {activeMenu === 'edit' && (
                <div className="google-menu-dropdown">
                  <button onClick={() => { undo(); setActiveMenu(null); }} className="menu-dropdown-item">
                    <RotateCcw size={14} />
                    <span>Undo (Ctrl+Z)</span>
                  </button>
                  <button onClick={() => { redo(); setActiveMenu(null); }} className="menu-dropdown-item">
                    <RotateCw size={14} />
                    <span>Redo (Ctrl+Y)</span>
                  </button>
                  <div className="menu-dropdown-divider" />
                  <button onClick={() => { clearSheet(activeSheetId); setActiveMenu(null); }} className="menu-dropdown-item" style={{ color: '#d93025' }}>
                    <span>Clear all cells</span>
                  </button>
                </div>
              )}
            </div>

            {/* View Menu */}
            <div className="menu-dropdown-container">
              <button
                onClick={() => setActiveMenu(activeMenu === 'view' ? null : 'view')}
                className={`menu-tab-btn ${activeMenu === 'view' ? 'active' : ''}`}
              >
                View
              </button>
              {activeMenu === 'view' && (
                <div className="google-menu-dropdown">
                  <button
                    onClick={() => { toggleGridEngine(); setActiveMenu(null); }}
                    className="menu-dropdown-item"
                  >
                    <span>Grid Engine: {gridEngine === 'ag-grid' ? 'Switch to Classic' : 'Switch to AG-Grid'}</span>
                  </button>
                </div>
              )}
            </div>

            {/* Direct Real Operation Buttons */}
            <button
              onClick={() => openModal('workbooks')}
              className="menu-tab-btn"
              title="View and open previous spreadsheets"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                fontWeight: 600,
                color: '#1a73e8',
                backgroundColor: 'rgba(26, 115, 232, 0.08)',
                padding: '2px 8px',
                borderRadius: '4px'
              }}
            >
              <FolderOpen size={14} color="#1a73e8" />
              <span>My Files</span>
            </button>

            <button
              onClick={() => openModal('comments')}
              className="menu-tab-btn"
              title="Open comments sidebar"
            >
              Comments
            </button>

            <button
              onClick={() => openModal('protect')}
              className="menu-tab-btn"
              title="Protect sheets and ranges"
            >
              Protect Ranges
            </button>

            <button
              onClick={() => openModal('history')}
              className="menu-tab-btn"
              title="View version history & snapshots"
            >
              Version History
            </button>
          </div>
        </div>
      </div>

      {/* Right: History, Comments, Notifications, Share, Avatar (Real options only) */}
      <div className="header-right">
        {/* Version History Button */}
        <button
          onClick={() => openModal('history')}
          title="Version history"
          className="google-icon-btn"
        >
          <History size={18} />
        </button>

        {/* Open Comment History Button */}
        <button
          onClick={() => openModal('comments')}
          title="Comments"
          className="google-icon-btn"
        >
          <MessageSquare size={18} />
        </button>

        {/* Notifications Button */}
        <button
          onClick={() => openModal('notifications')}
          title="Notifications"
          className="google-icon-btn"
          style={{ position: 'relative' }}
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span className="badge-count" style={{ top: '4px', right: '4px' }}>
              {unreadCount}
            </span>
          )}
        </button>

        {/* Google Workspace Authentic Pill-Shaped Share Button */}
        <button
          onClick={handleShareClick}
          className="google-share-pill"
          title={isUnsaved ? 'Save spreadsheet first to share' : 'Share with people and groups'}
        >
          <Lock size={14} strokeWidth={2.2} />
          <span>Share</span>
          <ChevronDown size={14} strokeWidth={2} style={{ marginLeft: '-2px' }} />
        </button>

        {/* User Profile Avatar / Guest Avatar */}
        {isAuthenticated && user ? (
          <div
            onClick={() => openModal('auth')}
            className="google-profile-avatar"
            style={{ backgroundColor: avatarColor }}
            title={`Signed in as ${user?.fullName || user?.username} (${user?.email || ''}) — Click to switch account`}
          >
            {userInitial}
          </div>
        ) : (
          <div
            onClick={() => openModal('auth')}
            className="google-guest-avatar"
            title="Not signed in — Click to sign in"
          >
            <User size={18} color="#5f6368" />
          </div>
        )}
      </div>
    </header>
  );
}
