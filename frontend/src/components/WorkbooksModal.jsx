import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useGridStore } from '../store/useGridStore';
import { useCollabStore } from '../store/useCollabStore';
import { useAuthStore } from '../store/useAuthStore';
import {
  X,
  Search,
  Plus,
  Clock,
  ExternalLink,
  Trash2,
  CheckCircle2,
  Loader2
} from 'lucide-react';

export default function WorkbooksModal() {
  const { activeModal, closeModal, showConfirm } = useUIStore();
  const {
    workbook,
    userWorkbooks,
    fetchUserWorkbooks,
    createWorkbook,
    deleteWorkbook,
    setWorkbook
  } = useWorkbookStore();
  const { loadSheetCells, clearGrid } = useGridStore();
  const { initCollab, disconnectCollab } = useCollabStore();
  const { user } = useAuthStore();

  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateInput, setShowCreateInput] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [creating, setCreating] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    if (activeModal === 'workbooks') {
      fetchUserWorkbooks();
    }
  }, [activeModal, fetchUserWorkbooks]);

  if (activeModal !== 'workbooks') return null;

  const handleCreateNew = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setCreating(true);
    try {
      clearGrid();
      const newWb = await createWorkbook(newTitle.trim());
      if (newWb?.id) {
        setShowCreateInput(false);
        setNewTitle('');
        const firstSheetId = newWb.sheets?.[0]?.id;
        if (firstSheetId) {
          await loadSheetCells(firstSheetId, true);
          initCollab(firstSheetId);
        }
        closeModal();
      }
    } finally {
      setCreating(false);
    }
  };

  const handleOpenWorkbook = async (wbId) => {
    if (workbook?.id === wbId) {
      closeModal();
      return;
    }

    const targetWb = userWorkbooks.find((w) => w.id === wbId);
    if (!targetWb) return;

    disconnectCollab();
    clearGrid();
    setWorkbook(targetWb);

    const url = new URL(window.location.href);
    url.searchParams.set('wb', wbId);
    window.history.replaceState({}, '', url.toString());

    const firstSheetId = targetWb.sheets?.[0]?.id;
    if (firstSheetId) {
      await loadSheetCells(firstSheetId, true);
      initCollab(firstSheetId);
    }

    closeModal();
  };

  const handleDeleteWorkbook = (e, wb) => {
    e.stopPropagation();
    showConfirm({
      title: 'Delete workbook?',
      message: `Are you sure you want to permanently delete "${wb.title || 'Untitled'}"? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      isDestructive: true,
      onConfirm: async () => {
        setDeletingId(wb.id);
        try {
          await deleteWorkbook(wb.id);
          if (workbook?.id === wb.id) {
            clearGrid();
            const url = new URL(window.location.href);
            url.searchParams.delete('wb');
            window.history.replaceState({}, '', url.toString());
          }
        } finally {
          setDeletingId(null);
        }
      },
    });
  };

  const filteredWorkbooks = userWorkbooks.filter((w) =>
    (w.title || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (w.id || '').toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div
        className="google-clean-dialog"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '620px' }}
      >
        {/* Header */}
        <div className="google-dialog-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <svg width="28" height="34" viewBox="0 0 32 38" fill="none">
              <path d="M21 0H3.5C1.57 0 0 1.57 0 3.5V34.5C0 36.43 1.57 38 3.5 38H28.5C30.43 38 32 36.43 32 34.5V11L21 0Z" fill="#0F9D58"/>
              <path d="M21 0V11H32L21 0Z" fill="#87CEAC"/>
              <rect x="7" y="16" width="18" height="15" rx="1.5" fill="white"/>
              <line x1="7" y1="21" x2="25" y2="21" stroke="#0F9D58" strokeWidth="1.5"/>
              <line x1="7" y1="26" x2="25" y2="26" stroke="#0F9D58" strokeWidth="1.5"/>
              <line x1="13.5" y1="16" x2="13.5" y2="31" stroke="#0F9D58" strokeWidth="1.5"/>
            </svg>
            <div>
              <h2 className="google-dialog-title">My Spreadsheets</h2>
              <p className="google-dialog-subtitle">Open previous spreadsheets or create a new one</p>
            </div>
          </div>
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>

        {/* Action Bar */}
        <div
          style={{
            padding: '12px 20px',
            background: '#f8fafd',
            borderBottom: '1px solid #e0e3e7',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}
        >
          <div style={{ position: 'relative', flex: 1 }}>
            <Search
              size={14}
              color="#5f6368"
              style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              type="text"
              placeholder="Search spreadsheets..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="google-text-input"
              style={{ paddingLeft: '32px', height: '34px', fontSize: '13px' }}
            />
          </div>

          <button
            onClick={() => setShowCreateInput(true)}
            className="google-btn-primary"
            style={{ padding: '0 16px', height: '34px', fontSize: '13px', whiteSpace: 'nowrap' }}
          >
            <Plus size={14} />
            <span>New Sheet</span>
          </button>
        </div>

        {/* Quick Create Prompt */}
        {showCreateInput && (
          <form
            onSubmit={handleCreateNew}
            style={{
              padding: '12px 20px',
              background: '#f0fdf4',
              borderBottom: '1px solid #bbf7d0',
              display: 'flex',
              gap: '8px'
            }}
          >
            <input
              type="text"
              placeholder="Enter spreadsheet name (e.g. Budget Model, Project Tracker)..."
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              autoFocus
              className="google-text-input"
              style={{ flex: 1, height: '34px', fontSize: '13px' }}
            />
            <button
              type="submit"
              disabled={creating}
              className="google-btn-primary"
              style={{ height: '34px', padding: '0 16px', fontSize: '13px' }}
            >
              {creating ? 'Creating...' : 'Create'}
            </button>
            <button
              type="button"
              onClick={() => setShowCreateInput(false)}
              className="google-btn-text"
              style={{ height: '34px', padding: '0 12px', fontSize: '13px' }}
            >
              Cancel
            </button>
          </form>
        )}

        {/* Workbooks List */}
        <div style={{ maxHeight: '320px', overflowY: 'auto', padding: '12px 20px' }}>
          {filteredWorkbooks.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '36px 0', color: '#747775' }}>
              <p style={{ fontSize: '14px', fontWeight: '500', color: '#1f1f1f', margin: 0 }}>No spreadsheets found</p>
              <p style={{ fontSize: '12px', marginTop: '4px' }}>
                Click <strong>"New Sheet"</strong> above to create a new spreadsheet.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {filteredWorkbooks.map((wb) => {
                const isCurrent = workbook?.id === wb.id;
                const formattedDate = wb.updatedAt
                  ? new Date(wb.updatedAt).toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })
                  : 'Recently active';

                return (
                  <div
                    key={wb.id}
                    onClick={() => handleOpenWorkbook(wb.id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      background: isCurrent ? '#f0fdf4' : '#ffffff',
                      border: isCurrent ? '1px solid #86efac' : '1px solid #dadce0',
                      borderRadius: '8px',
                      cursor: 'pointer',
                      transition: 'all 0.1s ease'
                    }}
                  >
                    {/* Left Details */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: 0 }}>
                      <svg width="20" height="24" viewBox="0 0 32 38" fill="none" style={{ flexShrink: 0 }}>
                        <path d="M21 0H3.5C1.57 0 0 1.57 0 3.5V34.5C0 36.43 1.57 38 3.5 38H28.5C30.43 38 32 36.43 32 34.5V11L21 0Z" fill="#0F9D58"/>
                        <path d="M21 0V11H32L21 0Z" fill="#87CEAC"/>
                        <rect x="7" y="16" width="18" height="15" rx="1.5" fill="white"/>
                      </svg>

                      <div style={{ minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span
                            style={{
                              fontSize: '13px',
                              fontWeight: '500',
                              color: '#1f1f1f',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis'
                            }}
                          >
                            {wb.title || 'Untitled spreadsheet'}
                          </span>
                          {isCurrent && (
                            <span
                              style={{
                                fontSize: '10px',
                                fontWeight: '500',
                                color: '#0f9d58',
                                background: '#e6f4ea',
                                padding: '1px 6px',
                                borderRadius: '4px'
                              }}
                            >
                              Current
                            </span>
                          )}
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '2px' }}>
                          <span style={{ fontSize: '11px', color: '#747775', display: 'flex', alignItems: 'center', gap: '3px' }}>
                            <Clock size={10} />
                            {formattedDate}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Right Actions */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenWorkbook(wb.id);
                        }}
                        className="google-btn-text"
                        style={{ padding: '4px 10px', fontSize: '12px' }}
                      >
                        Open
                      </button>

                      {wb.id !== 'wb-default-1' && (
                        <button
                          type="button"
                          onClick={(e) => handleDeleteWorkbook(e, wb)}
                          style={{
                            background: 'transparent',
                            border: 'none',
                            color: '#747775',
                            cursor: 'pointer',
                            padding: '6px',
                            borderRadius: '50%'
                          }}
                          onMouseEnter={(e) => (e.currentTarget.style.color = '#d93025')}
                          onMouseLeave={(e) => (e.currentTarget.style.color = '#747775')}
                          title="Delete spreadsheet"
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          className="google-dialog-footer"
          style={{
            padding: '12px 20px',
            borderTop: '1px solid #e0e3e7',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            margin: 0
          }}
        >
          <span style={{ fontSize: '12px', color: '#5f6368' }}>
            Logged in as <strong>{user?.username || 'User'}</strong>
          </span>
          <button onClick={closeModal} className="google-btn-text">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
