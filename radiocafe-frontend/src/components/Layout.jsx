import { Link, useNavigate } from 'react-router-dom';
import { Coffee, LogOut, Wheat } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../context/useAuth';
import { staffRoles } from '../lib/api';
export default function Layout({ children, variant = 'default', navigation, tools }) {
  const { user, logout } = useAuth();
  const [error, setError] = useState('');
  const navigate = useNavigate();
  async function leave() { try { await logout(); navigate('/'); } catch (e) { setError(e.message); } }
  return <div className={`app-layout artisan-layout ${variant === 'ordering' ? 'ordering-layout' : ''}`}>
    <div className="artisan-topbar"><span><Wheat size={12}/> Café, arte y buenos momentos.</span><span>Origen peruano · Hecho para compartir</span></div>
    <header className="site-header"><Link className="brand" to="/" aria-label="RadioCafé, inicio"><Coffee size={24}/><span className="brand-wordmark">RadioCafé</span><span className="brand-caption">CAFÉ · ARTE · COMUNIDAD</span></Link>
      <nav aria-label="Principal">{navigation}<span className="user-name">{user?.nombre}</span>{user && <>
        {!navigation && ['CLIENTE', 'ADMINISTRADOR'].includes(user.role) && <Link to="/catalogo">La carta</Link>}
        {staffRoles.includes(user.role) && <Link to="/admin">Panel de trabajo</Link>}
        {tools}<button className="button ghost" onClick={leave}><LogOut size={16}/> Salir</button></>}</nav>
    </header>
    {error && <div className="notice error" role="alert">{error}</div>}{children}
    <footer className="artisan-footer"><div><Link className="footer-wordmark" to="/">RadioCafé</Link><p>El sabor de una buena pausa.</p><span>Café de especialidad, sabores artesanales y una comunidad que crea.</span></div>
      <div><h3>Una taza, muchas historias</h3><a href="/#nuestra-esencia">Nuestra esencia</a>{user && <Link to="/catalogo">La carta y tus pedidos</Link>}<span>Delivery o recojo en local</span></div>
      <div><h3>Hecho para compartir</h3><p>Disfruta. Crea. Vuelve.</p><span>RadioCafé · Café, arte y comunidad.</span></div><small>RadioCafé · Todos los buenos momentos empiezan con una taza.</small>
    </footer>
  </div>;
}
