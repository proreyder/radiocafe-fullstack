import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { api, money } from '../lib/api';
import { guestOrders, rememberGuestOrder, trackingPath } from '../lib/guestOrders';
import Layout from '../components/Layout';
import { ShoppingBag, ArrowRight, Check, Coffee, Truck, CreditCard, Copy, Clock3 } from 'lucide-react';
import './GuestCheckout.css';
const labels={PENDIENTE:'Pedido recibido',EN_PREPARACION:'Estamos preparando tu pedido',LISTO:'Tu pedido está listo',EN_CAMINO:'Tu pedido está en camino',ENTREGADO:'Pedido entregado',CANCELLED:'Pedido cancelado'};
export default function GuestTracking() {
 const {id}=useParams();const {hash}=useLocation();
 const token=hash.slice(1)||guestOrders().find(x=>x.id===Number(id))?.token||'';
 const [order,setOrder]=useState(null),[error,setError]=useState(''),[copied,setCopied]=useState(false);
 const [simulated,setSimulated]=useState(false);
 useEffect(()=>{setSimulated(sessionStorage.getItem(`radiocafe.payment.demo.${id}`)==='done');},[id]);
 useEffect(()=>{
  if(!id)return;
  let active=true;const controller=new AbortController();setOrder(null);setError('');
  const load=async()=>{try{const data=await api(`/guest/orders/${id}`,{headers:{'X-Order-Token':token},signal:controller.signal});if(active){setOrder(data);setError('');rememberGuestOrder(data.idPedido,token);}}catch(e){if(active)setError(e.message);}};
  void load();const timer=setInterval(()=>{if(!document.hidden)void load();},8000);window.addEventListener('focus',load);
  return()=>{active=false;controller.abort();clearInterval(timer);window.removeEventListener('focus',load);};
 },[id,token]);
 async function copy(){try{await navigator.clipboard.writeText(`${window.location.origin}${trackingPath(Number(id),token)}`);setCopied(true);}catch{setCopied(false);}}
 return <Layout variant="ordering"><main className="guest-tracking">
  <p className="eyebrow">TU PEDIDO EN RADIOCAFÉ</p><h1>{id?`Seguimiento del pedido #${id}`:'Mis pedidos'}</h1>
  {!id?<><p>Consulta aquí el estado de tus pedidos recientes.</p><div className="stack">{guestOrders().map(x=><Link className="card guest-order-link" key={x.id} to={trackingPath(x.id,x.token)}><span><ShoppingBag size={20} aria-hidden="true" /> Pedido #{x.id}</span><span>Ver seguimiento <ArrowRight size={18} aria-hidden="true" /></span></Link>)}{guestOrders().length>0?<p className="guest-order-note">Tus pedidos se guardan en esta ventana. Para consultarlos en otro dispositivo, guarda el enlace privado de seguimiento.</p>:<div className="guest-orders-empty"><span className="guest-orders-icon"><ShoppingBag size={32} strokeWidth={1.5} aria-hidden="true" /></span><h2>Tu próxima pausa empieza aquí</h2><p>Todavía no tienes pedidos en esta ventana. Descubre nuestros cafés y antojos favoritos.</p><Link className="button" to="/catalogo">Explorar la carta <ArrowRight size={18} aria-hidden="true" /></Link></div>}</div></>:<>
   {error&&<p className="notice error" role="alert">{error} Comprueba que tienes el enlace privado completo.</p>}
   {!order&&!error&&<p role="status">Consultando tu pedido…</p>}
   {order&&<><div className="tracking-grid"><div className="tracking-main">
    <section className="tracking-status" role="status" aria-live="polite"><span className="tracking-status-icon"><Coffee size={28}/></span><div><span className="eyebrow">ESTADO DE TU PEDIDO</span><h2>{labels[order.estadoPedido]||order.estadoPedido}</h2><p>{order.estadoPedido==='CANCELLED'?'Este pedido ha sido cancelado.': 'Te acompañamos en cada paso de tu pedido.'}</p></div><span className="tracking-live">● Actualización automática</span></section>
    {order.estadoPedido!=='CANCELLED'&&<section className="tracking-progress card"><h2>Así va tu pedido</h2><div className="tracking-steps">{(order.tipoEntrega==='DELIVERY'?['PENDIENTE','EN_PREPARACION','LISTO','EN_CAMINO','ENTREGADO']:['PENDIENTE','EN_PREPARACION','LISTO','ENTREGADO']).map((step,index,steps)=>{const current=steps.indexOf(order.estadoPedido);return <div className={`tracking-step ${index<=current?'active':''} ${index===current?'current':''}`} key={step}><span>{index<current?<Check size={16}/>:index+1}</span><strong>{({PENDIENTE:'Recibido',EN_PREPARACION:'En preparación',LISTO:'Listo',EN_CAMINO:'En camino',ENTREGADO:'Entregado'})[step]}</strong></div>;})}</div>{order.preparacionEstimada?<p className="tracking-estimate"><Clock3 size={18}/> {order.nombreBarista} estima que estará listo a las <strong>{new Date(order.preparacionEstimada).toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit',timeZone:'America/Lima'})}</strong> · {order.minutosPreparacion} min</p>:<p className="tracking-estimate"><Clock3 size={18}/> El barista aún no ha indicado el tiempo estimado.</p>}</section>}
    <section className="card tracking-summary"><h2>Tu pedido</h2><p className="tracking-delivery"><Truck size={18}/>{order.tipoEntrega==='DELIVERY'?'Delivery':'Recojo en local'}</p><ul className="order-lines">{order.detalles.map((d,i)=><li key={i}><span>{d.cantidad} × {d.nombreProducto}{d.personalizacionDescripcion&&<small className="order-line-configuration">{d.personalizacionDescripcion}</small>}</span><strong>{money(d.subtotalLinea)}</strong></li>)}</ul><div className="tracking-total"><span>Total</span><strong>{money(order.total)}</strong></div><small>Subtotal {money(order.subtotal)} · Envío {money(order.costoEnvio)}{order.descuento>0&&` · Descuento −${money(order.descuento)}`}</small></section>
    </div><aside className="tracking-side"><section className="card tracking-payment"><span className="tracking-side-icon"><CreditCard size={22}/></span><h2>Demostración de pago</h2><p>Prueba el flujo de pago para tu presentación. No se realiza ningún cobro.</p><div className="tracking-payment-amount"><span>Importe de prueba</span><strong>{money(order.total)}</strong></div>{simulated?<p className="tracking-demo-success"><Check size={18}/> Pago simulado completado</p>:<button type="button" className="button" disabled={order.estadoPedido==='CANCELLED'} onClick={()=>{sessionStorage.setItem(`radiocafe.payment.demo.${id}`,'done');setSimulated(true);}}>Simular pago</button>}<small>Solo se guarda en esta ventana y no cambia el pago del pedido. El pago real sigue siendo al {order.tipoEntrega==='DELIVERY'?'recibir':'recoger'}.</small></section>
    <section className="card tracking-link"><h2>Tu enlace de seguimiento</h2><p>Guárdalo para consultar este pedido desde otro dispositivo. Es privado: quien lo tenga podrá ver el estado.</p><label>Enlace privado<input readOnly value={`${window.location.origin}${trackingPath(Number(id),token)}`} onFocus={e=>e.target.select()}/></label><button className="button secondary" onClick={copy}><Copy size={16}/> {copied?'Enlace copiado':'Copiar enlace'}</button></section></aside></div></>}
  </>}
  {id&&<Link className="button" to="/catalogo">Seguir explorando la carta</Link>}
 </main></Layout>;
}
