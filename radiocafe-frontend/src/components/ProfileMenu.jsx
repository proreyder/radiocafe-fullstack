import { useEffect, useId, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserRound, ShieldCheck, ReceiptText, LogOut, ChevronDown } from 'lucide-react';
import { api } from '../lib/api';
import { useAuth } from '../context/useAuth';
import '../pages/Account.css';
export function ProfileAvatar({ user, large = false }) {
 const [src,setSrc]=useState('');
 useEffect(()=>{const controller=new AbortController();let url;setSrc('');if(user.fotoUrl)api(user.fotoUrl.replace('/api/v1',''),{responseType:'blob',signal:controller.signal}).then(blob=>{if(!controller.signal.aborted){url=URL.createObjectURL(blob);setSrc(url);}}).catch(()=>{});return()=>{controller.abort();if(url)URL.revokeObjectURL(url);};},[user.idUsuario,user.fotoUrl]);
 return <span className={`profile-avatar ${large?'large':''}`}>{src?<img src={src} alt={`Foto de ${user.nombre}`}/>:<span aria-hidden="true">{user.nombre?.trim().slice(0,1).toUpperCase()||<UserRound/>}</span>}</span>;
}
export default function ProfileMenu({ user, onNavigate }) {
 const [open,setOpen]=useState(false),[error,setError]=useState('');const ref=useRef(null),id=useId(),navigate=useNavigate();const {logout}=useAuth();
 useEffect(()=>{if(!open)return;const close=e=>{if(e.type==='keydown'?e.key==='Escape':!ref.current?.contains(e.target))setOpen(false);};document.addEventListener('pointerdown',close);document.addEventListener('keydown',close);return()=>{document.removeEventListener('pointerdown',close);document.removeEventListener('keydown',close);};},[open]);
 function move(){setOpen(false);onNavigate?.();}
 return <div className="profile-menu" ref={ref}><button className="profile-trigger" aria-label={`Mi perfil, ${user.nombre}`} aria-expanded={open} aria-controls={id} onClick={()=>setOpen(!open)}><ProfileAvatar user={user}/><span>Mi perfil</span><ChevronDown size={15}/></button>{open&&<section id={id} className="profile-dropdown" aria-label="Opciones de mi cuenta"><div className="profile-dropdown-identity"><ProfileAvatar user={user}/><div><strong>{user.nombre}</strong><span>{user.correo}</span></div></div><Link to="/perfil" onClick={move}><UserRound size={18}/>Mi perfil e información</Link><Link to="/perfil?seccion=security" onClick={move}><ShieldCheck size={18}/>Seguridad y contraseña</Link><Link to="/perfil?seccion=activity" onClick={move}><ReceiptText size={18}/>Pedidos y movimientos</Link><button className="profile-logout" onClick={async()=>{try {await logout();move();navigate('/');}catch(e){setError(e.message);}}}><LogOut size={18}/>Cerrar sesión</button>{error&&<p className="notice error" role="alert">{error}</p>}</section>}</div>;
}
