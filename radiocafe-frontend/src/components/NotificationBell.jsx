import { useEffect, useRef, useState } from 'react';
import { Bell, CheckCheck, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import '../pages/Experience.css';
export default function NotificationBell({ user }) {
 const [open, setOpen] = useState(false), [box, setBox] = useState({ elementos: [], noLeidas: 0 }), [error, setError] = useState('');
 const last = useRef(null), container = useRef(null);const navigate = useNavigate();
 async function refresh() {const data=await api('/notifications'); if(last.current!==null && data.elementos[0]?.id>last.current) window.dispatchEvent(new Event('orders-updated')); last.current=data.elementos[0]?.id||0;setBox(data);setError('');}
 useEffect(() => {
  let active=true;
  const load=async()=>{if(document.visibilityState==='hidden')return;try {const data=await api('/notifications');if(active){if(last.current!==null && data.elementos[0]?.id>last.current)window.dispatchEvent(new Event('orders-updated'));last.current=data.elementos[0]?.id||0;setBox(data);setError('');}}catch(e){if(active)setError(e.message);}};
  void load();const timer=setInterval(load,8000);window.addEventListener('focus',load);document.addEventListener('visibilitychange',load);
  return()=>{active=false;clearInterval(timer);window.removeEventListener('focus',load);document.removeEventListener('visibilitychange',load);};
 },[user.idUsuario]);
 useEffect(()=>{if(!open)return;const close=e=>{if(e.type==='keydown'?e.key==='Escape':!container.current?.contains(e.target))setOpen(false);};document.addEventListener('pointerdown',close);document.addEventListener('keydown',close);return()=>{document.removeEventListener('pointerdown',close);document.removeEventListener('keydown',close);};},[open]);
 async function read(n) {try {if(!n.leida)await api(`/notifications/${n.id}/read`,{method:'PATCH'});await refresh();setOpen(false);if(/^\/(admin|catalogo)\?seccion=/.test(n.enlace))navigate(n.enlace);else navigate('/kappu');}catch(e){setError(e.message);}}
 return <div className="notification-bell" ref={container}><button className="notification-trigger" aria-label={`Notificaciones, ${box.noLeidas} sin leer`} aria-expanded={open} onClick={()=>{setOpen(!open);if(!open)void refresh().catch(e=>setError(e.message));}}><Bell size={21}/>{box.noLeidas>0&&<span>{box.noLeidas>99?'99+':box.noLeidas}</span>}</button>{open&&<section className="notification-popover" aria-label="Centro de notificaciones"><header><div><h2>Notificaciones</h2><p>{box.noLeidas} sin leer</p></div><button aria-label="Cerrar notificaciones" onClick={()=>setOpen(false)}><X size={20}/></button></header>{error&&<p className="notice error" role="alert">{error}</p>}<button className="text-button notification-read-all" onClick={async()=>{try {await api('/notifications/read-all',{method:'PATCH'});await refresh();}catch(e){setError(e.message);}}}><CheckCheck size={17}/>Marcar todas como leídas</button>{box.elementos.length?box.elementos.map(n=><button className={`notification-item ${n.leida?'':'unread'}`} key={n.id} onClick={()=>void read(n)}><span className="notification-type">{n.tipo} · {n.autor}</span><strong>{n.titulo}</strong><span>{n.mensaje}</span><small>{new Date(n.fecha).toLocaleString('es-PE')}</small></button>):<p className="notification-empty">Aquí recibirás las novedades de tus pedidos y de RadioCafé.</p>}</section>}</div>;
}
