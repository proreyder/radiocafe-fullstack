import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import SiteHeader from './SiteHeader';
export default function Layout({ children, variant = 'default', navigation, tools, hideHeader = false }) {
  const { user } = useAuth();
  return <div className={`app-layout artisan-layout ${variant === 'ordering' ? 'ordering-layout' : variant === 'home' ? 'home-layout' : ''}`}>
    {!hideHeader&&<SiteHeader navigation={navigation} tools={tools}/>}
    {children}
    <footer className="artisan-footer"><div><Link className="footer-wordmark" to="/">RadioCafé</Link><p>El sabor de una buena pausa.</p><span>Café de especialidad, sabores artesanales y una comunidad que crea.</span></div>
      <div><h3>Una taza, muchas historias</h3><a href="/#nuestra-esencia">Nuestra esencia</a><Link to="/kappu">Galería y concurso Kappu</Link><Link to="/catalogo">{user ? 'La carta y tus pedidos' : 'Catálogo de productos'}</Link><span>Delivery o recojo en local</span></div>
      <div><h3>Hecho para compartir</h3><p>Disfruta. Crea. Vuelve.</p><span>RadioCafé · Café, arte y comunidad.</span></div><small>RadioCafé · Todos los buenos momentos empiezan con una taza.</small>
    </footer>
  </div>;
}
