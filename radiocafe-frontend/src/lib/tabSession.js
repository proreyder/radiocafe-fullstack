const KEY = 'radiocafe.window-session';
export const tabHeader = 'X-RadioCafe-Tab';

// This selector is not an authentication token: the HttpOnly cookie is also required.
export function tabId({ fresh = false } = {}) {
  let id = sessionStorage.getItem(KEY);
  if (fresh || !id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(KEY, id);
  }
  return id;
}
