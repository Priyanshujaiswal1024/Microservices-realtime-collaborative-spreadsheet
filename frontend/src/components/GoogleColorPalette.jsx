import React, { useRef, useEffect } from 'react';
import { Pipette, Plus, Check } from 'lucide-react';

const GOOGLE_PALETTE = [
  ['#000000', '#434343', '#666666', '#999999', '#b7b7b7', '#cccccc', '#d9d9d9', '#efefef', '#f3f3f3', '#ffffff'],
  ['#980000', '#ff0000', '#ff9900', '#ffff00', '#00ff00', '#00ffff', '#4a86e8', '#0000ff', '#9900ff', '#ff00ff'],
  ['#e6b8af', '#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3', '#c9daf8', '#cfe2f3', '#d9d2e9', '#ead1dc'],
  ['#dd7e6b', '#ea9999', '#f9cb9c', '#ffe599', '#b6d7a8', '#a2c4c9', '#a4c2f4', '#9fc5e8', '#b4a7d6', '#d5a6bd'],
  ['#cc4125', '#e06666', '#f6b26b', '#ffd966', '#93c47d', '#76a5af', '#6d9eeb', '#6fa8dc', '#8e7cc3', '#c27ba0'],
  ['#a61c00', '#cc0000', '#e69138', '#f1c232', '#6aa84f', '#45818e', '#3c78d8', '#3d85c6', '#674ea7', '#a64d79'],
  ['#85200c', '#990000', '#b45f06', '#bf9000', '#38761d', '#134f5c', '#1155cc', '#0b5394', '#351c75', '#741b47'],
  ['#5b0f00', '#660000', '#783f04', '#7f6000', '#274e13', '#0c343d', '#1c4587', '#073763', '#20124d', '#4c1130'],
];

const STANDARD_COLORS = [
  '#000000', '#ffffff', '#4285f4', '#ea4335', '#fbbc04', '#34a853', '#ff6d01', '#46bdc6'
];

