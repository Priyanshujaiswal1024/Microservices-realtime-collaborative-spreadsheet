import React from 'react';
import { useGridStore } from '../store/useGridStore';
import { useWorkbookStore } from '../store/useWorkbookStore';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { toA1Notation } from '../utils/coordinate';
import { Lock } from 'lucide-react';

export default function FormulaBar() {
  const { selectedCell, formulaBarValue, setFormulaBarValue, updateCellValue } = useGridStore();
  const { activeSheetId, isCellProtected, workbook } = useWorkbookStore();
  const { showToast } = useUIStore();
  const auth = useAuthStore();

  const currentUserId = auth.user?.id || auth.user?.username || auth.clientId;
  const isOwner = (workbook?.ownerId && auth.user?.id && workbook.ownerId === auth.user.id)
    || (workbook?.userRole === 'OWNER' && auth.isAuthenticated);
  const userRole = isOwner ? 'OWNER' : (workbook?.userRole || 'COLLABORATOR');
  const isProtected = isCellProtected(activeSheetId, selectedCell.row, selectedCell.col, currentUserId, userRole);

  const cellNotation = toA1Notation(selectedCell.row, selectedCell.col);

  const handleInputChange = (e) => {
    if (isProtected) {
      showToast('🔒 This cell is protected and cannot be edited by your account.', 'warning');
      return;
    }
    const val = e.target.value;
    setFormulaBarValue(val);
    updateCellValue(selectedCell.row, selectedCell.col, val, activeSheetId);
  };

  const insertFormula = (fnName) => {
    if (isProtected) {
      showToast('🔒 This cell is protected and cannot be edited by your account.', 'warning');
      return;
    }
    const defaultFormula = `=${fnName}(A1:A5)`;
    setFormulaBarValue(defaultFormula);
    updateCellValue(selectedCell.row, selectedCell.col, defaultFormula, activeSheetId);
  };

  return (
    <div className="google-formula-bar">
      {/* Name Box (A1) */}
      <div className="google-name-box" title="Name box">
        <span>{cellNotation}</span>
        {isProtected && (
          <Lock size={12} style={{ marginLeft: '4px', color: '#0284c7' }} title="Protected cell" />
        )}
      </div>

      {/* fx Function Symbol */}
      <div className="google-fx-symbol" title="Functions">
        <span>fx</span>
      </div>

      {/* Main Formula Text Input */}
      <div className="google-formula-input-container">
        <input
          type="text"
          value={isProtected ? '' : (formulaBarValue || '')}
          onChange={handleInputChange}
          disabled={isProtected}
          placeholder={isProtected ? '🔒 This cell is protected and locked from editing' : 'Enter a value or formula (=SUM(A1:B10), =A1*2)'}
          className="google-formula-input"
          style={isProtected ? { backgroundColor: '#f8fafc', color: '#94a3b8', cursor: 'not-allowed' } : {}}
        />
      </div>
    </div>
  );
}
