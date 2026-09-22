// Global temporary storage for title and planned action before login redirect
let _pendingTitleAfterLogin = null;
let _pendingActionAfterLogin = null; // 'save' | 'saveAndShare'

export function setPendingSave(title, action) {
  _pendingTitleAfterLogin = title;
  _pendingActionAfterLogin = action;
}

export function consumePendingAfterLogin() {
  const data = { title: _pendingTitleAfterLogin, action: _pendingActionAfterLogin };
  _pendingTitleAfterLogin = null;
  _pendingActionAfterLogin = null;
  return data;
}
