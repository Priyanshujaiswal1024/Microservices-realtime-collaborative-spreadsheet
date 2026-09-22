import React, { useRef, useEffect, useState, useCallback } from 'react';
import { useGridStore } from '../store/useGridStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useCollabStore } from '../store/useCollabStore';
import { useCommentStore } from '../store/useCommentStore';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { indexToColLetter } from '../utils/coordinate';
import { Lock } from 'lucide-react';

const ROW_COUNT = 60;
const COL_COUNT = 26;
const DEFAULT_COL_WIDTH = 110;

export default function SpreadsheetGrid() {
  const {
    cells,
    selectedCell,
    setSelectedCell,
    selectedRange,
    setSelectedRange,
    editMode,
    setEditMode,
    updateCellValue,
  } = useGridStore();
  const { showToast } = useUIStore();

  const { activeSheetId, workbook, isCellProtected, loadProtectedRanges } = useWorkbookStore();
  const { remoteCursors, broadcastCursorMove } = useCollabStore();
  const { threads } = useCommentStore();
  const { openCellComment } = useUIStore();
  const auth = useAuthStore();

  const currentUserId = auth.user?.id || auth.user?.username || auth.clientId;
  const isOwner = (workbook?.ownerId && auth.user?.id && workbook.ownerId === auth.user.id)
    || (workbook?.userRole === 'OWNER' && auth.isAuthenticated);
  const userRole = isOwner ? 'OWNER' : (workbook?.userRole || 'COLLABORATOR');
  const userColor = auth.userColor || auth.user?.colorHex || auth.user?.color || '#8b5cf6';

  const gridRef = useRef(null);
  const inputRef = useRef(null);
  const [editValue, setEditValue] = useState('');
  const [isMouseDown, setIsMouseDown] = useState(false);
  const [dragAnchor, setDragAnchor] = useState(null);

  // Map of commented cells
  const commentedCells = new Set(
    threads
      .filter((t) => (!t.sheetId || t.sheetId === activeSheetId || !activeSheetId) && !t.resolved && t.row !== undefined && t.col !== undefined)
      .map((t) => `${Number(t.row)}:${Number(t.col)}`)
  );

  // Duplicate-name detection: assign "(1)", "(2)" suffix when same userName appears
  // from multiple tabs/browsers (same account, different clientIds)
  const cursorList = Object.values(remoteCursors).filter(
    (cur) =>
      cur &&
      cur.userId !== currentUserId &&
      cur.userId !== auth.user?.id &&
      cur.userId !== auth.user?.username &&
      cur.userName !== auth.user?.username
  );
  // Count how many times each name appears
  const nameCount = {};
  cursorList.forEach((cur) => {
    const n = cur.userName || 'Collaborator';
    nameCount[n] = (nameCount[n] || 0) + 1;
  });
  // Assign sequential suffix for each duplicate
  const nameIndex = {};
  const cursorDisplayName = {}; // userId → display name
  cursorList.forEach((cur) => {
    const n = cur.userName || 'Collaborator';
    if (nameCount[n] > 1) {
      nameIndex[n] = (nameIndex[n] || 0) + 1;
      cursorDisplayName[cur.userId] = `${n} (${nameIndex[n]})`;
    } else {
      cursorDisplayName[cur.userId] = n;
    }
  });

  // Load comments and protected ranges on mount & when sheet changes
  useEffect(() => {
    if (activeSheetId) {
      useCommentStore.getState().loadComments(null, activeSheetId);
      loadProtectedRanges(activeSheetId);
    }
  }, [activeSheetId, loadProtectedRanges]);

  // Focus in-cell editor
  useEffect(() => {
    if (editMode && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editMode]);

  // Broadcast cursor when selection changes
  useEffect(() => {
    if (activeSheetId) {
      broadcastCursorMove(activeSheetId, selectedCell.row, selectedCell.col);
    }
  }, [selectedCell, activeSheetId, broadcastCursorMove]);

  const startEdit = useCallback((initialVal = null) => {
    if (isCellProtected(activeSheetId, selectedCell.row, selectedCell.col, currentUserId, userRole)) {
      showToast('🔒 This cell is protected and cannot be edited by your account.', 'warning');
      return;
    }
    const key = `${selectedCell.row}:${selectedCell.col}`;
    const cell = cells[key];
    const val = initialVal !== null ? initialVal : (cell?.rawValue || cell?.value || '');
    setEditValue(val);
    setEditMode(true);
  }, [selectedCell, cells, setEditMode, isCellProtected, activeSheetId, currentUserId, userRole, showToast]);

  const commitEdit = useCallback(() => {
    if (editMode) {
      updateCellValue(selectedCell.row, selectedCell.col, editValue, activeSheetId);
      setEditMode(false);
    }
  }, [editMode, selectedCell, editValue, activeSheetId, updateCellValue, setEditMode]);

  // Mouse selection handlers (Excel-like range dragging)
  const handleCellMouseDown = (r, c, e) => {
    if (e.button !== 0) return; // Left click only

    if (editMode && (selectedCell.row !== r || selectedCell.col !== c)) {
      commitEdit();
    }

    if (e.shiftKey) {
      // Shift+Click range expansion
      const anchorRow = dragAnchor ? dragAnchor.row : selectedCell.row;
      const anchorCol = dragAnchor ? dragAnchor.col : selectedCell.col;
      setSelectedRange(
        Math.min(anchorRow, r),
        Math.min(anchorCol, c),
        Math.max(anchorRow, r),
        Math.max(anchorCol, c)
      );
    } else {
      setIsMouseDown(true);
      setDragAnchor({ row: r, col: c });
      setSelectedCell(r, c);
      setSelectedRange(r, c, r, c);
    }
  };

  const handleCellMouseEnter = (r, c) => {
    if (!isMouseDown || !dragAnchor) return;
    setSelectedRange(
      Math.min(dragAnchor.row, r),
      Math.min(dragAnchor.col, c),
      Math.max(dragAnchor.row, r),
      Math.max(dragAnchor.col, c)
    );
  };

  const handleMouseUp = () => {
    setIsMouseDown(false);
  };

  useEffect(() => {
    window.addEventListener('mouseup', handleMouseUp);
    return () => window.removeEventListener('mouseup', handleMouseUp);
  }, []);

  // Header click selection (Whole row / Whole column)
  const handleColumnHeaderClick = (c) => {
    if (editMode) commitEdit();
    setSelectedCell(0, c);
    setSelectedRange(0, c, ROW_COUNT - 1, c);
  };

  const handleRowHeaderClick = (r) => {
    if (editMode) commitEdit();
    setSelectedCell(r, 0);
    setSelectedRange(r, 0, r, COL_COUNT - 1);
  };

  const handleCornerClick = () => {
    if (editMode) commitEdit();
    setSelectedCell(0, 0);
    setSelectedRange(0, 0, ROW_COUNT - 1, COL_COUNT - 1);
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.target.tagName === 'INPUT' && e.target !== inputRef.current) return;
      if (e.target.tagName === 'TEXTAREA') return;

      const { row, col } = selectedCell;

      if (editMode) {
        if (e.key === 'Enter') {
          e.preventDefault();
          commitEdit();
          setSelectedCell(Math.min(ROW_COUNT - 1, row + 1), col);
        } else if (e.key === 'Tab') {
          e.preventDefault();
          commitEdit();
          setSelectedCell(row, Math.min(COL_COUNT - 1, col + 1));
        } else if (e.key === 'Escape') {
          e.preventDefault();
          setEditMode(false);
        }
        return;
      }

      // Undo / Redo keyboard shortcuts (Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z)
      if (e.ctrlKey || e.metaKey) {
        if ((e.key === 'z' || e.key === 'Z') && !e.shiftKey) {
          e.preventDefault();
          useGridStore.getState().undo();
          return;
        }
        if ((e.key === 'y' || e.key === 'Y') || (e.shiftKey && (e.key === 'z' || e.key === 'Z'))) {
          e.preventDefault();
          useGridStore.getState().redo();
          return;
        }
      }

      if (e.shiftKey) {
        // Shift+Arrow Range Selection
        const curRange = selectedRange || { startRow: row, endRow: row, startCol: col, endCol: col };
        let newEndRow = curRange.endRow;
        let newEndCol = curRange.endCol;

        if (e.key === 'ArrowUp') {
          e.preventDefault();
          newEndRow = Math.max(0, curRange.endRow - 1);
          setSelectedRange(curRange.startRow, curRange.startCol, newEndRow, newEndCol);
        } else if (e.key === 'ArrowDown') {
          e.preventDefault();
          newEndRow = Math.min(ROW_COUNT - 1, curRange.endRow + 1);
          setSelectedRange(curRange.startRow, curRange.startCol, newEndRow, newEndCol);
        } else if (e.key === 'ArrowLeft') {
          e.preventDefault();
          newEndCol = Math.max(0, curRange.endCol - 1);
          setSelectedRange(curRange.startRow, curRange.startCol, newEndRow, newEndCol);
        } else if (e.key === 'ArrowRight') {
          e.preventDefault();
          newEndCol = Math.min(COL_COUNT - 1, curRange.endCol + 1);
          setSelectedRange(curRange.startRow, curRange.startCol, newEndRow, newEndCol);
        }
        return;
      }

      switch (e.key) {
        case 'ArrowUp':
          e.preventDefault();
          setSelectedCell(Math.max(0, row - 1), col);
          break;
        case 'ArrowDown':
          e.preventDefault();
          setSelectedCell(Math.min(ROW_COUNT - 1, row + 1), col);
          break;
        case 'ArrowLeft':
          e.preventDefault();
          setSelectedCell(row, Math.max(0, col - 1));
          break;
        case 'ArrowRight':
          e.preventDefault();
          setSelectedCell(row, Math.min(COL_COUNT - 1, col + 1));
          break;
        case 'Tab':
          e.preventDefault();
          if (e.shiftKey) {
            setSelectedCell(row, Math.max(0, col - 1));
          } else {
            setSelectedCell(row, Math.min(COL_COUNT - 1, col + 1));
          }
          break;
        case 'Enter':
          e.preventDefault();
          if (e.shiftKey) {
            setSelectedCell(Math.max(0, row - 1), col);
          } else {
            setSelectedCell(Math.min(ROW_COUNT - 1, row + 1), col);
          }
          break;
        case 'Delete':
        case 'Backspace':
          e.preventDefault();
          if (isCellProtected(activeSheetId, row, col, currentUserId, userRole)) {
            showToast('🔒 This cell is protected and cannot be edited by your account.', 'warning');
            return;
          }
          updateCellValue(row, col, '', activeSheetId);
          break;
        case 'F2':
          e.preventDefault();
          startEdit();
          break;
        default:
          if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
            startEdit(e.key);
          }
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedCell, selectedRange, editMode, commitEdit, setSelectedCell, setSelectedRange, startEdit, updateCellValue, activeSheetId, setEditMode]);

  // Compute cell style & formatting
  const getCellStyle = (cell) => {
    if (!cell) return {};
    let f = cell.format || {};
    if (typeof f === 'string') {
      try {
        f = JSON.parse(f);
      } catch (e) {
        f = {};
      }
    }
    const style = {};
    if (f.bold) style.fontWeight = '700';
    if (f.italic) style.fontStyle = 'italic';
    if (f.underline && f.strike) style.textDecoration = 'underline line-through';
    else if (f.underline) style.textDecoration = 'underline';
    else if (f.strike) style.textDecoration = 'line-through';
    if (f.align) style.textAlign = f.align;
    else if (cell.dataType === 'NUMBER') style.textAlign = 'right';
    if (f.textColor) style.color = f.textColor;
    if (f.backgroundColor) style.backgroundColor = f.backgroundColor;
    return style;
  };

  // Format display value (currency, %, number)
  const getFormattedValue = (cell) => {
    if (!cell || cell.value === undefined || cell.value === null) return '';
    const val = cell.value;
    const num = parseFloat(val);

    let f = cell.format || {};
    if (typeof f === 'string') {
      try { f = JSON.parse(f); } catch(e) { f = {}; }
    }

    if (!isNaN(num) && f.numberFormat) {
      if (f.numberFormat === 'currency') {
        return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
      }
      if (f.numberFormat === 'percent') {
        return (num * 100).toFixed(1) + '%';
      }
      if (f.numberFormat === 'number') {
        return num.toLocaleString('en-US');
      }
    }
    return val;
  };

  const isCellInRange = (r, c) => {
    if (!selectedRange) return false;
    const minR = Math.min(selectedRange.startRow, selectedRange.endRow);
    const maxR = Math.max(selectedRange.startRow, selectedRange.endRow);
    const minC = Math.min(selectedRange.startCol, selectedRange.endCol);
    const maxC = Math.max(selectedRange.startCol, selectedRange.endCol);
    return r >= minR && r <= maxR && c >= minC && c <= maxC;
  };

  const isColSelected = (c) => {
    if (!selectedRange) return selectedCell.col === c;
    const minC = Math.min(selectedRange.startCol, selectedRange.endCol);
    const maxC = Math.max(selectedRange.startCol, selectedRange.endCol);
    return c >= minC && c <= maxC;
  };

  const isRowSelected = (r) => {
    if (!selectedRange) return selectedCell.row === r;
    const minR = Math.min(selectedRange.startRow, selectedRange.endRow);
    const maxR = Math.max(selectedRange.startRow, selectedRange.endRow);
    return r >= minR && r <= maxR;
  };

  const isMultiCellRange = selectedRange &&
    (selectedRange.startRow !== selectedRange.endRow || selectedRange.startCol !== selectedRange.endCol);

  return (
    <div
      ref={gridRef}
      className="grid-viewport"
      tabIndex={0}
      style={{
        '--user-color': userColor,
      }}
    >
      <table className="spreadsheet-table">
        <colgroup>
          <col style={{ width: '48px' }} />
          {Array.from({ length: COL_COUNT }).map((_, c) => (
            <col key={c} style={{ width: `${DEFAULT_COL_WIDTH}px` }} />
          ))}
        </colgroup>

        {/* Column Headers (A, B, C...) */}
        <thead>
          <tr>
            <th className="corner-header" onClick={handleCornerClick} title="Select All" />
            {Array.from({ length: COL_COUNT }).map((_, c) => (
              <th
                key={c}
                onClick={() => handleColumnHeaderClick(c)}
                className={`col-header ${isColSelected(c) ? 'active' : ''}`}
                style={{ cursor: 'pointer', userSelect: 'none' }}
                title={`Select Column ${indexToColLetter(c)}`}
              >
                {indexToColLetter(c)}
              </th>
            ))}
          </tr>
        </thead>

        {/* Grid Cells */}
        <tbody>
          {Array.from({ length: ROW_COUNT }).map((_, r) => (
            <tr key={r}>
              {/* Row Header (1, 2, 3...) */}
              <th
                onClick={() => handleRowHeaderClick(r)}
                className={`row-header ${isRowSelected(r) ? 'active' : ''}`}
                style={{ cursor: 'pointer', userSelect: 'none' }}
                title={`Select Row ${r + 1}`}
              >
                {r + 1}
              </th>

              {/* Cells */}
              {Array.from({ length: COL_COUNT }).map((_, c) => {
                const key = `${r}:${c}`;
                const cell = cells[key];
                const isFocused = selectedCell.row === r && selectedCell.col === c;
                const inRange = isCellInRange(r, c);
                const hasComment = commentedCells.has(key);

                const minR = selectedRange ? Math.min(selectedRange.startRow, selectedRange.endRow) : selectedCell.row;
                const maxR = selectedRange ? Math.max(selectedRange.startRow, selectedRange.endRow) : selectedCell.row;
                const minC = selectedRange ? Math.min(selectedRange.startCol, selectedRange.endCol) : selectedCell.col;
                const maxC = selectedRange ? Math.max(selectedRange.startCol, selectedRange.endCol) : selectedCell.col;

                const isRangeTop = inRange && r === minR;
                const isRangeBottom = inRange && r === maxR;
                const isRangeLeft = inRange && c === minC;
                const isRangeRight = inRange && c === maxC;
                const isRangeBottomRight = inRange && r === maxR && c === maxC;

                const remoteUsersOnCell = cursorList.filter(
                  (cur) =>
                    cur &&
                    cur.row === r &&
                    cur.col === c
                );

                return (
                  <td
                    key={c}
                    onMouseDown={(e) => handleCellMouseDown(r, c, e)}
                    onMouseEnter={() => handleCellMouseEnter(r, c)}
                    onDoubleClick={() => startEdit()}
                    style={getCellStyle(cell)}
                    className={`grid-cell ${isFocused ? 'selected' : ''} ${inRange && !isFocused ? 'in-range' : ''} ${
                      isRangeTop ? 'cell-range-border-top' : ''
                    } ${isRangeBottom ? 'cell-range-border-bottom' : ''} ${
                      isRangeLeft ? 'cell-range-border-left' : ''
                    } ${isRangeRight ? 'cell-range-border-right' : ''}`}
                  >
                    {/* In-Cell Input */}
                    {isFocused && editMode ? (
                      <input
                        ref={inputRef}
                        type="text"
                        value={editValue}
                        onChange={(e) => {
                          setEditValue(e.target.value);
                          useGridStore.getState().setFormulaBarValue(e.target.value);
                        }}
                        onBlur={commitEdit}
                        className="cell-inline-input"
                      />
                    ) : (
                      <span style={{ display: 'block', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                        {getFormattedValue(cell)}
                      </span>
                    )}

                    {/* Single Cell Active Outline */}
                    {isFocused && !isMultiCellRange && !editMode && (
                      <div className="cell-selection-outline">
                        <div className="cell-fill-handle" />
                      </div>
                    )}

                    {/* Range Selection Drag Handle on Bottom-Right */}
                    {isMultiCellRange && isRangeBottomRight && !editMode && (
                      <div className="cell-fill-handle" />
                    )}

                    {/* Remote Collaborator Cursors & Name Tags */}
                    {remoteUsersOnCell.map((u) => (
                      <div
                        key={u.userId}
                        style={{
                          borderColor: u.color || '#3b82f6',
                          backgroundColor: `${u.color || '#3b82f6'}1a`
                        }}
                        className="remote-cursor-box"
                      >
                        <div style={{ backgroundColor: u.color || '#3b82f6' }} className="remote-cursor-tag">
                          <span>{cursorDisplayName[u.userId] || u.userName || 'Collaborator'}</span>
                        </div>
                      </div>
                    ))}

                    {/* Comment Indicator Badge */}
                    {hasComment && (
                      <div
                        className="cell-comment-badge"
                        onClick={(e) => {
                          e.stopPropagation();
                          openCellComment(r, c);
                        }}
                        style={{ cursor: 'pointer' }}
                        title="Click to view comments on this cell"
                      />
                    )}

                    {/* Protected Cell Indicator — subtle stripe overlay + lock icon */}
                    {isCellProtected(activeSheetId, r, c, currentUserId, userRole) && (
                      <>
                        <div className="cell-protected-overlay" />
                        <div
                          style={{
                            position: 'absolute',
                            bottom: '2px',
                            right: '3px',
                            color: '#0284c7',
                            pointerEvents: 'none',
                            opacity: 0.8,
                            display: 'flex',
                            alignItems: 'center',
                            zIndex: 10,
                          }}
                          title="Protected Range — locked from unauthorized editing"
                        >
                          <Lock size={9} />
                        </div>
                      </>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}


