import React, { useMemo } from 'react';
import { useGridStore } from '../store/useGridStore';

export default function StatusBar() {
  const { cells, selectedCell, selectedRange } = useGridStore();

  const stats = useMemo(() => {
    const startRow = selectedRange ? selectedRange.startRow : selectedCell.row;
    const endRow = selectedRange ? selectedRange.endRow : selectedCell.row;
    const startCol = selectedRange ? selectedRange.startCol : selectedCell.col;
    const endCol = selectedRange ? selectedRange.endCol : selectedCell.col;

    let sum = 0;
    let count = 0;
    let numCount = 0;

    for (let r = startRow; r <= endRow; r++) {
      for (let c = startCol; c <= endCol; c++) {
        const key = `${r}:${c}`;
        const cell = cells[key];
        if (cell && cell.value !== undefined && cell.value !== '') {
          count++;
          const num = parseFloat(cell.value);
          if (!isNaN(num)) {
            sum += num;
            numCount++;
          }
        }
      }
    }

    if (count === 0) return null;

    if (numCount > 0) {
      return {
        label: 'SUM',
        value: sum.toLocaleString('en-US', { maximumFractionDigits: 2 }),
        count
      };
    }

    return {
      label: 'COUNT',
      value: count,
      count
    };
  }, [cells, selectedCell, selectedRange]);

  if (!stats) return null;

  return (
    <div className="google-aggregate-pill">
      <span style={{ color: '#5f6368', marginRight: '4px' }}>{stats.label}:</span>
      <span style={{ color: '#1f1f1f', fontWeight: '500' }}>{stats.value}</span>
    </div>
  );
}
