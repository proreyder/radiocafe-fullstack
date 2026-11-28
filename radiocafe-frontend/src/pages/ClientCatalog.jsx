import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronRight, MapPin, Minus, Plus, ShoppingBag, ArrowLeft, Check } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { api, money } from '../lib/api';
import useAction from '../lib/useAction';
import Layout from '../components/Layout';
import Orders from '../components/Orders';
import Contest from '../components/Contest';
import CatalogBrowser from '../components/CatalogBrowser';
import Checkout from '../components/Checkout';
import FullCatalog from '../components/FullCatalog';
import useSessionCart from '../lib/useSessionCart';
import { BaristaAvailability } from '../components/BaristaPanel';
import ProductCustomizer from '../components/ProductCustomizer';
import { configurationPrice, configurationDescription } from '../lib/productConfiguration';
import { CommerceShowcase, ProductReviews, SupportPanel } from '../components/CommercePanels';
import './Ordering.css';
import { rememberGuestOrder, trackingPath } from '../lib/guestOrders';

const freshChoice = { tamano: 'ALTO', leche: '', crema: '', extras: [] };
const categories = { CAFE: 'Cafés y bebidas', PASTELERIA: 'Pastelería', COMBO: 'Combos' };
export default function ClientCatalog() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [contact,setContact]=useState({nombre:'',telefono:'',correo:''});
  const { busy, message, error, action } = useAction();
  const [params] = useSearchParams();
  const [tab, setTab] = useState(() => user && params.get('seccion') === 'kappu' ? 'kappu' : 'menu');
  const [products, setProducts] = useState([]);
  const [couponInput, setCouponInput] = useState(''), [couponCode, setCouponCode] = useState(''), [couponVersion, setCouponVersion] = useState(0);
  const [quote, setQuote] = useState(null), [quoteError, setQuoteError] = useState('');
  const [options, setOptions] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [choice, setChoice] = useState(freshChoice);
  const [quantity, setQuantity] = useState(1);
  const [cart, setCart] = useSessionCart(user?.idUsuario ?? 'guest');
  const [cartOpen, setCartOpen] = useState(() => params.get('carrito') === '1');
  const [delivery, setDelivery] = useState('PICKUP');
  const [address, setAddress] = useState(user?.direccion||'');
  const [loadError, setLoadError] = useState('');
  const [added, setAdded] = useState(false);
  const [checkoutMessage,setCheckoutMessage]=useState('');
  const cartDialog = useRef(null);
  const addedTimer = useRef(null);
  const selected = products.find(p => p.idProducto === selectedId);
  const count = cart.reduce((sum, line) => sum + line.cantidad, 0);
  const showCartShortcut = count > 0 && !cartOpen && ['menu','catalog'].includes(tab);
  const subtotal = cart.reduce((sum, line) => sum + Math.round(line.precio * 100) * line.cantidad, 0) / 100;
  const quoteReady = quote && quote.subtotal === subtotal && quote.code === couponCode;
  const discount = quoteReady ? Number(quote.importe) : 0;
  const shipping = cart.length && delivery === 'DELIVERY' && subtotal < 50 ? 5 : 0;
  const unitPrice = configurationPrice(selected, options, choice);
  const remainingStock = Math.max(0, (selected?.stock || 0) - cart.filter(line => line.idProducto === selected?.idProducto).reduce((sum,line) => sum + line.cantidad,0));
  const ready = selected && remainingStock >= quantity && (!selected.personalizable || (choice.tamano && choice.leche && choice.crema));

  useEffect(() => {
    const controller = new AbortController();
    setQuoteError(''); setQuote(null);
    if (!count) { setQuote(null); return () => controller.abort(); }
    api('/commerce/quote', { method: 'POST', body: { subtotal, codigo: couponCode }, signal: controller.signal }).then(result => {
      if (!controller.signal.aborted) setQuote({ ...result, subtotal, code: couponCode });
    }).catch(e => { if (!controller.signal.aborted) { setQuote(null); setQuoteError(e.message); } });
    return () => controller.abort();
  }, [subtotal, count, couponCode, couponVersion]);
  async function refreshOrders() { if(user)setOrders(await api('/orders')); }
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([api('/products', { signal: controller.signal }), user ? api('/orders', { signal: controller.signal }) : Promise.resolve([]), api('/products/options', { signal: controller.signal })]).then(([p,o,config]) => {
      if (!controller.signal.aborted) { setProducts(p); setOrders(o); setOptions(config); }
    }).catch(e => { if (!controller.signal.aborted) setLoadError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => { controller.abort(); clearTimeout(addedTimer.current); };
  }, [user]);
  useEffect(() => {
    // Refresh on returning to the window so clients see newly published products/photos.
    const controller = new AbortController();
    const refresh = () => {
      if (document.visibilityState !== 'visible') return;
      api('/products', { signal: controller.signal }).then(list => {
        if (!controller.signal.aborted) setProducts(list);
      }).catch(() => {});
    };
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refresh);
    const timer = window.setInterval(refresh, 30000);
    return () => { controller.abort(); clearInterval(timer); window.removeEventListener('focus', refresh); document.removeEventListener('visibilitychange', refresh); };
  }, []);
  useEffect(() => {
    if (cartOpen && !cartDialog.current.open) cartDialog.current.showModal();
    if (!cartOpen && cartDialog.current.open) cartDialog.current.close();
  }, [cartOpen]);
  useEffect(()=>{const refresh=()=>user&&void api('/orders').then(setOrders).catch(()=>{});window.addEventListener('orders-updated',refresh);return()=>window.removeEventListener('orders-updated',refresh);},[user]);
  useEffect(()=>{const requested=params.get('seccion');if((user?['menu','catalog','orders','kappu','support']:['menu','catalog']).includes(requested)){setTab(requested);setCartOpen(false);setSelectedId(null);}},[params,user]);
  function selectProduct(product,source='menu') {
    setSelectedId(product.idProducto); setChoice(freshChoice); setQuantity(1); setAdded(false); setTab(source);
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
  async function checkout() {
    const pedido = { tipoEntrega: delivery, codigoCupon: couponCode || null, direccionEntrega: delivery === 'DELIVERY' ? address : null, detalles: cart.map(line => ({ idProducto: line.idProducto, cantidad: line.cantidad, personalizacion: line.personalizacion })) };
    if(!user){
      const result=await api('/guest/orders',{method:'POST',body:{pedido,contacto:contact}});
      rememberGuestOrder(result.pedido.idPedido,result.seguimientoToken);
      setCart([]);setCartOpen(false);navigate(trackingPath(result.pedido.idPedido,result.seguimientoToken));return;
    }
    await api('/orders', { method: 'POST', body: pedido });
    setCart([]);setCouponCode('');setCouponInput('');setCartOpen(false);setTab('orders');setCheckoutMessage('Pedido confirmado. El tiempo de preparación llegará a tus notificaciones.');
    await refreshOrders();window.scrollTo({top:0});
  }
  function openSection(section){setCartOpen(false);setSelectedId(null);setTab(section);if(section==='orders')void action(refreshOrders);}
  const navigation=user?<><button className={tab==='menu'?'nav-active':''} onClick={()=>openSection('menu')}>Menú</button><button className={tab==='catalog'?'nav-active':''} onClick={()=>openSection('catalog')}>Catálogo</button><button className={tab==='orders'?'nav-active':''} onClick={()=>openSection('orders')}>Mis pedidos</button><button className={tab==='kappu'?'nav-active':''} onClick={()=>openSection('kappu')}>Kappu</button><button className={tab==='support'?'nav-active':''} onClick={()=>openSection('support')}>Atención</button></>:<><button className={tab==='menu'?'nav-active':''} onClick={()=>openSection('menu')}>Menú</button><button className={tab==='catalog'?'nav-active':''} onClick={()=>openSection('catalog')}>Catálogo</button><Link to="/mis-pedidos">Mis pedidos</Link><Link to="/kappu">Kappu</Link><Link to="/?acceso=login">Iniciar sesión</Link></>;
  const tools=<><button className="delivery-header" onClick={()=>setCartOpen(true)}><MapPin size={17}/><span>{delivery==='PICKUP'?'Recojo en local':address||'Selecciona tu dirección'}</span></button><button className="cart-header" aria-label={`Ver mi pedido, ${count} productos`} onClick={()=>setCartOpen(true)}><ShoppingBag size={22}/><span>{count}</span></button></>;
  return <Layout variant="ordering" navigation={navigation} tools={tools} hideHeader={cartOpen}>
    <main className={`ordering-page ${['menu','catalog'].includes(tab) && selected ? 'has-order-bar' : ''} ${showCartShortcut ? 'has-cart-shortcut' : ''}`}>
      <div className="order-breadcrumb"><div><button onClick={() => { setTab('menu'); setSelectedId(null); }}>Menú</button><ChevronRight size={16}/>{['menu','catalog'].includes(tab) && selected ? <><button onClick={() => setSelectedId(null)}>{categories[selected.categoria]}</button><ChevronRight size={16}/><span>{selected.nombre}</span></> : <span>{tab === 'orders' ? 'Mis pedidos' : tab === 'kappu' ? 'Concurso Kappu' : tab === 'support' ? 'Atención al cliente' : tab==='catalog'?'Catálogo':'Todos los productos'}</span>}</div></div>
      <div className="ordering-content">
        {(error || loadError) && <p className="notice error" role="alert">{error || loadError}</p>}{(message||checkoutMessage) && <p className="notice success" role="status">{message||checkoutMessage}</p>}
        {loading ? <p role="status" className="loading">Preparando tu menú…</p> : ['menu','catalog'].includes(tab) ? selected && options ? <><ProductCustomizer product={selected} options={options} choice={choice} onChange={value => { setChoice(value); setAdded(false); }}/><ProductReviews key={selected.idProducto} product={selected} user={user}/></>
          : tab==='catalog'?<FullCatalog products={products} onSelect={p=>selectProduct(p,'catalog')}/>:<section className="ordering-menu"><div className="menu-intro"><p className="eyebrow">RADIOCAFÉ A TU MANERA</p><h1>Un momento para ti.</h1><p>Elige tu favorito. Nosotros lo preparamos a tu gusto.</p></div>
            <BaristaAvailability/><button className="kappu-catalog-invite" onClick={()=>user?setTab('kappu'):navigate('/kappu')}><img src="/assets/kappu-mascota.png" alt="Kappu, nuestro zorrito mascota"/><span><small>ARTE, CAFÉ Y COMUNIDAD</small><strong>Participa en el concurso de la mascota Kappu y llévate un premio</strong><span>Ver galería o participar</span></span></button>
            <CatalogBrowser products={products} onSelect={selectProduct}/>
            <CommerceShowcase/>
          </section> : tab === 'orders' ? <Orders orders={orders} role="CLIENTE" refresh={refreshOrders} action={action} busy={busy}/> : tab === 'support' ? <SupportPanel/> : <Contest user={user} action={action} busy={busy}/>}
      </div>
    </main>
    {['menu','catalog'].includes(tab) && selected && <div className={`product-order-bar ${showCartShortcut ? 'with-cart-shortcut' : ''}`}><div><button className="back-to-menu" onClick={() => setSelectedId(null)}><ArrowLeft size={18}/><span>Seguir explorando</span></button><span className="selection-hint" aria-live="polite">{added ? 'Tu producto se agregó al pedido' : !ready ? (remainingStock < quantity ? 'No hay stock para esta cantidad' : 'Selecciona las opciones obligatorias') : 'Hecho a tu manera'}</span><div className="order-bar-actions"><div className="order-stepper"><button aria-label="Reducir cantidad" disabled={quantity <= 1} onClick={() => setQuantity(q => q-1)}><Minus size={17}/></button><output aria-label="Cantidad">{quantity}</output><button aria-label="Aumentar cantidad" disabled={quantity >= Math.min(99, remainingStock)} onClick={() => setQuantity(q => q+1)}><Plus size={17}/></button></div><button className={`add-custom-product ${added ? 'added' : ''}`} disabled={!ready || busy} onClick={add}>{added ? <><Check size={18}/> Agregado a tu pedido</> : <>Agregar por {money(unitPrice * quantity)}</>}</button></div></div></div>}
    {showCartShortcut&&<aside className="catalog-cart-shortcut" aria-label="Acceso rápido al carrito"><div><span className="cart-shortcut-summary" aria-live="polite"><strong>{count} {count===1?'producto':'productos'} en tu carrito</strong><small>Subtotal: {money(subtotal)}</small></span><button type="button" className="button" aria-label={`Ver carrito, ${count} ${count===1?'producto':'productos'}`} onClick={()=>setCartOpen(true)}><ShoppingBag size={20}/><span>Ver carrito</span><ChevronRight size={19}/></button></div></aside>}
    <Checkout guest={!user} contact={contact} setContact={setContact} navigation={navigation} tools={tools} dialogRef={cartDialog} open={cartOpen} onClose={()=>setCartOpen(false)} onExplore={()=>{setCartOpen(false);setTab('menu');setSelectedId(null);}} cart={cart} delivery={delivery} setDelivery={setDelivery} address={address} setAddress={setAddress} couponInput={couponInput} setCouponInput={setCouponInput} couponCode={couponCode} applyCoupon={()=>{setCouponCode(couponInput.trim().toUpperCase());setCouponVersion(v=>v+1);}} removeCoupon={()=>{setCouponInput('');setCouponCode('');}} quoteError={quoteError} quoteReady={quoteReady} quote={quote} subtotal={subtotal} shipping={shipping} discount={discount} changeQuantity={changeQuantity} remove={key=>setCart(previous=>previous.filter(p=>p.key!==key))} onCheckout={checkout}/>

  </Layout>;
}
