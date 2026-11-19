import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Coffee, Palette, Truck, X, Wheat, Heart } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { staffRoles } from '../lib/api';
import Layout from '../components/Layout';
export default function Home() {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [nextSection, setNextSection] = useState('menu');
  function destination(account, section) {
    const page = staffRoles.includes(account.role) ? '/admin' : '/catalogo';
    return section === 'kappu' && account.role !== 'REPARTIDOR' ? `${page}?seccion=kappu` : page;
  }
  async function submit(e) {
    e.preventDefault(); setError('');
    if (registering && password !== confirm) { setError('Las contraseñas no coinciden.'); return; }
    setBusy(true);
    try {
      const account = registering ? await register(nombre, correo, password) : await login(correo, password);
      navigate(destination(account, nextSection));
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  function enter(section = 'menu') {
    const requested = section === 'kappu' ? 'kappu' : 'menu';
    setNextSection(requested);
    if (user) navigate(destination(user, requested));
    else setOpen(true);
  }
  return <Layout navigation={<><a className="artisan-nav-tab current" href="#inicio">Inicio</a><a className="artisan-nav-tab" href="#nuestra-esencia">Nuestra esencia</a><a className="artisan-nav-tab" href="#la-carta">La carta</a><a className="artisan-nav-tab" href="#comunidad">Comunidad</a>{!user && <button className="artisan-nav-login" onClick={enter}>Iniciar sesión</button>}</>}><main className="artisan-home" id="inicio">
    <section className="bakery-hero" aria-labelledby="hero-heading"><img className="bakery-hero-photo" src="/images/radiocafe-artesanal-hero.png" alt="Cappuccino, croissants y pan artesanal sobre una mesa de madera" fetchPriority="high"/><div className="bakery-hero-shade"/>
      <div className="bakery-hero-content"><span className="hero-ribbon">CAFÉ DE ESPECIALIDAD & BUENOS MOMENTOS</span><h1 id="hero-heading">Lo bueno se hace<br/><em>con calma.</em></h1><p>Una taza de café, algo recién hecho<br/>y ese momento que es solo tuyo.</p><button className="artisan-cta" onClick={enter}>Descubre nuestra carta </button><span className="hero-footnote">Sabor que acompaña. Historias que se quedan.</span></div>
      <div className="hero-stamp" aria-hidden="true"><Coffee size={27}/><span>Con cariño,</span><strong>RadioCafé</strong><small>UNA TAZA A LA VEZ</small></div>
    </section>
    <section className="artisan-story paper-section" id="nuestra-esencia"><div className="story-polaroid polaroid-left"><img src="/images/radiocafe-artesanal-hero.png" alt="Una pausa con café y sabores artesanales" loading="lazy"/><span>Los pequeños placeres.</span></div>
      <div className="story-copy"><span className="section-flourish"><Wheat size={24}/> NUESTRA ESENCIA <Wheat size={24}/></span><h2>Un café con alma.</h2><p>Nos gustan las cosas sencillas y bien hechas: un buen café, el aroma de la pastelería y las conversaciones que no miran el reloj.</p><p className="story-signature">Así se vive una pausa en RadioCafé.</p><a className="ribbon-link" href="#la-carta">Encuentra tu favorito </a></div>
      <div className="story-polaroid polaroid-right"><div><Coffee size={65} strokeWidth={1}/><span>Tu momento<br/>favorito.</span></div><span>Siempre hay tiempo para un café.</span></div>
    </section>
    <section className="artisan-offerings paper-section" id="la-carta"><span className="section-flourish">HECHO PARA DISFRUTAR</span><h2>Con sabor a buenos momentos</h2><p className="section-intro">Elige tu antojo. Nosotros ponemos el cariño.</p><div className="offering-grid">
      <article><div className="offering-illustration"><Coffee size={64} strokeWidth={1}/><span className="mini-bean"/></div><h3>Nuestro café</h3><p>Tu bebida favorita, a tu manera. Elige tamaño, leche y ese toque que la hace tuya.</p><button className="ribbon-link" onClick={enter}>Ver la carta </button></article>
      <article><div className="offering-illustration"><Wheat size={65} strokeWidth={1}/></div><h3>Algo para acompañar</h3><p>Croissants, pasteles y pequeños antojos para convertir una pausa en un buen plan.</p><button className="ribbon-link" onClick={enter}>Descubrir sabores </button></article>
      <article><div className="offering-illustration"><Truck size={64} strokeWidth={1}/></div><h3>Donde tú estés</h3><p>Recoge en el local o pide delivery. Envío gratis en pedidos desde $50.</p><button className="ribbon-link" onClick={enter}>Hacer un pedido </button></article>
    </div></section>
    <section className="artisan-community" id="comunidad"><div className="community-art" aria-hidden="true"><div className="community-paper"><Palette size={45} strokeWidth={1}/><span>Un poquito de café.<br/><em>Mucho de ti.</em></span><Heart size={23}/></div><span className="community-doodle">Kappu</span></div><div><span className="section-flourish">MÁS QUE UNA CAFETERÍA</span><h2>Aquí también<br/>se sirve creatividad.</h2><p>Comparte tu arte en el concurso Kappu, vota por tu ilustración favorita y sé parte de nuestra comunidad.</p><button className="artisan-cta" onClick={() => enter('kappu')}>Conoce Kappu </button></div></section>
  </main>
  {open && <div className="modal-backdrop"><section className="modal" role="dialog" aria-modal="true" aria-labelledby="auth-title"><button aria-label="Cerrar" className="icon-close" onClick={() => setOpen(false)}><X /></button>
    <p className="eyebrow">BIENVENIDO A LA COMUNIDAD</p><h2 id="auth-title">{registering ? 'Crea tu cuenta' : 'Qué bueno verte'}</h2><p>Ingresa para pedir y participar en Kappu.</p>
    {error && <p className="notice error" role="alert">{error}</p>}
    <form onSubmit={submit} className="stack">
      {registering && <label>Nombre<input autoFocus required maxLength={100} value={nombre} onChange={e => setNombre(e.target.value)} autoComplete="name" /></label>}
      <label>Correo<input required type="email" maxLength={150} value={correo} onChange={e => setCorreo(e.target.value)} autoComplete="email" /></label>
      <label>Contraseña<input required type="password" minLength={registering ? 8 : undefined} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} autoComplete={registering ? 'new-password' : 'current-password'} /></label>
      {registering && <label>Repite la contraseña<input required type="password" maxLength={72} value={confirm} onChange={e => setConfirm(e.target.value)} autoComplete="new-password" /></label>}
      <button className="button" disabled={busy}>{busy ? 'Un momento…' : registering ? 'Crear cuenta' : 'Iniciar sesión'}</button>
    </form><button className="text-button" disabled={busy} onClick={() => { setRegistering(!registering); setError(''); }}>{registering ? 'Ya tengo una cuenta' : '¿Primera vez? Regístrate'}</button>
  </section></div>}</Layout>;
}
