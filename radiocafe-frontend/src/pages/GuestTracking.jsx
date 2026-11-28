import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { api, money } from '../lib/api';
import { guestOrders, rememberGuestOrder, trackingPath } from '../lib/guestOrders';
import Layout from '../components/Layout';
import './GuestCheckout.css';
const labels={PENDIENTE:'Pedido recibido',EN_PREPARACION:'Estamos preparando tu pedido',LISTO:'Tu pedido está listo',EN_CAMINO:'Tu pedido está en camino',ENTREGADO:'Pedido entregado',CANCELLED:'Pedido cancelado'};
export default function GuestTracking() {
 const {id}=useParams();const {hash}=useLocation();
 const token=hash.slice(1)||guestOrders().find(x=>x.id===Number(id))?.token||'';
 const [order,setOrder]=useState(null),[error,setError]=useState(''),[copied,setCopied]=useState(false);
 useEffect(()=>{
  if(!id)return;
  let active=true;const controller=new AbortController();setOrder(null);setError('');
  const load=async()=>{try{const data=await api(`/guest/orders/${id}`,{headers:{'X-Order-Token':token},signal:controller.signal});if(active){setOrder(data);setError('');rememberGuestOrder(data.idPedido,token);}}catch(e){if(active)setError(e.message);}};
  void load();const timer=setInterval(()=>{if(!document.hidden)void load();},8000);window.addEventListener('focus',load);
  return()=>{active=false;controller.abort();clearInterval(timer);window.removeEventListener('focus',load);};
 },[id,token]);
 async function copy(){try{await navigator.clipboard.writeText(`${window.location.origin}${trackingPath(Number(id),token)}`);setCopied(true);}catch{setCopied(false);}}
 return <Layout variant="ordering"><main className="guest-tracking">
  <p className="eyebrow">TU PEDIDO EN RADIOCAFÉ</p><h1>{id?`Seguimiento del pedido #${id}`:'Mis pedidos como invitado'}</h1>
  {!id?<><p>Estos enlaces se guardan en esta ventana. Conserva el enlace privado de cada pedido para abrirlo en otro dispositivo.</p><div className="stack">{guestOrders().map(x=><Link className="card" key={x.id} to={trackingPath(x.id,x.token)}>Ver pedido #{x.id} →</Link>)}{!guestOrders().length&&<p className="empty">Aún no hay pedidos de invitado guardados en esta ventana.</p>}</div></>:<>
   {error&&<p className="notice error" role="alert">{error} Comprueba que tienes el enlace privado completo.</p>}
   {!order&&!error&&<p role="status">Consultando tu pedido…</p>}
   {order&&<><div className="guest-status card" role="status" aria-live="polite"><span className="eyebrow">ESTADO ACTUAL</span><h2>{labels[order.estadoPedido]||order.estadoPedido}</h2>{order.preparacionEstimada&&<p>{order.nombreBarista} estimó {order.minutosPreparacion} minutos. Hora de preparación: <strong>{new Date(order.preparacionEstimada).toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit',timeZone:'America/Lima'})}</strong> (Lima).</p>}<small>El seguimiento se actualiza automáticamente cada 8 segundos mientras esta página está visible.</small></div>
    <section className="card stack"><h2>Resumen de tu pedido</h2><p>{order.tipoEntrega==='DELIVERY'?'Delivery · Pago al recibir':'Recojo en local · Pago al recoger'}</p><ul className="order-lines">{order.detalles.map((d,i)=><li key={i}><span>{d.cantidad} × {d.nombreProducto}{d.personalizacionDescripcion&&<small className="order-line-configuration">{d.personalizacionDescripcion}</small>}</span><strong>{money(d.subtotalLinea)}</strong></li>)}</ul><p>Subtotal: {money(order.subtotal)} · Envío: {money(order.costoEnvio)}{order.descuento>0&&` · Descuento: −${money(order.descuento)}`}</p><strong>Total: {money(order.total)}</strong><p>El pago se realiza al entregar el pedido. Esta confirmación no acredita un pago.</p></section>
    <section className="card stack"><h2>Guarda tu enlace privado</h2><p>Quien tenga este enlace podrá consultar el estado de este pedido. Guárdalo antes de cerrar la ventana.</p><label>Enlace de seguimiento<input readOnly value={`${window.location.origin}${trackingPath(Number(id),token)}`} onFocus={e=>e.target.select()}/></label><button className="button secondary" onClick={copy}>{copied?'Enlace copiado':'Copiar enlace'}</button></section>
    <section className="guest-account-invite"><h2>¿Quieres una cuenta para tu próxima visita?</h2><p>Es opcional: guarda tus direcciones, consulta tus próximos pedidos y participa en Kappu.</p><Link className="button secondary" to="/?acceso=registro">Crear una cuenta</Link></section></>}
  </>}
  <Link className="button" to="/catalogo">Seguir explorando la carta</Link>
 </main></Layout>;
}
