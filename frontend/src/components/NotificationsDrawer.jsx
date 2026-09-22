import React, { useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useNotificationStore } from '../store/useNotificationStore';
import { X, Bell, CheckCheck, MessageSquare, Share2 } from 'lucide-react';

export default function NotificationsDrawer() {
  const { activeModal, closeModal } = useUIStore();
  const {
    notifications,
    loadNotifications,
    markAsRead,
    markAllAsRead,
  } = useNotificationStore();

  useEffect(() => {
    if (activeModal === 'notifications') {
      loadNotifications();
    }
  }, [activeModal, loadNotifications]);

  if (activeModal !== 'notifications') return null;

  const getIcon = (type) => {
    switch (type) {
      case 'COMMENT_MENTION':
      case 'COMMENT_REPLY':
        return <MessageSquare size={14} color="#1a73e8" />;
      case 'SHEET_SHARED':
        return <Share2 size={14} color="#0f9d58" />;
      default:
        return <Bell size={14} color="#1a73e8" />;
    }
  };

  return (
    <div className="google-comments-sidebar">
      {/* Header */}
      <div className="google-comments-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Bell size={18} color="#1a73e8" />
          <h2 style={{ fontSize: '15px', fontWeight: '500', color: '#1f1f1f', margin: 0 }}>
            Notifications
          </h2>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          {notifications.some(n => !n.read) && (
            <button
              onClick={markAllAsRead}
              className="google-btn-text"
              style={{ fontSize: '12px', padding: '4px 8px' }}
            >
              Mark all read
            </button>
          )}
          <button onClick={closeModal} className="google-close-icon-btn">
            <X size={18} color="#5f6368" />
          </button>
        </div>
      </div>

      {/* Notifications List */}
      <div className="google-comments-list">
        {notifications.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#747775', fontSize: '13px', padding: '48px 16px' }}>
            <Bell size={32} color="#dadce0" style={{ margin: '0 auto 12px' }} />
            <p style={{ margin: 0, fontWeight: '500', color: '#444746' }}>All caught up</p>
            <p style={{ margin: '4px 0 0', fontSize: '12px' }}>
              No unread notifications at this time.
            </p>
          </div>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.read && markAsRead(n.id)}
              className="google-thread-card"
              style={{
                cursor: 'pointer',
                backgroundColor: n.read ? '#ffffff' : '#f8fafd',
                borderColor: n.read ? '#dadce0' : '#8ab4f8',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {getIcon(n.type)}
                  <span style={{ fontSize: '12px', fontWeight: '500', color: '#1f1f1f' }}>{n.title}</span>
                </div>
                {!n.read && (
                  <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: '#1a73e8' }} />
                )}
              </div>
              <p style={{ fontSize: '12px', color: '#444746', margin: '4px 0 0', lineHeight: 1.35 }}>
                {n.message}
              </p>
              <span style={{ fontSize: '11px', color: '#747775', marginTop: '4px' }}>
                {new Date(n.createdAt).toLocaleTimeString()}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
