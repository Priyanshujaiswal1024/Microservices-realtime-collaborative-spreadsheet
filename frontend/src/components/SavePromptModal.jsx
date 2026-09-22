import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useGridStore } from '../store/useGridStore';
import { useAuthStore } from '../store/useAuthStore';
import { useCollabStore } from '../store/useCollabStore';
import { X, Loader2, LogIn, ShieldCheck } from 'lucide-react';

import { setPendingSave } from '../utils/pendingSave';

export default function SavePromptModal() {
  const { activeModal, closeModal, openModal, pendingAction, clearPendingAction } = useUIStore();
  const { workbook, createWorkbook } = useWorkbookStore();
  const { loadSheetCells } = useGridStore();
  const { isAuthenticated } = useAuthStore();

  const [workbookTitle, setWorkbookTitle] = useState(
    workbook?.title && workbook.title !== 'Untitled Spreadsheet' && workbook.title !== 'Untitled spreadsheet'
      ? workbook.title
      : ''
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  // Step: 'name' → user types name | 'loginRequired' → we ask them to sign in
  const [step, setStep] = useState('name');
  const [plannedAction, setPlannedAction] = useState(null); // 'save' | 'saveAndShare'

  // After user logs in, auto-resume save if there was a pending title
  useEffect(() => {
    if (activeModal === 'savePrompt' && isAuthenticated && step === 'loginRequired') {
      // User just logged in — resume save automatically
      handleActuallySave(plannedAction);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  if (activeModal !== 'savePrompt') return null;

  const handleActuallySave = async (action) => {
    const title = workbookTitle.trim() || 'Untitled spreadsheet';
    setSaving(true);
    setError('');

    try {
      const saved = await createWorkbook(title);
      if (saved?.id) {
        const sheetId = saved.sheets?.[0]?.id;
        if (sheetId) {
          const currentCells = useGridStore.getState().cells;
          if (currentCells) {
            Object.values(currentCells).forEach((c) => {
              if (c && c.rawValue !== undefined && c.rawValue !== '') {
                useGridStore.getState().updateCellValue(c.row, c.col, c.rawValue, sheetId);
              }
            });
          }
          await loadSheetCells(sheetId, true);
          useCollabStore.getState().initCollab(sheetId);
        }

        closeModal();
        clearPendingAction();
        setStep('name');

        if (action === 'saveAndShare') {
          setTimeout(() => openModal('share'), 120);
        }
        return saved;
      }
    } catch (err) {
      console.error('Failed to save workbook:', err);
      setError('Could not save spreadsheet. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveOnly = async () => {
    if (!isAuthenticated) {
      setPlannedAction('save');
      setStep('loginRequired');
      return;
    }
    await handleActuallySave('save');
  };

  const handleSaveAndShare = async (e) => {
    e?.preventDefault();
    if (!isAuthenticated) {
      setPlannedAction('saveAndShare');
      setStep('loginRequired');
      return;
    }
    await handleActuallySave('saveAndShare');
  };

  const handleDiscard = () => {
    setStep('name');
    closeModal();
    clearPendingAction();
    if (pendingAction === 'share') {
      setTimeout(() => openModal('share'), 120);
    } else if (pendingAction === 'exit') {
      window.close();
    }
  };

  const handleGoToLogin = () => {
    // Close this modal and open Auth modal; when user logs in, useEffect will auto-resume
    closeModal();
    // Re-open savePrompt after login by watching auth state in App.jsx or via openModal
    // For now open auth, and store pending info globally
    setPendingSave(workbookTitle.trim() || 'Untitled spreadsheet', plannedAction);
    openModal('auth');
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div
        className="google-clean-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="google-dialog-header">
          <div>
            {step === 'name' ? (
              <>
                <h2 className="google-dialog-title">Name before sharing</h2>
                <p className="google-dialog-subtitle">
                  Give your untitled spreadsheet a name before sharing.
                </p>
              </>
            ) : (
              <>
                <h2 className="google-dialog-title">Sign in required</h2>
                <p className="google-dialog-subtitle">
                  You need to sign in to save <strong>"{workbookTitle || 'Untitled spreadsheet'}"</strong> to your account.
                </p>
              </>
            )}
          </div>
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>

        {/* Body */}
        {step === 'name' ? (
          <form onSubmit={handleSaveAndShare} className="google-dialog-body">
            {error && (
              <div className="google-error-alert">
                <span>{error}</span>
              </div>
            )}

            <div className="google-input-group">
              <label className="google-input-label">Spreadsheet name</label>
              <input
                type="text"
                value={workbookTitle}
                onChange={(e) => setWorkbookTitle(e.target.value)}
                placeholder="e.g. Budget Plan, Project Tracker..."
                autoFocus
                className="google-text-input"
              />
            </div>

            <div className="google-info-card">
              <svg width="20" height="24" viewBox="0 0 20 24" fill="none" style={{ flexShrink: 0 }}>
                <path d="M13 0H2C0.9 0 0 0.9 0 2V22C0 23.1 0.9 24 2 24H18C19.1 24 20 23.1 20 22V7L13 0Z" fill="#0F9D58"/>
                <rect x="4" y="10" width="12" height="9" rx="1" fill="white"/>
              </svg>
              <span style={{ fontSize: '12px', color: '#444746', lineHeight: 1.4 }}>
                Choose <strong>Save only</strong> to save this spreadsheet to your drive, or <strong>Save &amp; share</strong> to invite collaborators.
              </span>
            </div>

            {/* Footer */}
            <div className="google-dialog-footer" style={{ gap: '8px' }}>
              <button
                type="button"
                onClick={handleDiscard}
                className="google-btn-text"
              >
                Skip &amp; share
              </button>
              <button
                type="button"
                onClick={handleSaveOnly}
                disabled={saving}
                className="google-btn-secondary"
              >
                {saving && plannedAction === 'save' ? 'Saving...' : 'Save only'}
              </button>
              <button
                type="submit"
                disabled={saving}
                className="google-btn-primary"
              >
                {saving && plannedAction === 'saveAndShare' ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Loader2 size={14} className="animate-spin" />
                    <span>Saving...</span>
                  </div>
                ) : (
                  <span>Save &amp; share</span>
                )}
              </button>
            </div>
          </form>
        ) : (
          /* ── Step 2: Login Required ── */
          <div className="google-dialog-body">
            {/* Illustration */}
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '16px',
              padding: '12px 0 20px',
              textAlign: 'center',
            }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'linear-gradient(135deg, #e8f0fe 0%, #d2e3fc 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 2px 12px rgba(26,115,232,0.2)',
              }}>
                <ShieldCheck size={32} color="#1a73e8" />
              </div>
              <div>
                <p style={{ fontSize: '14px', color: '#3c4043', margin: '0 0 6px', fontWeight: 500 }}>
                  Your work will be saved to your account
                </p>
                <p style={{ fontSize: '12px', color: '#80868b', margin: 0, lineHeight: 1.5 }}>
                  After signing in, your spreadsheet <strong>"{workbookTitle || 'Untitled spreadsheet'}"</strong> will automatically be saved and a share link will be generated.
                </p>
              </div>
            </div>

            {/* Footer */}
            <div className="google-dialog-footer" style={{ gap: '8px' }}>
              <button
                type="button"
                onClick={() => setStep('name')}
                className="google-btn-text"
              >
                ← Back
              </button>
              <button
                type="button"
                onClick={handleGoToLogin}
                className="google-btn-primary"
                style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <LogIn size={16} />
                Sign in to save
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
