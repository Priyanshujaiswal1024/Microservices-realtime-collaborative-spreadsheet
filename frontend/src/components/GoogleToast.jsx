import React from 'react';
import { useUIStore } from '../store/useUIStore';
import { AlertCircle, CheckCircle, Info, Lock, X } from 'lucide-react';

export default function GoogleToast() {
  const { toast, closeToast } = useUIStore();

  if (!toast) return null;

  const { message, type = 'info' } = toast;

  const renderIcon = () => {
    if (message.includes('🔒') || message.includes('protected')) {
      return <Lock size={16} color="#f28b82" style={{ flexShrink: 0 }} />;
    }
    switch (type) {
      case 'success':
        return <CheckCircle size={16} color="#81c995" style={{ flexShrink: 0 }} />;
      case 'warning':
      case 'error':
        return <AlertCircle size={16} color="#f28b82" style={{ flexShrink: 0 }} />;
      default:
        return <Info size={16} color="#8ab4f8" style={{ flexShrink: 0 }} />;
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '28px',
        left: '28px',
        zIndex: 999999,
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '12px 18px',
        background: '#202124',
        color: '#e8eaed',
        borderRadius: '6px',
        boxShadow: '0 4px 18px rgba(0, 0, 0, 0.35), 0 1px 4px rgba(0, 0, 0, 0.15)',
        fontSize: '13.5px',
        fontFamily: 'Roboto, Arial, sans-serif',
        maxWidth: '460px',
        lineHeight: '1.4',
        animation: 'slideUpFade 0.2s cubic-bezier(0, 0, 0.2, 1)',
      }}
    >
      {renderIcon()}
      <span style={{ flex: 1 }}>{message.replace(/^🔒\s*/, '')}</span>
      <button
        onClick={closeToast}
        style={{
          background: 'none',
          border: 'none',
          color: '#8ab4f8',
          fontSize: '13px',
          fontWeight: '500',
          cursor: 'pointer',
          padding: '2px 6px',
          borderRadius: '4px',
          fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
          marginLeft: '6px',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'rgba(138, 180, 248, 0.1)')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        Dismiss
      </button>
    </div>
  );
}
