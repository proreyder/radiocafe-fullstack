import { money, api } from '../lib/api';
const labels = { PENDIENTE: 'Pendiente', EN_PREPARACION: 'En preparación', LISTO: 'Listo', EN_CAMINO: 'En camino', ENTREGADO: 'Entregado', CANCELLED: 'Cancelado' };
function nextStates(order, role) {
  let states = { PENDIENTE: ['EN_PREPARACION', 'CANCELLED'], EN_PREPARACION: ['LISTO', 'CANCELLED'], LISTO: [order.tipoEntrega === 'DELIVERY' ? 'EN_CAMINO' : 'ENTREGADO'], EN_CAMINO: ['ENTREGADO'] }[order.estadoPedido] || [];
  if (role === 'BARISTA') states = states.filter(s => s !== 'EN_CAMINO' && !(s === 'ENTREGADO' && order.tipoEntrega === 'DELIVERY'));
  if (role === 'REPARTIDOR') states = states.filter(s => ['EN_CAMINO', 'ENTREGADO'].includes(s));
  if (role === 'CLIENTE') states = [];
  return states;
}
export default function Orders({ orders, role, refresh, action, busy }) {
  async function change(id, state) { await api(`/orders/${id}/status`, { method: 'PATCH', body: { nuevoEstado: state } }); await refresh(); }
  return <section><div className="section-heading"><div><p className="eyebrow">SEGUIMIENTO</p><h2>{role === 'CLIENTE' ? 'Mis pedidos' : 'Pedidos de la cafetería'}</h2></div><button className="button secondary" disabled={busy} onClick={() => action(refresh)}>Actualizar</button></div>
    {orders.length === 0 ? <p className="empty">Todavía no hay pedidos.</p> : <div className="stack">{[...orders].sort((a,b) => b.idPedido - a.idPedido).map(o => <article className="card order" key={o.idPedido}>
      <div className="section-heading"><strong>Pedido #{o.idPedido}</strong><span className="badge">{labels[o.estadoPedido]}</span></div>
      <p className="muted">{o.fechaCreacion ? new Date(o.fechaCreacion).toLocaleString('es-PE') : ''} · {o.tipoEntrega === 'DELIVERY' ? 'Delivery' : 'Recojo en local'}</p>
      {o.tipoEntrega === 'DELIVERY' && <p>{o.direccionEntrega}</p>}
      <ul className="order-lines">{o.detalles.map(d => <li key={d.idDetalle}><span>{d.cantidad} × {d.nombreProducto}{d.personalizacionDescripcion && <small className="order-line-configuration">{d.personalizacionDescripcion}</small>}</span><strong>{money(d.subtotalLinea)}</strong></li>)}</ul>
      <div className="totals"><span>Envío: {money(o.costoEnvio)}</span><strong>Total: {money(o.total)}</strong></div>
      <div className="actions">{nextStates(o, role).map(s => <button className="button secondary" disabled={busy} key={s} onClick={() => action(() => change(o.idPedido, s))}>{labels[s]}</button>)}</div>
    </article>)}</div>}
  </section>;
}
