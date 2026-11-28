import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { staffRoles } from '../lib/api';
import NotificationBell from './NotificationBell';
import ProfileMenu from './ProfileMenu';
import '../pages/SiteHeader.css';
export default function SiteHeader({ navigation, tools, onNavigate, disabled = false }) {
 const {user}=useAuth();
 const wrapper=useRef(null),header=useRef(null);
 const [scrolled,setScrolled]=useState(false),[expandedHeight,setExpandedHeight]=useState(116);
 useEffect(()=>{
  const target=wrapper.current?.closest('dialog')||window;
  const update=()=>setScrolled((target===window?window.scrollY:target.scrollTop)>72);
  update();target.addEventListener('scroll',update,{passive:true});
  return()=>target.removeEventListener('scroll',update);
 },[]);
 useLayoutEffect(()=>{
  if(!header.current||scrolled)return;
  const measure=()=>{const height=Math.ceil(header.current.getBoundingClientRect().height);setExpandedHeight(height);wrapper.current?.closest('.artisan-layout')?.style.setProperty('--site-header-expanded-height',`${height}px`);};
  measure();const observer=new ResizeObserver(measure);observer.observe(header.current);
  return()=>observer.disconnect();
 },[scrolled]);
 return <div ref={wrapper} className={`site-chrome floating-site-header ${scrolled?'is-scrolled':''}`} style={{height:expandedHeight}} inert={disabled||undefined}><header ref={header} className="site-header"><Link className="brand official-brand" to="/" aria-label="RadioCafé, inicio" onClick={onNavigate}><img className="official-brand-image" src="/assets/radiocafe-oficial.jpg" alt="Radio Café · Desde 2012" width="92" height="92"/></Link><nav aria-label="Principal">{navigation}{!user&&!navigation&&<><Link to="/" onClick={onNavigate}>Inicio</Link><Link to="/catalogo" onClick={onNavigate}>Menú</Link><Link to="/?acceso=login" onClick={onNavigate}>Iniciar sesión</Link></>}{!user&&tools}{user&&<>{!navigation&&['CLIENTE','ADMINISTRADOR'].includes(user.role)&&<><Link to="/catalogo?seccion=menu" onClick={onNavigate}>Menú</Link><Link to="/catalogo?seccion=orders" onClick={onNavigate}>Mis pedidos</Link><Link to="/catalogo?seccion=kappu" onClick={onNavigate}>Kappu</Link><Link to="/catalogo?seccion=support" onClick={onNavigate}>Atención</Link></>}{staffRoles.includes(user.role)&&<Link to="/admin" onClick={onNavigate}>Panel de trabajo</Link>}{tools}<NotificationBell key={user.idUsuario} user={user}/><ProfileMenu user={user} onNavigate={onNavigate}/></>}</nav></header></div>;
}
