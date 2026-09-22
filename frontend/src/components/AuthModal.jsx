import React, { useState } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useAuthStore } from '../store/useAuthStore';
import { useCollabStore } from '../store/useCollabStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { getUserInitial } from '../utils/avatar';
import { X, LogOut, Check, Loader2, AlertCircle } from 'lucide-react';

const PRESET_COLORS = [
  '#1a73e8', // Google Blue
  '#0f9d58', // Google Green
  '#ea4335', // Google Red
  '#fbbc04', // Google Yellow
  '#8e24aa', // Purple
  '#d81b60', // Pink
  '#00897b', // Teal
  '#3949ab', // Indigo
];

// Authentic Google Sheets Green document icon
const GoogleSheetsIcon = ({ width = 28, height = 34 }) => (
  <svg width={width} height={height} viewBox="0 0 32 38" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M21 0H3.5C1.57 0 0 1.57 0 3.5V34.5C0 36.43 1.57 38 3.5 38H28.5C30.43 38 32 36.43 32 34.5V11L21 0Z" fill="#0F9D58"/>
    <path d="M21 0V11H32L21 0Z" fill="#87CEAC"/>
    <rect x="7" y="16" width="18" height="15" rx="1.5" fill="white"/>
    <line x1="7" y1="21" x2="25" y2="21" stroke="#0F9D58" strokeWidth="1.5"/>
    <line x1="7" y1="26" x2="25" y2="26" stroke="#0F9D58" strokeWidth="1.5"/>
    <line x1="13.5" y1="16" x2="13.5" y2="31" stroke="#0F9D58" strokeWidth="1.5"/>
  </svg>
);

