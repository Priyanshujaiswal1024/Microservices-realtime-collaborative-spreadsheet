import React, { useState } from 'react';
import { useGridStore } from '../store/useGridStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useUIStore } from '../store/useUIStore';
import GoogleColorPalette from './GoogleColorPalette';
import {
  Bold,
  Italic,
  Strikethrough,
  AlignLeft,
  AlignCenter,
  AlignRight,
  DollarSign,
  Percent,
  MessageSquarePlus,
  ShieldCheck,
  Palette,
  RotateCcw,
  RotateCw,
  ChevronDown,
  Trash2
} from 'lucide-react';

export default function Toolbar() {
  const { formatSelection, selectedCell, cells, undo, redo, clearGrid, clearSheet } = useGridStore();
  const { activeSheetId } = useWorkbookStore();
  const { openCellComment, openModal, showConfirm } = useUIStore();

  const [activePalette, setActivePalette] = useState(null); // 'text' | 'fill' | null

  const key = `${selectedCell.row}:${selectedCell.col}`;
  const currentFormat = cells[key]?.format || {};

  const toggleBold = () => {
    formatSelection({ bold: !currentFormat.bold }, activeSheetId);
  };

  const toggleItalic = () => {
    formatSelection({ italic: !currentFormat.italic }, activeSheetId);
  };

  const toggleStrike = () => {
    formatSelection({ strike: !currentFormat.strike }, activeSheetId);
  };

  const setAlign = (align) => {
    formatSelection({ align }, activeSheetId);
  };

  const setNumberFormat = (numberFormat) => {
    formatSelection({ numberFormat }, activeSheetId);
  };

  const setTextColor = (color) => {
    formatSelection({ textColor: color }, activeSheetId);
  };

  const previewTextColor = (color) => {
    formatSelection({ textColor: color }, activeSheetId, true);
  };

  const setBgColor = (backgroundColor) => {
    formatSelection({ backgroundColor }, activeSheetId);
  };

  const previewBgColor = (backgroundColor) => {
    formatSelection({ backgroundColor }, activeSheetId, true);
  };

  return (
    <div className="google-toolbar-wrapper">
      <div className="google-toolbar-capsule">
        {/* Undo / Redo */}
        <button onClick={undo} title="Undo (Ctrl+Z)" className="google-tool-btn">
          <RotateCcw size={14} />
        </button>
        <button onClick={redo} title="Redo (Ctrl+Y)" className="google-tool-btn">
          <RotateCw size={14} />
        </button>

        <div className="google-toolbar-divider" />

        {/* Number Formats: Currency & Percent */}
        <button
          onClick={() => setNumberFormat(currentFormat.numberFormat === 'currency' ? null : 'currency')}
          title="Format as currency ($)"
          className={`google-tool-btn ${currentFormat.numberFormat === 'currency' ? 'active' : ''}`}
        >
          <DollarSign size={14} />
        </button>

        <button
          onClick={() => setNumberFormat(currentFormat.numberFormat === 'percent' ? null : 'percent')}
          title="Format as percent (%)"
          className={`google-tool-btn ${currentFormat.numberFormat === 'percent' ? 'active' : ''}`}
        >
          <Percent size={14} />
        </button>

        <div className="google-toolbar-divider" />

        {/* Text Styling: Bold, Italic, Strikethrough */}
        <button
          onClick={toggleBold}
          title="Bold (Ctrl+B)"
          className={`google-tool-btn font-bold ${currentFormat.bold ? 'active' : ''}`}
        >
          <Bold size={14} strokeWidth={2.5} />
        </button>

        <button
          onClick={toggleItalic}
          title="Italic (Ctrl+I)"
          className={`google-tool-btn ${currentFormat.italic ? 'active' : ''}`}
        >
          <Italic size={14} />
        </button>

        <button
          onClick={toggleStrike}
          title="Strikethrough (Alt+Shift+5)"
          className={`google-tool-btn ${currentFormat.strike ? 'active' : ''}`}
        >
          <Strikethrough size={14} />
        </button>

        <div className="google-toolbar-divider" />

        {/* Authentic Google Text Color Dropdown */}
        <div style={{ position: 'relative' }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setActivePalette(activePalette === 'text' ? null : 'text');
            }}
            title="Text color"
            className={`google-tool-btn ${activePalette === 'text' ? 'active' : ''}`}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '1px' }}
          >
            <span style={{ fontSize: '13px', fontWeight: 'bold', lineHeight: 1 }}>A</span>
            <div
              style={{
                width: '14px',
                height: '3px',
                borderRadius: '1px',
                backgroundColor: currentFormat.textColor || '#000000'
              }}
            />
          </button>
          {activePalette === 'text' && (
            <GoogleColorPalette
              currentColor={currentFormat.textColor}
              onSelectColor={(col) => setTextColor(col)}
              onClose={() => setActivePalette(null)}
              title="Text color"
            />
          )}
        </div>

        {/* Authentic Google Fill Color (Background) Dropdown */}
        <div style={{ position: 'relative' }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setActivePalette(activePalette === 'fill' ? null : 'fill');
            }}
            title="Fill color (Background)"
            className={`google-tool-btn ${activePalette === 'fill' ? 'active' : ''}`}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '1px' }}
          >
            <Palette size={13} />
            <div
              style={{
                width: '14px',
                height: '3px',
                borderRadius: '1px',
                backgroundColor: currentFormat.backgroundColor || '#ffffff',
                border: !currentFormat.backgroundColor ? '1px solid #dadce0' : 'none'
              }}
            />
          </button>
          {activePalette === 'fill' && (
            <GoogleColorPalette
              currentColor={currentFormat.backgroundColor}
              onSelectColor={(col) => setBgColor(col)}
              onClose={() => setActivePalette(null)}
              title="Fill color"
            />
          )}
        </div>

        <div className="google-toolbar-divider" />

        {/* Alignment */}
        <button
          onClick={() => setAlign(currentFormat.align === 'left' ? 'center' : currentFormat.align === 'center' ? 'right' : 'left')}
          title={`Align text (${currentFormat.align || 'left'})`}
          className="google-tool-select-btn"
        >
          {currentFormat.align === 'center' ? (
            <AlignCenter size={14} />
          ) : currentFormat.align === 'right' ? (
            <AlignRight size={14} />
          ) : (
            <AlignLeft size={14} />
          )}
          <ChevronDown size={11} />
        </button>

        <div className="google-toolbar-divider" />

        {/* Real Action: Insert Comment on active cell */}
        <button
          onClick={() => openCellComment(selectedCell.row, selectedCell.col)}
          title="Add comment to cell (Ctrl+Alt+M)"
          className="google-tool-btn"
        >
          <MessageSquarePlus size={14} />
        </button>

        {/* Real Action: Protect Range */}
        <button
          onClick={() => openModal('protect')}
          title="Protect sheets and ranges"
          className="google-tool-btn"
        >
          <ShieldCheck size={14} />
        </button>

        <div className="google-toolbar-divider" />

        {/* Real Action: Clear All Cells */}
        <button
          onClick={() => {
            showConfirm({
              title: 'Clear sheet?',
              message: 'Are you sure you want to clear all cells in this sheet? This cannot be undone.',
              confirmText: 'Clear',
              cancelText: 'Cancel',
              isDestructive: true,
              onConfirm: () => clearSheet(activeSheetId),
            });
          }}
          title="Clear all cells in sheet"
          className="google-tool-btn"
          style={{ color: '#d93025' }}
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  );
}

