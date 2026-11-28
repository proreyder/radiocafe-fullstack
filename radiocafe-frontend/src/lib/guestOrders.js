const key = 'radiocafe.guest.orders';
export function guestOrders() {
  try { const items=JSON.parse(sessionStorage.getItem(key)||'[]');return Array.isArray(items)?items.filter(x=>Number.isInteger(x.id)&&typeof x.token==='string'&&/^[A-Za-z0-9_-]{43}$/.test(x.token)).slice(0,20):[]; } catch { return []; }
}
export function rememberGuestOrder(id,token) {
  try { sessionStorage.setItem(key,JSON.stringify([{id,token},...guestOrders().filter(x=>x.id!==id)].slice(0,20))); } catch { /* El enlace privado sigue disponible en la confirmación. */ }
}
export function trackingPath(id,token) { return `/pedido/${id}#${token}`; }
