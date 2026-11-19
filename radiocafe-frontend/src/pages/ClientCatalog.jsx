import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ChevronRight, MapPin, Minus, Plus, ShoppingBag, Trash2, X, ArrowLeft, Check } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { api, money } from '../lib/api';
import useAction from '../lib/useAction';
import Layout from '../components/Layout';
import Orders from '../components/Orders';
import Contest from '../components/Contest';
import ProductVisual from '../components/ProductVisual';
import ProductCustomizer from '../components/ProductCustomizer';
import { configurationPrice, configurationDescription } from '../lib/productConfiguration';
import './Ordering.css';

const freshChoice = { tamano: 'ALTO', leche: '', crema: '', extras: [] };
const categories = { CAFE: 'Cafés y bebidas', PASTELERIA: 'Pastelería', COMBO: 'Combos' };
export default function ClientCatalog() {
  const { user } = useAuth();
  const { busy, message, error, action } = useAction();
  const [params] = useSearchParams();
  const [tab, setTab] = useState(() => params.get('seccion') === 'kappu' ? 'kappu' : 'menu');
  const [products, setProducts] = useState([]);
  const [options, setOptions] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [choice, setChoice] = useState(freshChoice);
  const [quantity, setQuantity] = useState(1);
  const [cart, setCart] = useState([]);
  const [cartOpen, setCartOpen] = useState(false);
  const [delivery, setDelivery] = useState('PICKUP');
  const [address, setAddress] = useState('');
  const [loadError, setLoadError] = useState('');
  const [added, setAdded] = useState(false);
  const cartDialog = useRef(null);
  const addedTimer = useRef(null);
  const selected = products.find(p => p.idProducto === selectedId);
  const count = cart.reduce((sum, line) => sum + line.cantidad, 0);
  const subtotal = cart.reduce((sum, line) => sum + Math.round(line.precio * 100) * line.cantidad, 0) / 100;
  const shipping = cart.length && delivery === 'DELIVERY' && subtotal < 50 ? 5 : 0;
  const unitPrice = configurationPrice(selected, options, choice);
  const remainingStock = Math.max(0, (selected?.stock || 0) - cart.filter(line => line.idProducto === selected?.idProducto).reduce((sum,line) => sum + line.cantidad,0));
  const ready = selected && remainingStock >= quantity && (!selected.personalizable || (choice.tamano && choice.leche && choice.crema));

  async function refreshOrders() { setOrders(await api('/orders')); }
  async function refreshCatalog() { const list = await api('/products'); setProducts(list); setLoadError(''); if (!list.some(p => p.idProducto === selectedId)) setSelectedId(list[0]?.idProducto ?? null); }
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([api('/products', { signal: controller.signal }), api('/orders', { signal: controller.signal }), api('/products/options', { signal: controller.signal })]).then(([p,o,config]) => {
      if (!controller.signal.aborted) { setProducts(p); setOrders(o); setOptions(config); setSelectedId(p[0]?.idProducto ?? null); }
    }).catch(e => { if (!controller.signal.aborted) setLoadError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => { controller.abort(); clearTimeout(addedTimer.current); };
  }, []);
  useEffect(() => {
    if (cartOpen && !cartDialog.current.open) cartDialog.current.showModal();
    if (!cartOpen && cartDialog.current.open) cartDialog.current.close();
  }, [cartOpen]);
  function selectProduct(product) {
    setSelectedId(product.idProducto); setChoice(freshChoice); setQuantity(1); setAdded(false); setTab('menu');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  function add() {
    if (!ready || busy) return;
    const personalizacion = selected.personalizable ? { ...choice, extras: [...choice.extras].sort() } : null;
    const key = `${selected.idProducto}:${JSON.stringify(personalizacion)}`;
    setCart(prev => {
      const existing = prev.find(line => line.key === key);
      return existing ? prev.map(line => line.key === key ? { ...line, cantidad: Math.min(99, line.cantidad + quantity) } : line)
        : [...prev, { ...selected, key, cantidad: quantity, precio: unitPrice, personalizacion, personalizacionDescripcion: configurationDescription(selected,options,choice) }];
    });
    setAdded(true); clearTimeout(addedTimer.current); addedTimer.current = setTimeout(() => setAdded(false), 2500);
  }
  function changeQuantity(key, delta) { setCart(prev => prev.map(line => line.key === key ? { ...line, cantidad: Math.min(99, Math.max(1, line.cantidad+delta)) } : line)); }
  async function checkout(e) {
    e.preventDefault();
    const ok = await action(async () => {
      await api('/orders', { method: 'POST', body: { tipoEntrega: delivery, direccionEntrega: delivery === 'DELIVERY' ? address : null, detalles: cart.map(line => ({ idProducto: line.idProducto, cantidad: line.cantidad, personalizacion: line.personalizacion })) } });
      setCart([]); setCartOpen(false); setTab('orders'); await refreshOrders();
    }, 'Pedido registrado. Paga al recoger o recibir tu pedido.');
    if (ok) window.scrollTo({ top: 0 });
  }
  return <Layout variant="ordering" navigation={<><button className={tab === 'menu' ? 'nav-active' : ''} onClick={() => { setTab('menu'); setSelectedId(null); }}>Menú</button><button className={tab === 'orders' ? 'nav-active' : ''} onClick={() => { setTab('orders'); void action(refreshOrders); }}>Mis pedidos</button><button className={tab === 'kappu' ? 'nav-active' : ''} onClick={() => setTab('kappu')}>Kappu</button></>}
    tools={<><button className="delivery-header" onClick={() => setCartOpen(true)}><MapPin size={17}/><span>{delivery === 'PICKUP' ? 'Recojo en local' : address || 'Selecciona tu dirección'}</span></button><button className="cart-header" aria-label={`Ver mi pedido, ${count} productos`} onClick={() => setCartOpen(true)}><ShoppingBag size={22}/><span>{count}</span></button></>}>
    <main className={`ordering-page ${tab === 'menu' && selected ? 'has-order-bar' : ''}`}>
      <div className="order-breadcrumb"><div><button onClick={() => { setTab('menu'); setSelectedId(null); }}>Menú</button><ChevronRight size={16}/>{tab === 'menu' && selected ? <><button onClick={() => setSelectedId(null)}>{categories[selected.categoria]}</button><ChevronRight size={16}/><span>{selected.nombre}</span></> : <span>{tab === 'orders' ? 'Mis pedidos' : tab === 'kappu' ? 'Concurso Kappu' : 'Todos los productos'}</span>}</div></div>
      <div className="ordering-content">
        {(error || loadError) && <p className="notice error" role="alert">{error || loadError}</p>}{message && <p className="notice success" role="status">{message}</p>}
        {loading ? <p role="status" className="loading">Preparando tu menú…</p> : tab === 'menu' ? selected && options ? <ProductCustomizer product={selected} options={options} choice={choice} onChange={value => { setChoice(value); setAdded(false); }}/>
          : <section className="ordering-menu"><div className="menu-intro"><p className="eyebrow">RADIOCAFÉ A TU MANERA</p><h1>Un momento para ti.</h1><p>Elige tu favorito. Nosotros lo preparamos a tu gusto.</p><button className="text-button" disabled={busy} onClick={() => action(refreshCatalog)}>Actualizar menú</button></div>
            {Object.entries(categories).map(([id,label]) => { const group = products.filter(p => p.categoria === id); return group.length > 0 && <section className="menu-category" key={id}><h2>{label}</h2><div className="menu-product-grid">{group.map(p => <button key={p.idProducto} className="menu-product" onClick={() => selectProduct(p)}><span className="menu-product-image"><ProductVisual product={p}/></span><strong>{p.nombre}</strong><span>Desde {money(p.precio)}</span>{!p.stock && <span className="stock-label">Agotado</span>}</button>)}</div></section>; })}
            {!products.length && <p className="empty">No hay productos disponibles.</p>}
          </section> : tab === 'orders' ? <Orders orders={orders} role="CLIENTE" refresh={refreshOrders} action={action} busy={busy}/> : <Contest user={user} action={action} busy={busy}/>}
      </div>
    </main>
    {tab === 'menu' && selected && <div className="product-order-bar"><div><button className="back-to-menu" onClick={() => setSelectedId(null)}><ArrowLeft size={18}/><span>Seguir explorando</span></button><span className="selection-hint" aria-live="polite">{added ? 'Tu producto se agregó al pedido' : !ready ? (remainingStock < quantity ? 'No hay stock para esta cantidad' : 'Selecciona las opciones obligatorias') : 'Hecho a tu manera'}</span><div className="order-bar-actions"><div className="order-stepper"><button aria-label="Reducir cantidad" disabled={quantity <= 1} onClick={() => setQuantity(q => q-1)}><Minus size={17}/></button><output aria-label="Cantidad">{quantity}</output><button aria-label="Aumentar cantidad" disabled={quantity >= Math.min(99, remainingStock)} onClick={() => setQuantity(q => q+1)}><Plus size={17}/></button></div><button className={`add-custom-product ${added ? 'added' : ''}`} disabled={!ready || busy} onClick={add}>{added ? <><Check size={18}/> Agregado a tu pedido</> : <>Agregar por {money(unitPrice * quantity)}</>}</button></div></div></div>}
    <dialog ref={cartDialog} className="order-cart-dialog" aria-labelledby="order-cart-title" onCancel={() => setCartOpen(false)} onClick={e => { if (e.target === e.currentTarget) setCartOpen(false); }} onClose={() => setCartOpen(false)}>
      <div className="order-cart-panel"><header><div><p>RADIOCAFÉ</p><h2 id="order-cart-title">Tu pedido <span>({count})</span></h2></div><button autoFocus aria-label="Cerrar pedido" onClick={() => setCartOpen(false)}><X size={23}/></button></header>
        <form onSubmit={checkout}><div className="order-cart-body"><div className="delivery-toggle"><button type="button" className={delivery === 'PICKUP' ? 'selected' : ''} onClick={() => setDelivery('PICKUP')}>Recojo en local</button><button type="button" className={delivery === 'DELIVERY' ? 'selected' : ''} onClick={() => setDelivery('DELIVERY')}>Delivery</button></div>
          {delivery === 'DELIVERY' && <label className="order-address">Dirección de entrega<input required maxLength={500} placeholder="Calle, número y referencia" value={address} onChange={e => setAddress(e.target.value)}/></label>}
          {cart.length === 0 ? <div className="order-cart-empty"><ShoppingBag size={42} strokeWidth={1}/><h3>Tu próxima pausa te espera</h3><p>Agrega tus favoritos para armar tu pedido.</p><button type="button" className="button" onClick={() => { setCartOpen(false); setTab('menu'); }}>Explorar el menú</button></div> : cart.map(line => <article className="configured-cart-line" key={line.key}><div className="cart-mini-visual"><ProductVisual product={line}/></div><div><h3>{line.nombre}</h3>{line.personalizacionDescripcion && <p>{line.personalizacionDescripcion}</p>}<strong>{money(line.precio*line.cantidad)}</strong><div className="cart-line-controls"><div className="order-stepper"><button type="button" aria-label={`Reducir ${line.nombre}`} disabled={busy || line.cantidad <= 1} onClick={() => changeQuantity(line.key,-1)}><Minus size={14}/></button><span>{line.cantidad}</span><button type="button" aria-label={`Aumentar ${line.nombre}`} disabled={busy || line.cantidad >= 99} onClick={() => changeQuantity(line.key,1)}><Plus size={14}/></button></div><button type="button" className="remove-cart-line" disabled={busy} aria-label={`Quitar ${line.nombre}`} onClick={() => setCart(prev => prev.filter(p => p.key !== line.key))}><Trash2 size={16}/></button></div></div></article>)}
        </div><div className="order-cart-checkout">{error && <p role="alert" className="notice error">{error}</p>}<div><span>Subtotal</span><strong>{money(subtotal)}</strong></div><div><span>Envío</span><strong>{shipping ? money(shipping) : 'Gratis'}</strong></div><div className="cart-grand-total"><span>Total estimado</span><strong>{money(subtotal+shipping)}</strong></div><p>Pago al recoger o recibir. El precio final se confirma al registrar.</p><button className="button checkout-button" disabled={busy || !cart.length}>{busy ? 'Registrando…' : 'Confirmar pedido'}</button></div></form>
      </div>
    </dialog>
  </Layout>;
}