export default function GoogleColorPalette({
  currentColor,
  onSelectColor,
  onHoverColor,
  onClose,
  title = 'Color'
}) {
  const popoverRef = useRef(null);
  const customInputRef = useRef(null);

  // Close when clicking outside (deferred so open click does not immediately close it)
  useEffect(() => {
    let cleanUp = () => {};
    const timer = setTimeout(() => {
      const handleOutsideClick = (e) => {
        if (popoverRef.current && !popoverRef.current.contains(e.target)) {
          if (e.target.closest && e.target.closest('.google-tool-btn')) {
            return;
          }
          if (onHoverColor) {
            onHoverColor(currentColor || '');
          }
          onClose();
        }
      };
      document.addEventListener('mousedown', handleOutsideClick);
      cleanUp = () => document.removeEventListener('mousedown', handleOutsideClick);
    }, 60);

    return () => {
      clearTimeout(timer);
      cleanUp();
    };
  }, [onClose, onHoverColor, currentColor]);

  const handleColorClick = (color) => {
    onSelectColor(color);
    onClose();
  };

  const handleColorHover = (color) => {
    if (onHoverColor) {
      onHoverColor(color);
    }
  };

  const handleReset = () => {
    onSelectColor('');
    onClose();
  };

  return (
    <div
      ref={popoverRef}
      onMouseDown={(e) => e.stopPropagation()}
      onClick={(e) => e.stopPropagation()}
      onMouseLeave={() => {
        if (onHoverColor) {
          onHoverColor(currentColor || '');
        }
      }}
      style={{
        position: 'absolute',
        top: 'calc(100% + 8px)',
        left: '-10px',
        zIndex: 99999,
        background: '#ffffff',
        borderRadius: '8px',
        boxShadow: '0 8px 28px rgba(0,0,0,0.22), 0 0 0 1px rgba(0,0,0,0.1)',
        padding: '12px',
        width: '240px',
        fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        userSelect: 'none',
      }}
    >
      {/* 🚫 Reset Button */}
      <button
        type="button"
        onClick={handleReset}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '6px 10px',
          border: 'none',
          background: 'transparent',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '13px',
          color: '#3c4043',
          fontWeight: 500,
          marginBottom: '8px',
          transition: 'background 0.15s ease'
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = '#f1f3f4';
          handleColorHover('');
        }}
        onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
      >
        <span
          style={{
            display: 'inline-block',
            width: '16px',
            height: '16px',
            borderRadius: '50%',
            border: '1.5px solid #d93025',
            position: 'relative',
            boxSizing: 'border-box'
          }}
        >
          <span
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              width: '12px',
              height: '1.5px',
              background: '#d93025',
              transform: 'translate(-50%, -50%) rotate(-45deg)'
            }}
          />
        </span>
        <span>Reset</span>
        {(!currentColor || currentColor === 'transparent') && (
          <Check size={14} color="#1a73e8" style={{ marginLeft: 'auto' }} />
        )}
      </button>

      {/* Main 10x8 Palette Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(10, 1fr)',
          gap: '4px',
          marginBottom: '10px'
        }}
      >
        {GOOGLE_PALETTE.flat().map((color, idx) => {
          const isSelected = currentColor && currentColor.toLowerCase() === color.toLowerCase();
          return (
            <div
              key={idx}
              onClick={() => handleColorClick(color)}
              title={color}
              style={{
                width: '17px',
                height: '17px',
                borderRadius: '50%',
                backgroundColor: color,
                cursor: 'pointer',
                border: color === '#ffffff' ? '1px solid #dadce0' : '1px solid transparent',
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'transform 0.1s ease, box-shadow 0.1s ease',
                boxSizing: 'border-box'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.25)';
                e.currentTarget.style.boxShadow = '0 0 0 1.5px #1a73e8';
                e.currentTarget.style.zIndex = '2';
                handleColorHover(color);
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
                e.currentTarget.style.boxShadow = 'none';
                e.currentTarget.style.zIndex = '1';
              }}
            >
              {isSelected && (
                <Check
                  size={10}
                  color={
                    color === '#ffffff' || color === '#fce5cd' || color === '#fff2cc' || color === '#d9ead3' || color === '#efefef'
                      ? '#000000'
                      : '#ffffff'
                  }
                  strokeWidth={3}
                />
              )}
            </div>
          );
        })}
      </div>

      <div style={{ height: '1px', background: '#e0e0e0', margin: '8px 0' }} />

      {/* STANDARD Colors Label & Row */}
      <div style={{ fontSize: '11px', fontWeight: 600, color: '#5f6368', letterSpacing: '0.4px', marginBottom: '6px' }}>
        STANDARD
      </div>
      <div style={{ display: 'flex', gap: '6px', alignItems: 'center', marginBottom: '10px' }}>
        {STANDARD_COLORS.map((color, idx) => {
          const isSelected = currentColor && currentColor.toLowerCase() === color.toLowerCase();
          return (
            <div
              key={idx}
              onClick={() => handleColorClick(color)}
              title={color}
              style={{
                width: '19px',
                height: '19px',
                borderRadius: '50%',
                backgroundColor: color,
                cursor: 'pointer',
                border: color === '#ffffff' ? '1px solid #dadce0' : '1px solid transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxSizing: 'border-box'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.2)';
                e.currentTarget.style.boxShadow = '0 0 0 1.5px #1a73e8';
                handleColorHover(color);
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              {isSelected && (
                <Check
                  size={11}
                  color={color === '#ffffff' || color === '#fbbc04' ? '#000000' : '#ffffff'}
                  strokeWidth={3}
                />
              )}
            </div>
          );
        })}
      </div>

      <div style={{ height: '1px', background: '#e0e0e0', margin: '8px 0' }} />

      {/* CUSTOM Color Option */}
      <div style={{ fontSize: '11px', fontWeight: 600, color: '#5f6368', letterSpacing: '0.4px', marginBottom: '6px' }}>
        CUSTOM
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button
          type="button"
          onClick={() => customInputRef.current?.click()}
          title="Custom color picker"
          style={{
            width: '24px',
            height: '24px',
            borderRadius: '50%',
            border: '1px solid #dadce0',
            background: '#ffffff',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#5f6368'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = '#f1f3f4')}
          onMouseLeave={(e) => (e.currentTarget.style.background = '#ffffff')}
        >
          <Plus size={14} />
        </button>

        <span style={{ fontSize: '12px', color: '#5f6368' }}>Add custom color</span>

        {/* Hidden native input triggered by the + button */}
        <input
          ref={customInputRef}
          type="color"
          value={currentColor && currentColor.startsWith('#') ? currentColor : '#4285f4'}
          onInput={(e) => handleColorClick(e.target.value)}
          onChange={(e) => handleColorClick(e.target.value)}
          style={{ opacity: 0, width: 0, height: 0, position: 'absolute', pointerEvents: 'none' }}
        />
      </div>
    </div>
  );
}
