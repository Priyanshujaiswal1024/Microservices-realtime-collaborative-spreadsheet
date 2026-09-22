import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useGridStore } from '../store/useGridStore';
import { parseA1Notation, toA1Notation } from '../utils/coordinate';
import { X, ShieldCheck, Lock, Trash2, Loader2 } from 'lucide-react';

export default function ProtectedRangeModal() {
  const { activeModal, closeModal, showToast } = useUIStore();
  const { activeSheetId, protectedRanges, addProtectedRange, deleteProtectedRange } = useWorkbookStore();
  const { selectedRange } = useGridStore();

  const [rangeStr, setRangeStr] = useState('A1:D10');
  const [description, setDescription] = useState('Protected Data');
  const [allowedUsers, setAllowedUsers] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (activeModal === 'protect' && selectedRange) {
      const sA1 = toA1Notation(selectedRange.startRow, selectedRange.startCol);
      const eA1 = toA1Notation(selectedRange.endRow, selectedRange.endCol);
      setRangeStr(sA1 === eA1 ? sA1 : `${sA1}:${eA1}`);
    }
  }, [activeModal, selectedRange]);

  if (activeModal !== 'protect') return null;

  const handleProtect = async (e) => {
    e.preventDefault();
    if (!rangeStr.trim() || !activeSheetId) {
      showToast('Please enter a valid range notation (e.g. A1:D10)', 'warning');
      return;
    }

    let startA1, endA1;
    if (rangeStr.includes(':')) {
      [startA1, endA1] = rangeStr.split(':');
    } else {
      startA1 = rangeStr;
      endA1 = rangeStr;
    }

    const start = parseA1Notation(startA1.trim());
    const end = parseA1Notation(endA1.trim());

    if (!start || !end) {
      showToast('Invalid cell coordinates in range', 'error');
      return;
    }

    setLoading(true);
    try {
      await addProtectedRange(activeSheetId, {
        startRow: Math.min(start.row, end.row),
        endRow: Math.max(start.row, end.row),
        startCol: Math.min(start.col, end.col),
        endCol: Math.max(start.col, end.col),
        description: description.trim() || 'Protected Range',
        allowedUserIds: allowedUsers ? allowedUsers.split(',').map((s) => s.trim()).filter(Boolean) : [],
      });
      closeModal();
    } catch (err) {
      closeModal();
    } finally {
      setLoading(false);
    }
  };

  const currentSheetRanges = protectedRanges.filter((r) => !r.sheetId || r.sheetId === activeSheetId);

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div
        className="google-clean-dialog"
        style={{ maxWidth: '480px' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="google-dialog-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                background: '#e8f0fe',
                color: '#1a73e8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <ShieldCheck size={20} />
            </div>
            <div>
              <h2 className="google-dialog-title">Protected sheets and ranges</h2>
              <p className="google-dialog-subtitle">
                Lock cells to prevent unauthorized collaborators from editing
              </p>
            </div>
          </div>
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleProtect} className="google-dialog-body" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div className="google-input-group" style={{ margin: 0 }}>
            <label className="google-input-label">Cell range (e.g. A1:D10)</label>
            <input
              type="text"
              required
              value={rangeStr}
              onChange={(e) => setRangeStr(e.target.value.toUpperCase())}
              className="google-text-input"
              style={{ fontFamily: 'monospace', fontWeight: '600', color: '#1a73e8' }}
            />
          </div>

          <div className="google-input-group" style={{ margin: 0 }}>
            <label className="google-input-label">Description / Reason</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Locked Final Revenue Numbers"
              className="google-text-input"
            />
          </div>

          <div className="google-input-group" style={{ margin: 0 }}>
            <label className="google-input-label">
              Allowed user IDs (comma-separated, leave blank for Owner only)
            </label>
            <input
              type="text"
              value={allowedUsers}
              onChange={(e) => setAllowedUsers(e.target.value)}
              placeholder="e.g. user@example.com or User ID"
              className="google-text-input"
            />
          </div>

          {currentSheetRanges.length > 0 && (
            <div style={{ marginTop: '4px' }}>
              <label style={{ fontSize: '11px', fontWeight: '600', color: '#747775', textTransform: 'uppercase', display: 'block', marginBottom: '6px' }}>
                Active protected ranges
              </label>
              <div style={{ maxHeight: '120px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {currentSheetRanges.map((r) => {
                  const s = toA1Notation(r.startRow, r.startCol);
                  const e = toA1Notation(r.endRow, r.endCol);
                  return (
                    <div
                      key={r.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 12px',
                        background: '#f8fafd',
                        border: '1px solid #dadce0',
                        borderRadius: '6px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Lock size={12} color="#0f9d58" />
                        <span style={{ fontSize: '12px', fontWeight: '600', color: '#0f9d58', fontFamily: 'monospace' }}>
                          {s === e ? s : `${s}:${e}`}
                        </span>
                        <span style={{ fontSize: '11px', color: '#5f6368' }}>({r.description || 'Protected'})</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => deleteProtectedRange(activeSheetId, r.id)}
                        title="Remove protection"
                        style={{ background: 'transparent', border: 'none', color: '#d93025', cursor: 'pointer', padding: '2px' }}
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Footer */}
          <div className="google-dialog-footer" style={{ marginTop: '10px' }}>
            <button
              type="button"
              onClick={closeModal}
              className="google-btn-text"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="google-btn-primary"
            >
              {loading ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Loader2 size={14} className="animate-spin" />
                  <span>Protecting...</span>
                </div>
              ) : (
                <span>Protect range</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
