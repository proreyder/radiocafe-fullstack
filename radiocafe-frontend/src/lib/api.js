import { tabId, tabHeader } from './tabSession';
const BASE = '/api/v1';
let csrf = null;
async function token() {
  if (!csrf) {
    const res = await fetch(`${BASE}/auth/csrf`, { credentials: 'include', headers: { [tabHeader]: tabId() } });
    if (!res.ok) throw new Error('No se pudo conectar con RadioCafé. Comprueba que el backend esté iniciado.');
    csrf = await res.json();
  }
  return csrf;
}
export async function api(path, { method = 'GET', body, responseType = 'json', ...options } = {}) {
  // A login in a duplicated window must not replace the source window's account.
  if (path === '/auth/login' && method === 'POST') { tabId({ fresh: true }); csrf = null; }
  const headers = { ...options.headers, [tabHeader]: tabId() };
  const multipart = body instanceof FormData;
  if (body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD'].includes(method)) {
    const t = await token();
    headers[t.headerName] = t.token;
  }
  let response;
  try {
    response = await fetch(`${BASE}${path}`, { ...options, method, credentials: 'include', headers, body: body === undefined ? undefined : multipart ? body : JSON.stringify(body) });
  } catch { throw new Error('No se pudo conectar con el servidor. Comprueba tu conexión.'); }
  if (!response.ok) {
    if (response.status === 403 && headers[tabHeader] === tabId()) csrf = null;
    if (response.status === 401 && headers[tabHeader] === tabId()) window.dispatchEvent(new Event('session-expired'));
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || 'No se pudo completar la operación.');
  }
  if (path === '/auth/login' || path === '/auth/logout' || path === '/account/password') csrf = null;
  return response.status === 204 ? null : responseType === 'blob' ? response.blob() : response.json();
}
export const money = value => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'USD' }).format(value || 0);
export const staffRoles = ['ADMINISTRADOR', 'BARISTA', 'REPARTIDOR'];
