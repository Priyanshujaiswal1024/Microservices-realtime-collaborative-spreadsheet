import React, { useState } from 'react';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useGridStore } from '../store/useGridStore';
import { useCollabStore } from '../store/useCollabStore';
import { useUIStore } from '../store/useUIStore';
import { Plus, ChevronDown, X } from 'lucide-react';

export default function SheetTabs() {
  const { workbook, activeSheetId, setActiveSheetId, addSheet, deleteSheet, renameSheet } = useWorkbookStore();
  const { loadSheetCells, saveActiveSheetToCache } = useGridStore();
  const { initCollab } = useCollabStore();
  const { showConfirm, showToast } = useUIStore();

  const [editingSheetId, setEditingSheetId] = useState(null);
  const [editName, setEditName] = useState('');

  const handleTabClick = (sheetId) => {
    if (sheetId === activeSheetId) return;
    if (activeSheetId) {
      saveActiveSheetToCache(activeSheetId);
    }
    setActiveSheetId(sheetId);
    loadSheetCells(sheetId, false);
    initCollab(sheetId);
  };

  const handleAddSheet = async () => {
    if (activeSheetId) {
      saveActiveSheetToCache(activeSheetId);
    }
    const newSheet = await addSheet();
    if (newSheet?.id) {
      setActiveSheetId(newSheet.id);
      loadSheetCells(newSheet.id, false);
      initCollab(newSheet.id);
    }
  };

  const handleDelete = (e, sheetId, sheetName) => {
    e.stopPropagation();
    const sheetsList = workbook?.sheets || [];
    if (sheetsList.length <= 1) {
      showToast('A workbook must contain at least one sheet.', 'warning');
      return;
    }
    showConfirm({
      title: 'Delete sheet?',
      message: `Delete sheet "${sheetName}"? Are you sure you want to delete this sheet? This cannot be undone.`,
      confirmText: 'OK',
      cancelText: 'Cancel',
      isDestructive: true,
      onConfirm: () => deleteSheet(sheetId),
    });
  };

  const startRenaming = (e, sheet) => {
    e.stopPropagation();
    setEditingSheetId(sheet.id);
    setEditName(sheet.name);
  };

  const commitRenaming = (sheetId) => {
    if (editName.trim()) {
      renameSheet(sheetId, editName.trim());
    }
    setEditingSheetId(null);
  };

  const sheets = workbook?.sheets && workbook.sheets.length > 0
    ? workbook.sheets
    : [{ id: activeSheetId || 'sheet-default', name: 'Sheet1', position: 0 }];

  return (
    <div className="google-sheet-tabs-bar">
      {/* Plus (+) Add Sheet Button */}
      <button
        onClick={handleAddSheet}
        title="Add Sheet"
        className="google-tabs-icon-btn"
      >
        <Plus size={16} color="#444746" strokeWidth={2.2} />
      </button>

      {/* Tabs Container */}
      <div className="google-tabs-scroll-area">
        {sheets.map((sheet) => {
          const isActive = sheet.id === activeSheetId;
          const isRenaming = editingSheetId === sheet.id;

          return (
            <div
              key={sheet.id}
              onClick={() => handleTabClick(sheet.id)}
              onDoubleClick={(e) => startRenaming(e, sheet)}
              className={`google-sheet-tab ${isActive ? 'active' : ''}`}
              title="Double click to rename"
            >
              {isRenaming ? (
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  onBlur={() => commitRenaming(sheet.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') commitRenaming(sheet.id);
                    if (e.key === 'Escape') setEditingSheetId(null);
                  }}
                  autoFocus
                  className="tab-rename-input"
                />
              ) : (
                <span className="tab-title-text">{sheet.name}</span>
              )}

              {/* Dropdown Chevron for Active Tab */}
              {isActive && !isRenaming && (
                <ChevronDown size={12} color="#1a73e8" className="tab-dropdown-icon" />
              )}

              {/* Delete Button (X) if multiple sheets */}
              {sheets.length > 1 && !isRenaming && (
                <button
                  onClick={(e) => handleDelete(e, sheet.id, sheet.name)}
                  title={`Delete ${sheet.name}`}
                  className="tab-close-btn"
                >
                  <X size={11} />
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
