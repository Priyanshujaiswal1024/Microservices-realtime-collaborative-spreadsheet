import React, { useState } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { Loader2, X } from 'lucide-react';

// Exact authentic Google Sheets SVG logo matching Header.jsx
const GoogleSheetsLogo = () => (
  <svg width="42" height="50" viewBox="0 0 32 38" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M21 0H3.5C1.57 0 0 1.57 0 3.5V34.5C0 36.43 1.57 38 3.5 38H28.5C30.43 38 32 36.43 32 34.5V11L21 0Z" fill="#0F9D58"/>
    <path d="M21 0V11H32L21 0Z" fill="#87CEAC"/>
    <rect x="7" y="16" width="18" height="15" rx="1.5" fill="white"/>
    <line x1="7" y1="21" x2="25" y2="21" stroke="#0F9D58" strokeWidth="1.5"/>
    <line x1="7" y1="26" x2="25" y2="26" stroke="#0F9D58" strokeWidth="1.5"/>
    <line x1="13.5" y1="16" x2="13.5" y2="31" stroke="#0F9D58" strokeWidth="1.5"/>
  </svg>
);

export default function SharedLinkLoginGate({ workbookTitle, onContinueAsGuest }) {
  const { login, register, loading } = useAuthStore();
  const [tab, setTab] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login({ emailOrUsername: email, password });
    } catch (err) {
      setError(err?.message || 'Username or password is incorrect. Please try again.');
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    if (!username.trim() || !email.trim() || !password.trim()) {
      setError('⚠️ Please fill all required fields.');
      return;
    }
    try {
      await register({ username, email, password, fullName: fullName || username });
    } catch (err) {
      setError(err?.message || '⚠️ Registration failed. Please try again.');
    }
  };

  return (
    /* ── Fixed Backdrop: Spreadsheet grid is visible behind this soft tint ── */
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        /* Light frosted backdrop so Excel sheet behind remains clearly visible */
        backdropFilter: 'blur(2px)',
        WebkitBackdropFilter: 'blur(2px)',
        backgroundColor: 'rgba(15, 23, 42, 0.32)',
      }}
      onClick={(e) => {
        // Clicking outside the card closes gate and continues to sheet as guest
        if (e.target === e.currentTarget && onContinueAsGuest) {
          onContinueAsGuest();
        }
      }}
    >
      {/* ── Google-styled Modal Card ── */}
      <div
        style={{
          position: 'relative',
          background: '#ffffff',
          borderRadius: '12px',
          boxShadow: '0 12px 40px rgba(0, 0, 0, 0.22), 0 2px 8px rgba(0, 0, 0, 0.08)',
          width: '100%',
          maxWidth: '410px',
          overflow: 'hidden',
          margin: '20px',
          animation: 'fadeInScale 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button in top right */}
        {onContinueAsGuest && (
          <button
            onClick={onContinueAsGuest}
            title="Close and continue as guest"
            style={{
              position: 'absolute',
              top: '16px',
              right: '16px',
              width: '32px',
              height: '32px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: 'none',
              background: 'transparent',
              borderRadius: '50%',
              color: '#5f6368',
              cursor: 'pointer',
              transition: 'background 0.15s, color 0.15s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f1f3f4';
              e.currentTarget.style.color = '#202124';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
              e.currentTarget.style.color = '#5f6368';
            }}
          >
            <X size={18} />
          </button>
        )}

        {/* Header with Google Sheets Logo */}
        <div
          style={{
            padding: '32px 36px 20px',
            textAlign: 'center',
            borderBottom: '1px solid #e8eaed',
          }}
        >
          {/* Authentic Google Sheets green logo */}
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
            <GoogleSheetsLogo />
          </div>

          <h1
            style={{
              fontSize: '20px',
              fontWeight: '500',
              color: '#202124',
              margin: 0,
              fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              letterSpacing: '-0.2px',
            }}
          >
            Sign in to continue to sheet
          </h1>
        </div>

        {/* Tabs: Sign In / Create Account */}
        <div style={{ display: 'flex', borderBottom: '1px solid #e8eaed', background: '#fafbfc' }}>
          {[
            { key: 'login', label: 'Sign In' },
            { key: 'register', label: 'Create Account' },
          ].map((t) => (
            <button
              key={t.key}
              onClick={() => {
                setTab(t.key);
                setError('');
              }}
              style={{
                flex: 1,
                padding: '12px 8px',
                border: 'none',
                background: tab === t.key ? '#ffffff' : 'transparent',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: tab === t.key ? '600' : '400',
                color: tab === t.key ? '#1a73e8' : '#5f6368',
                borderBottom: tab === t.key ? '3px solid #1a73e8' : '3px solid transparent',
                transition: 'all 0.15s',
                fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              }}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Form Body */}
        <div style={{ padding: '24px 36px 28px' }}>
          {/* Error Banner */}
          {error && (
            <div
              style={{
                background: '#fce8e6',
                border: '1px solid #fad2cf',
                borderRadius: '6px',
                padding: '10px 14px',
                marginBottom: '18px',
                fontSize: '13px',
                color: '#c5221f',
                fontFamily: 'Roboto, Arial, sans-serif',
                lineHeight: '1.45',
              }}
            >
              {error}
            </div>
          )}

          {tab === 'login' ? (
            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
              <div>
                <label style={labelStyle}>Email or Username</label>
                <input
                  type="text"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter email or username"
                  required
                  autoFocus
                  style={inputStyle}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#1a73e8';
                    e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = '#dadce0';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              <div>
                <label style={labelStyle}>Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  required
                  style={inputStyle}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#1a73e8';
                    e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = '#dadce0';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              <button type="submit" disabled={loading} style={primaryBtnStyle}>
                {loading ? (
                  <>
                    <Loader2 size={16} style={{ marginRight: 8, animation: 'spin 1s linear infinite' }} />
                    Signing in...
                  </>
                ) : (
                  'Sign In'
                )}
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', gap: '10px' }}>
                <div style={{ flex: 1 }}>
                  <label style={labelStyle}>Full Name</label>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Rahul Sharma"
                    autoFocus
                    style={inputStyle}
                    onFocus={(e) => {
                      e.target.style.borderColor = '#1a73e8';
                      e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                    }}
                    onBlur={(e) => {
                      e.target.style.borderColor = '#dadce0';
                      e.target.style.boxShadow = 'none';
                    }}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={labelStyle}>Username</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="rahul_12"
                    required
                    style={inputStyle}
                    onFocus={(e) => {
                      e.target.style.borderColor = '#1a73e8';
                      e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                    }}
                    onBlur={(e) => {
                      e.target.style.borderColor = '#dadce0';
                      e.target.style.boxShadow = 'none';
                    }}
                  />
                </div>
              </div>

              <div>
                <label style={labelStyle}>Email address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="rahul@example.com"
                  required
                  style={inputStyle}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#1a73e8';
                    e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = '#dadce0';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              <div>
                <label style={labelStyle}>Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Min. 8 characters"
                  required
                  style={inputStyle}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#1a73e8';
                    e.target.style.boxShadow = '0 0 0 2px rgba(26, 115, 232, 0.15)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = '#dadce0';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              <button type="submit" disabled={loading} style={primaryBtnStyle}>
                {loading ? (
                  <>
                    <Loader2 size={16} style={{ marginRight: 8, animation: 'spin 1s linear infinite' }} />
                    Creating account...
                  </>
                ) : (
                  'Create account'
                )}
              </button>
            </form>
          )}

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0 16px' }}>
            <div style={{ flex: 1, height: '1px', background: '#e8eaed' }} />
            <span style={{ fontSize: '12px', color: '#9aa0a6', fontFamily: 'Roboto, Arial, sans-serif' }}>or</span>
            <div style={{ flex: 1, height: '1px', background: '#e8eaed' }} />
          </div>

          {/* Continue as Guest Button */}
          <button
            onClick={onContinueAsGuest}
            style={{
              width: '100%',
              padding: '10px 16px',
              background: '#ffffff',
              border: '1px solid #dadce0',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '13px',
              color: '#1a73e8',
              fontWeight: '500',
              fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
              transition: 'all 0.15s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f8fafd';
              e.currentTarget.style.borderColor = '#1a73e8';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = '#ffffff';
              e.currentTarget.style.borderColor = '#dadce0';
            }}
          >
            Continue as Guest
          </button>

          <p
            style={{
              fontSize: '11px',
              color: '#70757a',
              textAlign: 'center',
              marginTop: '14px',
              marginBottom: 0,
              fontFamily: 'Roboto, Arial, sans-serif',
              lineHeight: '1.4',
            }}
          >
            Signing in allows your name and cursor to appear to collaborators in real time
          </p>
        </div>
      </div>
    </div>
  );
}

const labelStyle = {
  fontSize: '12px',
  fontWeight: '500',
  color: '#3c4043',
  display: 'block',
  marginBottom: '6px',
  fontFamily: 'Roboto, Arial, sans-serif',
};

const inputStyle = {
  width: '100%',
  padding: '10px 12px',
  border: '1px solid #dadce0',
  borderRadius: '6px',
  fontSize: '14px',
  color: '#202124',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'all 0.15s ease',
  fontFamily: 'Roboto, Arial, sans-serif',
};

const primaryBtnStyle = {
  width: '100%',
  padding: '10px 24px',
  background: '#1a73e8',
  color: '#ffffff',
  border: 'none',
  borderRadius: '6px',
  fontSize: '14px',
  fontWeight: '500',
  cursor: 'pointer',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
  transition: 'background 0.15s, box-shadow 0.15s',
  boxShadow: '0 1px 2px rgba(26, 115, 232, 0.3)',
};
