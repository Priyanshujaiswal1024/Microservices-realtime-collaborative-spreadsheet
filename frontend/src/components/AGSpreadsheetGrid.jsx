import React, { useMemo, useCallback, useRef } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-quartz.css';
import { useGridStore } from '../store/useGridStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useCollabStore } from '../store/useCollabStore';
import { useCommentStore } from '../store/useCommentStore';
import { useAuthStore } from '../store/useAuthStore';
import { indexToColLetter } from '../utils/coordinate';

const ROW_COUNT = 60;
const COL_COUNT = 26;


function SpreadsheetCellRenderer(props) {
  const { value, data, colDef } = props;
  const rowIndex = data?.rowIndex ?? props.rowIndex;
  const colIndex = colDef?.field ? parseInt(colDef.field.replace('col_', ''), 10) : 0;

  const { cells } = useGridStore();
  const { activeSheetId } = useWorkbookStore();
  const { remoteCursors } = useCollabStore();
  const { threads } = useCommentStore();

  const key = `${rowIndex}:${colIndex}`;
  const cell = cells[key];
  const displayVal = cell?.value !== undefined ? cell.value : (value || '');

  // Check if cell has comments
  const hasComment = threads.some(
    (t) => (!t.sheetId || t.sheetId === activeSheetId || !activeSheetId) && Number(t.row) === rowIndex && Number(t.col) === colIndex && !t.resolved
  );

  // Check if any remote collaborator has cursor on this cell
  const remoteUsersOnCell = Object.values(remoteCursors).filter(
    (cur) => cur && cur.row === rowIndex && cur.col === colIndex
  );

  // Compute cell formatting
  const f = cell?.format || {};
  const style = {
    fontWeight: f.bold ? '700' : 'normal',
    fontStyle: f.italic ? 'italic' : 'normal',
    textDecoration: [f.underline ? 'underline' : '', f.strike ? 'line-through' : ''].filter(Boolean).join(' ') || 'none',
    textAlign: f.align || (cell?.dataType === 'NUMBER' ? 'right' : 'left'),
    color: f.textColor || '#0f172a',
    backgroundColor: f.backgroundColor || 'transparent',
    height: '100%',
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    padding: '0 4px',
    position: 'relative',
  };

  return (
    <div style={style} className="relative w-full h-full">
      <span className="truncate w-full">{displayVal}</span>

      {/* Remote Collaborator Cursors & Name Tags */}
      {remoteUsersOnCell.map((u) => (
        <div
          key={u.userId}
          style={{ borderColor: u.color }}
          className="absolute inset-0 border-2 pointer-events-none z-10"
        >
          <div
            style={{ backgroundColor: u.color }}
            className="absolute -top-4 left-0 px-1.5 py-0.5 text-[9px] font-bold text-white rounded shadow"
          >
            {u.userName}
          </div>
        </div>
      ))}

      {/* Comment Marker */}
      {hasComment && (
        <div
          className="absolute top-0 right-0 w-0 h-0 border-t-[8px] border-t-amber-500 border-l-[8px] border-l-transparent pointer-events-none"
          title="Has comments"
        />
      )}
    </div>
  );
}

export default function AGSpreadsheetGrid() {
  const gridRef = useRef(null);
  const { cells, setSelectedCell, updateCellValue } = useGridStore();
  const { activeSheetId, workbook, isCellProtected } = useWorkbookStore();
  const { broadcastCursorMove } = useCollabStore();
  const auth = useAuthStore();

  // Column definitions for AG Grid
  const columnDefs = useMemo(() => {
    const cols = [];

    for (let c = 0; c < COL_COUNT; c++) {
      const colLetter = indexToColLetter(c);
      cols.push({
        headerName: colLetter,
        field: `col_${c}`,
        width: 110,
        editable: (params) => {
          const r = params.node?.rowIndex;
          if (r === undefined || r === null) return true;
          const currentUserId = auth.user?.id || auth.user?.username || auth.clientId;
          const isOwner = (workbook?.ownerId && auth.user?.id && workbook.ownerId === auth.user.id)
            || (workbook?.userRole === 'OWNER' && auth.isAuthenticated);
          const userRole = isOwner ? 'OWNER' : (workbook?.userRole || 'COLLABORATOR');
          return !isCellProtected(activeSheetId, r, c, currentUserId, userRole);
        },
        resizable: true,
        sortable: false,
        cellRenderer: SpreadsheetCellRenderer,
      });
    }
    return cols;
  }, [activeSheetId, workbook, isCellProtected, auth]);

  // Row Data mapped from cells store
  const rowData = useMemo(() => {
    const rows = [];
    for (let r = 0; r < ROW_COUNT; r++) {
      const rowObj = { rowIndex: r };
      for (let c = 0; c < COL_COUNT; c++) {
        const key = `${r}:${c}`;
        rowObj[`col_${c}`] = cells[key]?.value || '';
      }
      rows.push(rowObj);
    }
    return rows;
  }, [cells]);

  // Handle cell value change in AG Grid
  const onCellValueChanged = useCallback(
    (event) => {
      const rowIndex = event.data?.rowIndex;
      const colField = event.colDef.field;
      const colIndex = parseInt(colField.replace('col_', ''), 10);
      const newValue = event.newValue || '';

      if (rowIndex !== undefined && !isNaN(colIndex)) {
        updateCellValue(rowIndex, colIndex, newValue, activeSheetId);
      }
    },
    [activeSheetId, updateCellValue]
  );

  // Handle cell focus / selection to broadcast remote cursor
  const onCellFocused = useCallback(
    (event) => {
      if (event.rowIndex !== null && event.column) {
        const colIndex = parseInt(event.column.getColId().replace('col_', ''), 10);
        if (!isNaN(colIndex)) {
          setSelectedCell(event.rowIndex, colIndex);
          if (activeSheetId) {
            broadcastCursorMove(activeSheetId, event.rowIndex, colIndex);
          }
        }
      }
    },
    [activeSheetId, setSelectedCell, broadcastCursorMove]
  );

  return (
    <div style={{ flex: 1, width: '100%', height: 'calc(100vh - 165px)', minHeight: 400 }} className="ag-theme-quartz">
      <AgGridReact
        ref={gridRef}
        columnDefs={columnDefs}
        rowData={rowData}
        onCellValueChanged={onCellValueChanged}
        onCellFocused={onCellFocused}
        rowHeight={28}
        headerHeight={28}
        suppressCellFocus={false}
        enableCellTextSelection={true}
        animateRows={false}
      />
    </div>
  );
}