export default function AuthModal() {
  const { activeModal, closeModal, showToast } = useUIStore();
  const {
    user,
    isAuthenticated,
    login,
    register,
    logout,
    updateProfile,
    userColor,
    loading,
  } = useAuthStore();
  const { activeSheetId } = useWorkbookStore();
  const { initCollab } = useCollabStore();

  const [tab, setTab] = useState('login'); // 'login' | 'register'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [formError, setFormError] = useState(null);

  // Profile edit state when logged in
  const [displayName, setDisplayName] = useState(user?.fullName || user?.username || 'User');
  const [selectedColor, setSelectedColor] = useState(user?.colorHex || user?.color || userColor || '#1a73e8');

  if (activeModal !== 'auth') return null;

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    try {
      await login(email.trim(), password);
      if (activeSheetId) {
        initCollab(activeSheetId);
      }
      showToast('Signed in successfully', 'success');
      closeModal();
    } catch (err) {
      setFormError(err.message || 'Username or password is incorrect. Please try again.');
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    try {
      await register({
        email: email.trim(),
        username: (username || email.split('@')[0]).trim(),
        password,
        fullName: (fullName || username || email.split('@')[0]).trim(),
      });
      if (activeSheetId) {
        initCollab(activeSheetId);
      }
      showToast('Account created successfully', 'success');
      closeModal();
    } catch (err) {
      setFormError(err.message || 'Registration failed. Please check your credentials and try again.');
    }
  };

  const handleUpdateProfile = (e) => {
    e?.preventDefault();
    updateProfile(displayName, selectedColor);
    if (activeSheetId) {
      initCollab(activeSheetId);
    }
    showToast('Profile updated', 'success');
    closeModal();
  };

  const handleLogout = () => {
    logout();
    showToast('Signed out', 'info');
    closeModal();
  };

  const currentInitial = isAuthenticated && user ? getUserInitial(user) : '?';
  const currentColor = user?.colorHex || user?.color || selectedColor || '#1a73e8';

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
      onClick={closeModal}
    >
      <div
        style={{
          position: 'relative',
          background: '#ffffff',
          borderRadius: '16px',
          boxShadow: '0 16px 48px rgba(0, 0, 0, 0.22), 0 2px 8px rgba(0, 0, 0, 0.08)',
          width: '100%',
          maxWidth: '430px',
          margin: '20px',
          overflow: 'hidden',
          animation: 'fadeInScale 0.18s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button in top right */}
        <button
          onClick={closeModal}
          title="Close"
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
            transition: 'background 0.15s',
            zIndex: 10,
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = '#f1f3f4')}
          onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
        >
          <X size={18} />
        </button>

        {/* LOGGED IN VIEW */}
        {isAuthenticated && user ? (
          <div style={{ padding: '32px 32px 24px' }}>
            {/* Header: Collaborator Account */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
              <GoogleSheetsIcon width={22} height={26} />
              <div>
                <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '500', color: '#202124', fontFamily: '"Google Sans", Roboto, sans-serif' }}>
                  Collaborator Account
                </h2>
                <div style={{ fontSize: '12px', color: '#5f6368' }}>Connected Collaborator</div>
              </div>
            </div>

            {/* User Profile Card */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
                padding: '16px',
                background: '#f8fafd',
                borderRadius: '12px',
                border: '1px solid #dadce0',
                marginBottom: '20px',
              }}
            >
              <div
                style={{
                  width: '52px',
                  height: '52px',
                  borderRadius: '50%',
                  backgroundColor: currentColor,
                  color: '#ffffff',
                  fontSize: '22px',
                  fontWeight: '600',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  userSelect: 'none',
                  flexShrink: 0,
                  boxShadow: '0 2px 6px rgba(0,0,0,0.12)',
                }}
              >
                {currentInitial}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: '15px', fontWeight: '600', color: '#1f1f1f', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {user.fullName || user.username}
                </div>
                <div style={{ fontSize: '12.5px', color: '#5f6368', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {user.email || 'collaborator@workspace.local'}
                </div>
                <div style={{ display: 'inline-block', marginTop: '6px', fontSize: '11px', fontWeight: '500', color: '#0f9d58', background: '#e6f4ea', padding: '2px 8px', borderRadius: '10px' }}>
                  Active Collaborator
                </div>
              </div>
            </div>

            {/* Edit Display Name */}
            <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={labelStyle}>Display Name in Collaborations</label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Enter your name"
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

              {/* Color Palette */}
              <div>
                <label style={{ ...labelStyle, marginBottom: '8px' }}>Avatar & Cursor Color</label>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {PRESET_COLORS.map((c) => (
                    <div
                      key={c}
                      onClick={() => setSelectedColor(c)}
                      style={{
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        backgroundColor: c,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: selectedColor === c ? '2px solid #1a73e8' : '2px solid transparent',
                        boxShadow: selectedColor === c ? '0 0 0 2px rgba(26, 115, 232, 0.4)' : 'none',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      {selectedColor === c && <Check size={14} color="#ffffff" strokeWidth={3} />}
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '4px' }}>
                <button
                  type="submit"
                  style={primaryBtnStyle}
                >
                  Save Changes
                </button>
              </div>
            </form>

            {/* Footer / Sign Out */}
            <div style={{ borderTop: '1px solid #dadce0', marginTop: '20px', paddingTop: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <button
                type="button"
                onClick={handleLogout}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  background: 'none',
                  border: '1px solid #dadce0',
                  borderRadius: '6px',
                  padding: '7px 14px',
                  color: '#d93025',
                  fontSize: '13px',
                  fontWeight: '500',
                  cursor: 'pointer',
                  fontFamily: '"Google Sans", Roboto, sans-serif',
                  transition: 'background 0.15s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#fce8e6')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                <LogOut size={14} />
                <span>Sign Out</span>
              </button>

              <button
                type="button"
                onClick={closeModal}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#5f6368',
                  fontSize: '13px',
                  cursor: 'pointer',
                  fontFamily: '"Google Sans", Roboto, sans-serif',
                }}
              >
                Done
              </button>
            </div>
          </div>
        ) : (
          /* NOT LOGGED IN VIEW: EXACT GOOGLE AUTHENTICATION CARD */
          <div style={{ padding: '36px 36px 30px' }}>
            {/* Top Logo and Header */}
            <div style={{ textAlign: 'center', marginBottom: '22px' }}>
              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
                <GoogleSheetsIcon width={36} height={44} />
              </div>

              <h1
                style={{
                  margin: 0,
                  fontSize: '22px',
                  fontWeight: '500',
                  color: '#202124',
                  fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
                  letterSpacing: '-0.2px',
                }}
              >
                {tab === 'login' ? 'Sign in' : 'Create your Google Account'}
              </h1>
              <p
                style={{
                  margin: '6px 0 0',
                  fontSize: '14px',
                  color: '#5f6368',
                  fontFamily: 'Roboto, Arial, sans-serif',
                }}
              >
                to continue to Google Sheets
              </p>
            </div>

            {/* Error Banner */}
            {formError && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: '#fce8e6',
                  border: '1px solid #fad2cf',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  marginBottom: '16px',
                  fontSize: '13px',
                  color: '#c5221f',
                  fontFamily: 'Roboto, Arial, sans-serif',
                }}
              >
                <AlertCircle size={16} style={{ flexShrink: 0 }} />
                <span>{formError}</span>
              </div>
            )}

            {/* Form Fields */}
            {tab === 'login' ? (
              <form onSubmit={handleLoginSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div>
                  <label style={labelStyle}>Email or username</label>
                  <input
                    type="text"
                    required
                    autoFocus
                    placeholder="Enter email or username"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
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
                    required
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
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

                {/* Bottom Actions: Create Account link on left, Sign In button on right */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginTop: '12px',
                  }}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setTab('register');
                      setFormError(null);
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#1a73e8',
                      fontSize: '14px',
                      fontWeight: '500',
                      cursor: 'pointer',
                      fontFamily: '"Google Sans", Roboto, sans-serif',
                      padding: '8px 0',
                    }}
                  >
                    Create account
                  </button>

                  <button
                    type="submit"
                    disabled={loading}
                    style={primaryBtnStyle}
                  >
                    {loading ? (
                      <>
                        <Loader2 size={16} className="animate-spin" style={{ marginRight: '6px' }} />
                        Signing in...
                      </>
                    ) : (
                      'Sign In'
                    )}
                  </button>
                </div>
              </form>
            ) : (
              /* Register Form */
              <form onSubmit={handleRegisterSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <div style={{ flex: 1 }}>
                    <label style={labelStyle}>Full Name</label>
                    <input
                      type="text"
                      required
                      autoFocus
                      placeholder="e.g. John Doe"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
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
                      required
                      placeholder="johndoe"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
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
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
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
                    required
                    placeholder="Create a strong password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
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

                {/* Bottom Actions */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginTop: '12px',
                  }}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setTab('login');
                      setFormError(null);
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#1a73e8',
                      fontSize: '14px',
                      fontWeight: '500',
                      cursor: 'pointer',
                      fontFamily: '"Google Sans", Roboto, sans-serif',
                      padding: '8px 0',
                    }}
                  >
                    Sign in instead
                  </button>

                  <button
                    type="submit"
                    disabled={loading}
                    style={primaryBtnStyle}
                  >
                    {loading ? (
                      <>
                        <Loader2 size={16} className="animate-spin" style={{ marginRight: '6px' }} />
                        Creating...
                      </>
                    ) : (
                      'Create account'
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

const labelStyle = {
  fontSize: '12.5px',
  fontWeight: '500',
  color: '#3c4043',
  display: 'block',
  marginBottom: '6px',
  fontFamily: 'Roboto, Arial, sans-serif',
};

const inputStyle = {
  width: '100%',
  padding: '11px 14px',
  border: '1px solid #dadce0',
  borderRadius: '6px',
  fontSize: '14px',
  color: '#202124',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'border-color 0.15s, box-shadow 0.15s',
  fontFamily: 'Roboto, Arial, sans-serif',
};

const primaryBtnStyle = {
  padding: '9px 24px',
  background: '#1a73e8',
  color: '#ffffff',
  border: 'none',
  borderRadius: '6px',
  fontSize: '14px',
  fontWeight: '500',
  cursor: 'pointer',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontFamily: '"Google Sans", Roboto, Arial, sans-serif',
  transition: 'background 0.15s, box-shadow 0.15s',
  boxShadow: '0 1px 2px rgba(26, 115, 232, 0.3)',
};
