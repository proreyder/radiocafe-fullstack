import { Link } from 'react-router-dom';
import { Wheat } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { staffRoles } from '../lib/api';
import NotificationBell from './NotificationBell';
import ProfileMenu from './ProfileMenu';
import '../pages/SiteHeader.css';
export default function SiteHeader({ navigation, tools, onNavigate, disabled = false }) {
 const {user}=useAuth();
 return <div className="site-chrome" inert={disabled||undefined}><div className="artisan-topbar"><span><Wheat size={12}/>Café, arte y buenos momentos.</span><span>Origen peruano · Hecho para compartir</span></div><header className="site-header"><Link className="brand official-brand" to="/" aria-label="RadioCafé, inicio" onClick={onNavigate}><img className="official-brand-image" src="/assets/radiocafe-oficial.jpg" alt="Radio Café · Desde 2012" width="92" height="92"/></Link><nav aria-label="Principal">{navigation}{!user&&!navigation&&<><Link to="/" onClick={onNavigate}>Inicio</Link><Link to="/catalogo" onClick={onNavigate}>Catálogo</Link><Link to="/?acceso=login" onClick={onNavigate}>Iniciar sesión</Link></>}{user&&<>{!navigation&&['CLIENTE','ADMINISTRADOR'].includes(user.role)&&<><Link to="/catalogo?seccion=menu" onClick={onNavigate}>Menú</Link><Link to="/catalogo?seccion=catalog" onClick={onNavigate}>Catálogo</Link><Link to="/catalogo?seccion=orders" onClick={onNavigate}>Mis pedidos</Link><Link to="/catalogo?seccion=kappu" onClick={onNavigate}>Kappu</Link><Link to="/catalogo?seccion=support" onClick={onNavigate}>Atención</Link></>}{staffRoles.includes(user.role)&&<Link to="/admin" onClick={onNavigate}>Panel de trabajo</Link>}{tools}<NotificationBell key={user.idUsuario} user={user}/><ProfileMenu user={user} onNavigate={onNavigate}/></>}</nav></header></div>;
}
