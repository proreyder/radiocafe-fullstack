import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Pencil, Trash2 } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { api, money } from '../lib/api';
import useAction from '../lib/useAction';
import Layout from '../components/Layout';
import Orders from '../components/Orders';
import Contest from '../components/Contest';
import InventoryPanel from '../components/InventoryPanel';
import UsersPanel from '../components/UsersPanel';
import './Operations.css';
const blank = { nombre: '', descripcion: '', categoria: 'CAFE', precio: '', imagenUrl: '', personalizable: false };
export default function AdminDashboard() {
  const { user } = useAuth();
  const admin = user.role === 'ADMINISTRADOR';
  const [params] = useSearchParams();
  const [tab, setTab] = useState(() => params.get('seccion') === 'kappu' && user.role !== 'REPARTIDOR' ? 'kappu' : 'orders');
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(null);
  const [initialError, setInitialError] = useState('');
  const { busy, message, error, action } = useAction();
  async function refreshOrders() { setOrders(await api('/orders')); }
  async function refreshProducts() { setProducts(await api('/products')); }
  useEffect(() => { let active = true; Promise.all([api('/products'), api('/orders')]).then(([p,o]) => { if (active) { setProducts(p); setOrders(o); } }).catch(e => { if (active) setInitialError(e.message); }); return () => { active = false; }; }, []);
  async function save(e) { e.preventDefault(); await action(async () => { await api(`/products${editing ? `/${editing}` : ''}`, { method: editing ? 'PUT' : 'POST', body: { ...form, precio: Number(form.precio) } }); setForm(blank); setEditing(null); await refreshProducts(); }, 'Catálogo actualizado. Los productos nuevos empiezan sin stock; registra su entrada en Inventario.'); }
  function edit(p) { setEditing(p.idProducto); setForm({ nombre: p.nombre, descripcion: p.descripcion || '', categoria: p.categoria, precio: p.precio, imagenUrl: p.imagenUrl || '', personalizable: Boolean(p.personalizable) }); }
  async function remove(p) { if (!window.confirm(`¿Retirar ${p.nombre} del catálogo? Los pedidos anteriores se conservarán.`)) return; await action(async () => { await api(`/products/${p.idProducto}`, { method: 'DELETE' }); await refreshProducts(); }, 'Producto retirado.'); }
  const field = key => ({ value: form[key], onChange: e => setForm({ ...form, [key]: e.target.value }) });
  return <Layout><main className="container page"><p className="eyebrow">RADIOCAFÉ · {user.role}</p><h1>Panel de trabajo</h1><p>Gestiona la experiencia de nuestra comunidad.</p>
    <div className="stats"><div className="card"><strong>{orders.filter(o => !['ENTREGADO','CANCELLED'].includes(o.estadoPedido)).length}</strong><span>Pedidos activos</span></div><div className="card"><strong>{orders.filter(o => o.estadoPedido === 'ENTREGADO').length}</strong><span>Entregados</span></div>{admin && <div className="card"><strong>{products.length}</strong><span>Productos disponibles</span></div>}</div>
    {admin && <p className="api-explorer-link"><a href="/api-explorer/index.html" target="_blank" rel="noopener noreferrer">Abrir documentación interactiva de la API</a></p>}
    <div className="tabs" role="tablist" aria-label="Panel de trabajo">{[['orders','Pedidos'], ...(admin ? [['catalog','Catálogo'],['inventory','Inventario'],['users','Usuarios']] : []), ...(user.role !== 'REPARTIDOR' ? [['kappu','Concurso y canje']] : [])].map(([id,label]) => <button role="tab" aria-selected={tab === id} className={tab === id ? 'active' : ''} key={id} onClick={() => setTab(id)}>{label}</button>)}</div>
    {(error || initialError) && <p className="notice error" role="alert">{error || initialError}</p>}{message && <p className="notice success" role="status">{message}</p>}
    {tab === 'inventory' ? <InventoryPanel action={action} busy={busy}/> : tab === 'users' ? <UsersPanel user={user} action={action} busy={busy}/> : tab === 'orders' ? <Orders orders={orders} role={user.role} refresh={refreshOrders} action={action} busy={busy} /> : tab === 'kappu' ? <Contest user={user} action={action} busy={busy} /> : <section className="stack"><div className="section-heading"><h2>{editing ? 'Editar producto' : 'Agregar producto'}</h2><button className="button secondary" disabled={busy} onClick={() => action(refreshProducts)}>Actualizar</button></div>
      <form className="card form-grid" onSubmit={save}><label>Nombre<input required maxLength={120} {...field('nombre')} /></label><label>Categoría<select {...field('categoria')}><option>CAFE</option><option>PASTELERIA</option><option>COMBO</option></select></label><label>Precio en USD<input required type="number" min="0.01" max="99999.99" step="0.01" {...field('precio')} /></label><label>Descripción<input maxLength={2000} {...field('descripcion')} /></label><label>Imagen HTTPS (opcional)<input type="url" pattern="https://.*" maxLength={2000} {...field('imagenUrl')} /></label><label className="personalizable-toggle"><input type="checkbox" checked={form.personalizable} onChange={e => setForm({ ...form, personalizable: e.target.checked })}/>Permitir tamaño, leche y extras</label><div className="actions"><button className="button" disabled={busy}>{editing ? 'Guardar cambios' : 'Agregar producto'}</button>{editing && <button type="button" className="button secondary" onClick={() => { setForm(blank); setEditing(null); }}>Cancelar</button>}</div></form>
      <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Acciones</th></tr></thead><tbody>{products.map(p => <tr key={p.idProducto}><td><strong>{p.nombre}</strong><p className="muted">{p.descripcion}</p></td><td>{p.categoria}</td><td>{money(p.precio)}</td><td>{p.stock || 0}</td><td><div className="actions"><button className="button secondary" disabled={busy} onClick={() => edit(p)} aria-label={`Editar ${p.nombre}`}><Pencil size={15}/></button><button className="button secondary" disabled={busy} onClick={() => remove(p)} aria-label={`Retirar ${p.nombre}`}><Trash2 size={15}/></button></div></td></tr>)}</tbody></table></div>
    </section>}
  </main></Layout>;
}
