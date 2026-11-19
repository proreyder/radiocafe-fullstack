const BASE = '/api/v1';
let csrf = null;
async function token() {
  if (!csrf) {
    const res = await fetch(`${BASE}/auth/csrf`, { credentials: 'include' });
    if (!res.ok) throw new Error('No se pudo conectar con RadioCafé. Comprueba que el backend esté iniciado.');
    csrf = await res.json();
  }
  return csrf;
}
export async function api(path, { method = 'GET', body, ...options } = {}) {
  const headers = { ...options.headers };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD'].includes(method)) {
    const t = await token();
    headers[t.headerName] = t.token;
  }
  let response;
  try {
    response = await fetch(`${BASE}${path}`, { ...options, method, credentials: 'include', headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch { throw new Error('No se pudo conectar con el servidor. Comprueba tu conexión.'); }
  if (!response.ok) {
    if (response.status === 403) csrf = null;
    if (response.status === 401) window.dispatchEvent(new Event('session-expired'));
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || 'No se pudo completar la operación.');
  }
  if (path === '/auth/login' || path === '/auth/logout') csrf = null;
  return response.status === 204 ? null : response.json();
}
export const money = value => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'USD' }).format(value || 0);
export const staffRoles = ['ADMINISTRADOR', 'BARISTA', 'REPARTIDOR'];
