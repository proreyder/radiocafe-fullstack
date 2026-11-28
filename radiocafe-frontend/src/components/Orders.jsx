import { useState } from 'react';
import '../pages/GuestCheckout.css';
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
  const [minutes, setMinutes] = useState({});
  async function estimate(e,id) {e.preventDefault();await action(async()=>{await api(`/orders/${id}/preparation`,{method:'PATCH',body:{minutos:Number(minutes[id]||15)}});await refresh();},'Tiempo enviado al cliente y preparación iniciada.');}
  async function change(id, state) { await api(`/orders/${id}/status`, { method: 'PATCH', body: { nuevoEstado: state } }); await refresh(); }
  return <section><div className="section-heading"><div><p className="eyebrow">SEGUIMIENTO</p><h2>{role === 'CLIENTE' ? 'Mis pedidos' : 'Pedidos de la cafetería'}</h2></div><button className="button secondary" disabled={busy} onClick={() => action(refresh)}>Actualizar</button></div>
    {orders.length === 0 ? <p className="empty">Todavía no hay pedidos.</p> : <div className="stack">{[...orders].sort((a,b) => b.idPedido - a.idPedido).map(o => <article className="card order" key={o.idPedido}>
      <div className="section-heading"><strong>Pedido #{o.idPedido}</strong><span className="badge">{labels[o.estadoPedido]}</span></div>
      <p className="muted">{o.fechaCreacion ? new Date(o.fechaCreacion).toLocaleString('es-PE') : ''} · {o.tipoEntrega === 'DELIVERY' ? 'Delivery' : 'Recojo en local'}</p>
      {role!=='CLIENTE'&&o.contacto&&<div className="guest-contact-summary"><strong>Invitado: {o.contacto.nombre}</strong><span>Tel. {o.contacto.telefono} · {o.contacto.correo}</span><small>Pago al {o.tipoEntrega==='DELIVERY'?'recibir':'recoger'} · Cobrar al entregar</small></div>}
      {o.tipoEntrega === 'DELIVERY' && <p>{o.direccionEntrega}</p>}
      <ul className="order-lines">{o.detalles.map(d => <li key={d.idDetalle}><span>{d.cantidad} × {d.nombreProducto}{d.personalizacionDescripcion && <small className="order-line-configuration">{d.personalizacionDescripcion}</small>}</span><strong>{money(d.subtotalLinea)}</strong></li>)}</ul>
      <div className="totals">{Number(o.descuento) > 0 && <span>Descuento {o.codigoCupon || o.promocion}: −{money(o.descuento)}</span>}<span>Envío: {money(o.costoEnvio)}</span><strong>Total: {money(o.total)}</strong></div>
      {o.preparacionEstimada && <div className="preparation-estimate"><strong>Preparación estimada: {o.minutosPreparacion} minutos</strong><span>{o.nombreBarista} · Hora estimada: {new Date(o.preparacionEstimada).toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit',timeZone:'America/Lima'})}</span><small>{o.estadoPedido==='ENTREGADO'?'Pedido entregado':o.estadoPedido==='LISTO'?'Tu pedido ya está listo':o.estadoPedido==='CANCELLED'?'Pedido cancelado':'Esta hora es una estimación de preparación.'}</small></div>}
      {['BARISTA','ADMINISTRADOR'].includes(role)&&['PENDIENTE','EN_PREPARACION'].includes(o.estadoPedido)&&<form className="estimate-form" onSubmit={e=>void estimate(e,o.idPedido)}><label>Tiempo estimado de preparación (minutos)<input required type="number" min="1" max="240" step="1" value={minutes[o.idPedido]??15} onChange={e=>setMinutes({...minutes,[o.idPedido]:e.target.value})}/></label><button className="button" disabled={busy}>Enviar tiempo al cliente</button></form>}
      <div className="actions">{nextStates(o, role).map(s => <button className="button secondary" disabled={busy} key={s} onClick={() => action(() => change(o.idPedido, s))}>{labels[s]}</button>)}</div>
    </article>)}</div>}
  </section>;
}
