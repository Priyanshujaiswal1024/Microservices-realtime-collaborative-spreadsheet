import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useAuditStore } from '../store/useAuditStore';
import { useGridStore } from '../store/useGridStore';
import { X, History, BookmarkPlus, RotateCcw, Clock, CheckCircle2 } from 'lucide-react';

export default function HistoryModal() {
  const { activeModal, closeModal, showConfirm, showToast } = useUIStore();
  const { workbook } = useWorkbookStore();
  const { cells } = useGridStore();
  const {
    historyEvents,
    snapshots,
    loadHistory,
    loadSnapshots,
    createSnapshot,
    restoreWorkbook,
  } = useAuditStore();

  const [activeTab, setActiveTab] = useState('snapshots');
  const [snapshotLabel, setSnapshotLabel] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [restoreSuccess, setRestoreSuccess] = useState(null);

  useEffect(() => {
    if (activeModal === 'history' && workbook?.id) {
      loadHistory(workbook.id);
      loadSnapshots(workbook.id);
    }
  }, [activeModal, workbook?.id, loadHistory, loadSnapshots]);

  if (activeModal !== 'history') return null;

  const handleCreateSnapshot = async (e) => {
    e.preventDefault();
    if (!snapshotLabel.trim() || !workbook?.id) return;

    setIsCreating(true);
    try {
      const snapshotData = JSON.stringify(cells);
      await createSnapshot(workbook.id, snapshotLabel.trim(), '', snapshotData);
      setSnapshotLabel('');
      setRestoreSuccess('Snapshot checkpoint created successfully!');
      setTimeout(() => setRestoreSuccess(null), 3000);
    } finally {
      setIsCreating(false);
    }
  };

  const handleRestore = (snapshot) => {
    showConfirm({
      title: 'Restore version?',
      message: `Restore spreadsheet to checkpoint "${snapshot.label}"? Your current changes will be overwritten by this checkpoint.`,
      confirmText: 'Restore',
      cancelText: 'Cancel',
      isDestructive: false,
      onConfirm: async () => {
        try {
          if (snapshot.snapshotData) {
            const parsed = JSON.parse(snapshot.snapshotData);
            useGridStore.setState({ cells: parsed });
          }
          await restoreWorkbook(workbook.id, snapshot.id);
          setRestoreSuccess(`Restored to checkpoint "${snapshot.label}"!`);
          setTimeout(() => setRestoreSuccess(null), 3000);
          showToast(`Restored to checkpoint "${snapshot.label}"!`, 'success');
        } catch (e) {
          showToast('Error restoring snapshot: ' + e.message, 'error');
        }
      },
    });
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div
        className="google-clean-dialog"
        style={{ maxWidth: '540px' }}
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
              <History size={20} />
            </div>
            <div>
              <h2 className="google-dialog-title">Version history</h2>
              <p className="google-dialog-subtitle">
                View past checkpoints and restore previous spreadsheet versions
              </p>
            </div>
          </div>
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>

        <div className="google-dialog-body" style={{ paddingTop: '16px' }}>
          {/* Tabs */}
          <div className="google-tab-segmented" style={{ marginBottom: '16px' }}>
            <button
              onClick={() => setActiveTab('snapshots')}
              className={`google-segment-btn ${activeTab === 'snapshots' ? 'active' : ''}`}
            >
              Checkpoints ({snapshots.length})
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`google-segment-btn ${activeTab === 'history' ? 'active' : ''}`}
            >
              Change Feed ({historyEvents.length})
            </button>
          </div>

          {/* Success Banner */}
          {restoreSuccess && (
            <div
              style={{
                padding: '8px 12px',
                background: '#e6f4ea',
                border: '1px solid #ceead6',
                borderRadius: '6px',
                color: '#137333',
                fontSize: '13px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                marginBottom: '14px',
              }}
            >
              <CheckCircle2 size={16} />
              <span>{restoreSuccess}</span>
            </div>
          )}

          {activeTab === 'snapshots' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {/* Create Checkpoint Box */}
              <form
                onSubmit={handleCreateSnapshot}
                style={{
                  padding: '12px 14px',
                  background: '#f8fafd',
                  border: '1px solid #dadce0',
                  borderRadius: '8px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: '500', color: '#1f1f1f' }}>
                  <BookmarkPlus size={14} color="#1a73e8" />
                  <span>Name current version</span>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="text"
                    placeholder="Checkpoint name (e.g. End of Sprint 1)"
                    value={snapshotLabel}
                    onChange={(e) => setSnapshotLabel(e.target.value)}
                    required
                    className="google-text-input"
                    style={{ flex: 1, height: '36px', fontSize: '13px' }}
                  />
                  <button
                    type="submit"
                    disabled={isCreating}
                    className="google-btn-primary"
                    style={{ padding: '0 16px', height: '36px', fontSize: '13px' }}
                  >
                    {isCreating ? 'Saving...' : 'Save'}
                  </button>
                </div>
              </form>

              {/* Snapshots List */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '240px', overflowY: 'auto' }}>
                {snapshots.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '28px 0', color: '#747775', fontSize: '13px' }}>
                    <History size={28} color="#dadce0" style={{ margin: '0 auto 8px' }} />
                    <p style={{ margin: 0, fontWeight: '500', color: '#444746' }}>No checkpoints saved yet</p>
                    <p style={{ margin: '4px 0 0', fontSize: '12px' }}>
                      Name the current version above to create a restore point.
                    </p>
                  </div>
                ) : (
                  snapshots.map((s) => (
                    <div
                      key={s.id}
                      style={{
                        padding: '10px 14px',
                        background: '#ffffff',
                        border: '1px solid #dadce0',
                        borderRadius: '8px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span style={{ fontSize: '13px', fontWeight: '500', color: '#1f1f1f' }}>
                            {s.label}
                          </span>
                          <span style={{ fontSize: '10px', color: '#5f6368', background: '#f0f4f9', padding: '1px 6px', borderRadius: '4px' }}>
                            by {s.createdBy || 'User'}
                          </span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: '#747775', marginTop: '2px' }}>
                          <Clock size={11} />
                          <span>{new Date(s.createdAt).toLocaleString()}</span>
                        </div>
                      </div>

                      <button
                        onClick={() => handleRestore(s)}
                        className="google-btn-text"
                        style={{ padding: '4px 12px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}
                      >
                        <RotateCcw size={12} />
                        <span>Restore</span>
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'history' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '300px', overflowY: 'auto' }}>
              {historyEvents.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '28px 0', color: '#747775', fontSize: '13px' }}>
                  No history events logged yet.
                </div>
              ) : (
                historyEvents.map((e) => (
                  <div
                    key={e.id || e.eventId}
                    style={{
                      padding: '8px 12px',
                      background: '#f8fafd',
                      border: '1px solid #dadce0',
                      borderRadius: '6px',
                      fontSize: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                      <span style={{ fontWeight: '500', color: '#1f1f1f' }}>
                        {e.eventType === 'CELL_EDIT' ? `Cell (${e.row + 1}, ${e.col + 1}) edited` : e.eventType}
                      </span>
                      <span style={{ fontSize: '10px', color: '#747775' }}>
                        {new Date(e.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    {e.previousValue !== undefined && (
                      <p style={{ fontSize: '11px', color: '#444746', margin: '2px 0 0' }}>
                        Changed from <span style={{ textDecoration: 'line-through', color: '#d93025' }}>"{e.previousValue || 'empty'}"</span> to{' '}
                        <span style={{ fontWeight: '500', color: '#0f9d58' }}>"{e.newValue}"</span>
                      </p>
                    )}
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
