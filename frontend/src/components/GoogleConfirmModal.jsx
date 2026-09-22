import React, { useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { AlertCircle, Trash2, HelpCircle } from 'lucide-react';

export default function GoogleConfirmModal() {
  const { confirmDialog, closeConfirm } = useUIStore();

  useEffect(() => {
    if (!confirmDialog) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        confirmDialog.onCancel?.();
        closeConfirm();
      } else if (e.key === 'Enter') {
        e.preventDefault();
        confirmDialog.onConfirm?.();
        closeConfirm();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [confirmDialog, closeConfirm]);

  if (!confirmDialog) return null;

  const {
    title = 'Confirm',
    message = 'Are you sure you want to proceed?',
    confirmText = 'OK',
    cancelText = 'Cancel',
    isDestructive = false,
    onConfirm,
    onCancel,
  } = confirmDialog;

  const handleCancel = () => {
    onCancel?.();
    closeConfirm();
  };

  const handleConfirm = () => {
    onConfirm?.();
    closeConfirm();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 99999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(15, 23, 42, 0.38)',
        backdropFilter: 'blur(2px)',
        WebkitBackdropFilter: 'blur(2px)',
        animation: 'fadeIn 0.15s ease-out',
      }}
      onClick={handleCancel}
    >
      <div
        style={{
          position: 'relative',
          background: '#ffffff',
          borderRadius: '12px',
          boxShadow: '0 16px 40px rgba(0, 0, 0, 0.22), 0 2px 10px rgba(0, 0, 0, 0.08)',
          width: '100%',
          maxWidth: '440px',
          margin: '20px',
          padding: '24px 28px 20px',
          boxSizing: 'border-box',
          animation: 'fadeInScale 0.18s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Dialog Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
          {isDestructive ? (
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: '#fce8e6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#d93025',
                flexShrink: 0,
              }}
            >
              <Trash2 size={18} />
            </div>
          ) : (
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: '#e8f0fe',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#1a73e8',
                flexShrink: 0,
              }}
            >
              <HelpCircle size={18} />
            </div>
          )}
          <h3
            style={{
              margin: 0,
              fontSize: '17px',
              fontWeight: '500',
              color: '#202124',
              fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              letterSpacing: '-0.2px',
            }}
          >
            {title}
          </h3>
        </div>

        {/* Dialog Message */}
        <div
          style={{
            fontSize: '14px',
            lineHeight: '1.55',
            color: '#3c4043',
            fontFamily: 'Roboto, Arial, sans-serif',
            marginBottom: '24px',
          }}
        >
          {message}
        </div>

        {/* Action Buttons */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <button
            type="button"
            onClick={handleCancel}
            style={{
              padding: '8px 18px',
              background: 'transparent',
              border: '1px solid #dadce0',
              borderRadius: '6px',
              color: '#1a73e8',
              fontSize: '14px',
              fontWeight: '500',
              cursor: 'pointer',
              fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              transition: 'background 0.15s, border-color 0.15s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f8fafd';
              e.currentTarget.style.borderColor = '#1a73e8';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
              e.currentTarget.style.borderColor = '#dadce0';
            }}
          >
            {cancelText}
          </button>

          <button
            type="button"
            onClick={handleConfirm}
            autoFocus
            style={{
              padding: '8px 22px',
              background: isDestructive ? '#d93025' : '#1a73e8',
              border: 'none',
              borderRadius: '6px',
              color: '#ffffff',
              fontSize: '14px',
              fontWeight: '500',
              cursor: 'pointer',
              fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              transition: 'background 0.15s, box-shadow 0.15s',
              boxShadow: isDestructive
                ? '0 1px 3px rgba(217, 48, 37, 0.3)'
                : '0 1px 3px rgba(26, 115, 232, 0.3)',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = isDestructive ? '#c5221f' : '#1557b0';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = isDestructive ? '#d93025' : '#1a73e8';
            }}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
