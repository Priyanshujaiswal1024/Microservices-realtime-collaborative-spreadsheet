import React, { useState, useEffect } from 'react';
import { useUIStore } from '../store/useUIStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useCommentStore } from '../store/useCommentStore';
import { useGridStore } from '../store/useGridStore';
import { useCollabStore } from '../store/useCollabStore';
import { useAuthStore } from '../store/useAuthStore';
import { toA1Notation } from '../utils/coordinate';
import { X, MessageSquare, Check, Trash2, CornerDownRight, CheckCircle2 } from 'lucide-react';

export default function CommentsDrawer() {
  const { activeModal, closeModal, activeCellCommentPos } = useUIStore();
  const { workbook, activeSheetId, setActiveSheetId } = useWorkbookStore();
  const { selectedCell, setSelectedCell, setSelectedRange, loadSheetCells } = useGridStore();
  const { initCollab } = useCollabStore();
  const { user } = useAuthStore();
  const {
    threads,
    loadComments,
    createComment,
    addReply,
    resolveThread,
    deleteThread,
  } = useCommentStore();

  const [newCommentText, setNewCommentText] = useState('');
  const [replyTextMap, setReplyTextMap] = useState({});
  const [filterMode, setFilterMode] = useState('all'); // 'all' | 'current'

  useEffect(() => {
    if (activeModal === 'comments' && workbook?.id) {
      loadComments(workbook.id, activeSheetId);
    }
  }, [activeModal, workbook?.id, activeSheetId, loadComments]);

  useEffect(() => {
    if (activeModal !== 'comments') return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        closeModal();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeModal, closeModal]);

  if (activeModal !== 'comments') return null;

  const targetRow = activeCellCommentPos ? activeCellCommentPos.row : selectedCell.row;
  const targetCol = activeCellCommentPos ? activeCellCommentPos.col : selectedCell.col;
  const activeCoordNotation = toA1Notation(targetRow, targetCol);

  const activeSheetObj = workbook?.sheets?.find((s) => s.id === activeSheetId);
  const activeSheetName = activeSheetObj?.name || 'Current Sheet';

  const handleCreateComment = async (e) => {
    e.preventDefault();
    if (!newCommentText.trim()) return;

    await createComment(activeSheetId, {
      workbookId: workbook?.id,
      row: targetRow,
      col: targetCol,
      content: newCommentText.trim(),
    });
    setNewCommentText('');
  };

  const handleSendReply = async (threadId, threadSheetId) => {
    const text = replyTextMap[threadId];
    if (!text || !text.trim()) return;

    await addReply(threadId, text.trim(), threadSheetId || activeSheetId);
    setReplyTextMap({ ...replyTextMap, [threadId]: '' });
  };

  const handleJumpToComment = (thread) => {
    if (!thread) return;
    if (thread.sheetId && thread.sheetId !== activeSheetId) {
      setActiveSheetId(thread.sheetId);
      loadSheetCells(thread.sheetId, false);
      initCollab(thread.sheetId);
    }
    const r = Number(thread.row);
    const c = Number(thread.col);
    if (!isNaN(r) && !isNaN(c)) {
      setSelectedCell(r, c);
      setSelectedRange(r, c, r, c);
      useUIStore.setState({ activeCellCommentPos: { row: r, col: c } });
    }
  };

  const filteredThreads = threads.filter((t) => {
    if (filterMode === 'current') {
      return !t.sheetId || t.sheetId === activeSheetId;
    }
    return true;
  });

  const currentSheetCount = threads.filter((t) => !t.sheetId || t.sheetId === activeSheetId).length;

  return (
    <div className="google-comments-sidebar">
      {/* Header */}
      <div className="google-comments-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MessageSquare size={18} color="#1a73e8" />
          <h2 style={{ fontSize: '15px', fontWeight: '500', color: '#1f1f1f', margin: 0 }}>
            Comments
          </h2>
        </div>
        <button
          onClick={closeModal}
          className="google-close-icon-btn"
          title="Close comments"
        >
          <X size={18} color="#5f6368" />
        </button>
      </div>

      {/* New Comment on Active Cell Box (Google Sheets Clean Style) */}
      <div className="google-new-comment-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div
              className="comment-author-avatar"
              style={{ backgroundColor: user?.colorHex || '#1a73e8' }}
            >
              {(user?.username || 'U').charAt(0).toUpperCase()}
            </div>
            <span style={{ fontSize: '12px', fontWeight: '500', color: '#1f1f1f' }}>
              {user?.username || 'Collaborator'}
            </span>
          </div>
          <span style={{ fontSize: '11px', color: '#1a73e8', fontWeight: '500', background: '#e8f0fe', padding: '2px 8px', borderRadius: '12px' }}>
            {activeSheetName} • Cell {activeCoordNotation}
          </span>
        </div>

        <form onSubmit={handleCreateComment}>
          <textarea
            placeholder="Comment or add others with @"
            value={newCommentText}
            onChange={(e) => setNewCommentText(e.target.value)}
            className="google-comment-textarea"
            rows={2}
          />
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
            <button
              type="button"
              onClick={() => setNewCommentText('')}
              className="google-comment-cancel-btn"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!newCommentText.trim()}
              className="google-comment-submit-btn"
            >
              Comment
            </button>
          </div>
        </form>
      </div>

      {/* Sheet Filter Toggle Tabs */}
      <div style={{ display: 'flex', gap: '4px', padding: '0 16px 8px', borderBottom: '1px solid #f1f3f4' }}>
        <button
          onClick={() => setFilterMode('all')}
          style={{
            padding: '4px 10px',
            borderRadius: '16px',
            fontSize: '11px',
            fontWeight: '500',
            border: 'none',
            background: filterMode === 'all' ? '#e8f0fe' : 'transparent',
            color: filterMode === 'all' ? '#1a73e8' : '#5f6368',
            cursor: 'pointer',
          }}
        >
          All Sheets ({threads.length})
        </button>
        <button
          onClick={() => setFilterMode('current')}
          style={{
            padding: '4px 10px',
            borderRadius: '16px',
            fontSize: '11px',
            fontWeight: '500',
            border: 'none',
            background: filterMode === 'current' ? '#e8f0fe' : 'transparent',
            color: filterMode === 'current' ? '#1a73e8' : '#5f6368',
            cursor: 'pointer',
          }}
        >
          This Sheet ({currentSheetCount})
        </button>
      </div>

      {/* Comments Thread List */}
      <div className="google-comments-list">
        {filteredThreads.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#5f6368', fontSize: '13px', padding: '48px 16px' }}>
            <MessageSquare size={32} color="#dadce0" style={{ margin: '0 auto 12px' }} />
            <p style={{ margin: 0, fontWeight: '500', color: '#444746' }}>No comments yet</p>
            <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#747775' }}>
              Select a cell and add a comment to start a discussion.
            </p>
          </div>
        ) : (
          filteredThreads.map((thread) => {
            const coord = toA1Notation(thread.row, thread.col);
            const authorInitial = (thread.authorName || 'C').charAt(0).toUpperCase();
            const threadSheet = workbook?.sheets?.find((s) => s.id === thread.sheetId);
            const sheetLabel = threadSheet?.name || (thread.sheetId === activeSheetId ? activeSheetName : 'Sheet');

            return (
              <div
                key={thread.id}
                onClick={() => handleJumpToComment(thread)}
                style={{ cursor: 'pointer' }}
                className={`google-thread-card ${thread.resolved ? 'resolved' : ''}`}
                title="Click to jump to cell"
              >
                {/* Thread Header */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div className="comment-author-avatar" style={{ backgroundColor: '#1a73e8' }}>
                      {authorInitial}
                    </div>
                    <div>
                      <div style={{ fontSize: '12px', fontWeight: '500', color: '#1f1f1f' }}>
                        {thread.authorName || 'Collaborator'}
                      </div>
                      <div style={{ fontSize: '11px', color: '#747775' }}>
                        <span style={{ color: '#1a73e8', fontWeight: '500' }}>{sheetLabel}</span> • Cell {coord}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }} onClick={(e) => e.stopPropagation()}>
                    {!thread.resolved ? (
                      <button
                        onClick={() => resolveThread(thread.id, thread.sheetId || activeSheetId)}
                        title="Mark as resolved"
                        className="google-thread-action-btn"
                      >
                        <Check size={14} color="#0f9d58" />
                      </button>
                    ) : (
                      <span title="Resolved" style={{ color: '#0f9d58', display: 'flex', alignItems: 'center' }}>
                        <CheckCircle2 size={14} />
                      </span>
                    )}
                    <button
                      onClick={() => deleteThread(thread.id, thread.sheetId || activeSheetId)}
                      title="Delete thread"
                      className="google-thread-action-btn"
                    >
                      <Trash2 size={14} color="#d93025" />
                    </button>
                  </div>
                </div>

                {/* Comment Content */}
                <p className="google-thread-content">{thread.initialContent}</p>

                {/* Replies */}
                {thread.replies?.length > 0 && (
                  <div className="google-replies-list">
                    {thread.replies.map((reply) => (
                      <div key={reply.id} className="google-reply-item">
                        <span style={{ fontWeight: '500', color: '#1f1f1f' }}>
                          {reply.authorName || 'Collaborator'}:{' '}
                        </span>
                        <span style={{ color: '#444746' }}>{reply.content}</span>
                      </div>
                    ))}
                  </div>
                )}

                {/* Reply Input */}
                {!thread.resolved && (
                  <div
                    style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '6px' }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <CornerDownRight size={13} color="#747775" />
                    <input
                      type="text"
                      placeholder="Reply..."
                      value={replyTextMap[thread.id] || ''}
                      onChange={(e) =>
                        setReplyTextMap({ ...replyTextMap, [thread.id]: e.target.value })
                      }
                      onKeyDown={(e) => e.key === 'Enter' && handleSendReply(thread.id, thread.sheetId)}
                      className="google-reply-input"
                    />
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
