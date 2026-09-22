import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { workbookService } from '../services/workbookService';
import { X, UserPlus, Copy, Check, Trash2, Link as LinkIcon, Shield } from 'lucide-react';

export default function ShareModal() {
  const { activeModal, closeModal } = useUIStore();
  const { workbook } = useWorkbookStore();

  const [emailOrId, setEmailOrId] = useState('');
  const [role, setRole] = useState('EDITOR');
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (activeModal === 'share' && workbook?.id) {
      workbookService.getPermissions(workbook.id)
        .then((data) => setPermissions(Array.isArray(data) ? data : []))
        .catch(() => setPermissions([]));
    }
  }, [activeModal, workbook?.id]);

  if (activeModal !== 'share') return null;

  const handleShare = async (e) => {
    e.preventDefault();
    const target = emailOrId.trim();
    if (!target || !workbook?.id) return;

    setLoading(true);
    try {
      const res = await workbookService.shareWorkbook(workbook.id, target, role);
      setPermissions((prev) => {
        const list = Array.isArray(prev) ? prev : [];
        if (Array.isArray(res)) return res;
        if (res && typeof res === 'object') {
          const targetId = res.userId || target;
          const idx = list.findIndex((p) => p.userId === targetId || (res.id && p.id === res.id));
          if (idx >= 0) {
            const next = [...list];
            next[idx] = res;
            return next;
          }
          return [...list, res];
        }
        return [...list, { userId: target, role }];
      });
      setEmailOrId('');
    } catch (err) {
      console.warn('Share request error, fallback locally:', err);
      setPermissions((prev) => {
        const list = Array.isArray(prev) ? prev : [];
        return [...list, { userId: target, role }];
      });
      setEmailOrId('');
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async (userId) => {
    if (!workbook?.id) return;
    try {
      await workbookService.revokePermission(workbook.id, userId);
    } catch (e) {}
    setPermissions((prev) => (Array.isArray(prev) ? prev.filter((p) => p.userId !== userId) : []));
  };

  const copyShareLink = () => {
    const origin = window.location.origin;
    const pathname = window.location.pathname;
    const currentWb = useWorkbookStore.getState().workbook;
    const wbId = currentWb?.id || workbook?.id || 'wb-default-1';
    const activeSheetId = useWorkbookStore.getState().activeSheetId;
    const shareUrl = `${origin}${pathname}?wb=${wbId}${activeSheetId ? `&sheet=${activeSheetId}` : ''}`;
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div
        className="google-clean-dialog"
        style={{ maxWidth: '520px' }}
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
              <UserPlus size={20} />
            </div>
            <div>
              <h2 className="google-dialog-title">
                Share "{workbook?.title || 'Spreadsheet'}"
              </h2>
              <p className="google-dialog-subtitle">
                Invite collaborators with granular roles
              </p>
            </div>
          </div>
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>

        {/* Content */}
        <div className="google-dialog-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Invite Form */}
          <form onSubmit={handleShare} style={{ display: 'flex', gap: '8px' }}>
            <input
              type="text"
              placeholder="Add people and groups (email or username)"
              value={emailOrId}
              onChange={(e) => setEmailOrId(e.target.value)}
              className="google-text-input"
              style={{ flex: 1 }}
            />
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              style={{
                padding: '0 10px',
                height: '38px',
                fontSize: '13px',
                color: '#1f1f1f',
                background: '#ffffff',
                border: '1px solid #dadce0',
                borderRadius: '4px',
                outline: 'none',
                cursor: 'pointer'
              }}
            >
              <option value="EDITOR">Editor</option>
              <option value="COMMENTER">Commenter</option>
              <option value="VIEWER">Viewer</option>
            </select>
            <button
              type="submit"
              disabled={loading || !emailOrId.trim()}
              className="google-btn-primary"
              style={{ height: '38px', padding: '0 18px', fontSize: '13px' }}
            >
              Invite
            </button>
          </form>

          {/* Collaborator List */}
          <div>
            <h3 style={{ fontSize: '11px', fontWeight: '600', color: '#747775', textTransform: 'uppercase', marginBottom: '8px', letterSpacing: '0.5px' }}>
              People with access
            </h3>
            <div style={{ maxHeight: '180px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: '#f8fafd', border: '1px solid #dadce0', borderRadius: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div className="comment-author-avatar" style={{ backgroundColor: '#0f9d58' }}>
                    O
                  </div>
                  <div>
                    <span style={{ fontSize: '13px', fontWeight: '500', color: '#1f1f1f' }}>Workbook Owner</span>
                    <p style={{ fontSize: '11px', color: '#5f6368', margin: 0 }}>Full administrative control</p>
                  </div>
                </div>
                <span style={{ fontSize: '11px', fontWeight: '500', color: '#0f9d58', background: '#e6f4ea', padding: '2px 8px', borderRadius: '12px' }}>
                  Owner
                </span>
              </div>

              {(Array.isArray(permissions) ? permissions : []).map((p) => (
                <div
                  key={p.id || p.userId}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: '#ffffff', border: '1px solid #dadce0', borderRadius: '8px' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div className="comment-author-avatar" style={{ backgroundColor: '#1a73e8' }}>
                      {p.userId?.charAt(0).toUpperCase()}
                    </div>
                    <span style={{ fontSize: '13px', fontWeight: '500', color: '#1f1f1f' }}>{p.userId}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '11px', fontWeight: '500', color: '#5f6368', background: '#f0f4f9', padding: '2px 8px', borderRadius: '12px' }}>
                      {p.role}
                    </span>
                    <button
                      onClick={() => handleRevoke(p.userId)}
                      style={{ background: 'transparent', border: 'none', color: '#747775', cursor: 'pointer', padding: '4px', borderRadius: '50%' }}
                      onMouseEnter={(e) => e.currentTarget.style.color = '#d93025'}
                      onMouseLeave={(e) => e.currentTarget.style.color = '#747775'}
                      title="Remove access"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Link Sharing Info */}
          <div className="google-info-card" style={{ marginTop: '4px' }}>
            <LinkIcon size={16} color="#1a73e8" style={{ flexShrink: 0 }} />
            <span style={{ fontSize: '12px', color: '#444746', lineHeight: 1.4 }}>
              Anyone with this spreadsheet link and registered account can view and edit collaboratively.
            </span>
          </div>

          {/* Footer */}
          <div className="google-dialog-footer" style={{ margin: 0, justifyContent: 'space-between' }}>
            <button
              type="button"
              onClick={copyShareLink}
              className="google-btn-text"
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              {copied ? <Check size={14} color="#0f9d58" /> : <Copy size={14} />}
              <span>{copied ? 'Link Copied!' : 'Copy Link'}</span>
            </button>
            <button
              type="button"
              onClick={closeModal}
              className="google-btn-primary"
            >
              Done
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
