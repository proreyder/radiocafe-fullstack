import { useEffect, useState } from 'react';
import { api } from '../lib/api';
const types = { INICIAL: 'Saldo inicial', AJUSTE: 'Ajuste manual', VENTA: 'Pedido confirmado', DEVOLUCION: 'Cancelación' };
export default function InventoryPanel({ action, busy }) {
  const [products, setProducts] = useState([]);
  const [movements, setMovements] = useState([]);
  const [selected, setSelected] = useState('');
  const [delta, setDelta] = useState('');
  const [reason, setReason] = useState('');
  const [search, setSearch] = useState('');
  const [loadError, setLoadError] = useState('');
  async function refresh() {
    const [items, history] = await Promise.all([api('/inventory'), api('/inventory/movements')]);
    setProducts(items); setMovements(history); setLoadError('');
  }
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([api('/inventory', { signal: controller.signal }), api('/inventory/movements', { signal: controller.signal })])
      .then(([items, history]) => { if (!controller.signal.aborted) { setProducts(items); setMovements(history); } })
      .catch(e => { if (!controller.signal.aborted) setLoadError(e.message); });
    return () => controller.abort();
  }, []);
  async function adjust(e) {
    e.preventDefault();
    if (!Number.isInteger(Number(delta)) || Number(delta) === 0) { setLoadError('Ingresa una cantidad entera distinta de cero.'); return; }
    await action(async () => {
      await api(`/inventory/${selected}/adjustments`, { method: 'POST', body: { cantidad: Number(delta), motivo: reason.trim() } });
      setDelta(''); setReason(''); await refresh();
    }, 'Ajuste registrado. El saldo y el historial se actualizaron.');
  }
  const visible = products.filter(p => `${p.nombre} ${p.categoria}`.toLowerCase().includes(search.toLowerCase()));
  const active = products.filter(p => p.disponible);
  const low = active.filter(p => (p.stock || 0) <= 5);
  return <section className="stack operations-panel">
    <div className="section-heading"><div><h2>Inventario</h2><p>Existencias por producto. Los pedidos descuentan stock y las cancelaciones lo devuelven.</p></div><button className="button secondary" disabled={busy} onClick={() => action(refresh)}>Actualizar inventario</button></div>
    {loadError && <p className="notice error" role="alert">{loadError}</p>}
    <div className="inventory-summary"><article><strong>{active.reduce((sum,p) => sum + (p.stock || 0),0)}</strong><span>Unidades en catálogo</span></article><article><strong>{low.length}</strong><span>Productos con 5 unidades o menos</span></article><article><strong>{active.filter(p => !p.stock).length}</strong><span>Productos agotados</span></article></div>
    <form className="card form-grid stock-adjustment" onSubmit={adjust}><label>Producto<select required value={selected} onChange={e => setSelected(e.target.value)}><option value="">Selecciona un producto</option>{products.map(p => <option key={p.idProducto} value={p.idProducto}>{p.nombre} · {p.stock || 0} unidades{!p.disponible ? ' · Retirado' : ''}</option>)}</select></label><label>Entrada (+) o salida (−)<input required type="number" min="-100000" max="100000" step="1" placeholder="Ej.: 20 o -3" value={delta} onChange={e => setDelta(e.target.value)}/></label><label>Motivo del movimiento<input required minLength={5} maxLength={200} placeholder="Reposición, merma o corrección" value={reason} onChange={e => setReason(e.target.value)}/></label><button className="button" disabled={busy || !selected}>Registrar ajuste</button><p className="operations-help">Un producto nuevo empieza con stock cero. Registra aquí su primera entrada. El saldo nunca puede quedar negativo.</p></form>
    <div className="section-heading"><h3>Existencias actuales</h3><label className="operations-search">Buscar producto<input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Nombre o categoría"/></label></div>
    <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Categoría</th><th>Stock</th><th>Estado</th></tr></thead><tbody>{visible.map(p => <tr key={p.idProducto}><td><strong>{p.nombre}</strong></td><td>{p.categoria}</td><td><strong className={(p.stock || 0) <= 5 ? 'stock-low' : ''}>{p.stock || 0}</strong></td><td><span className={`operation-status ${!p.stock ? 'warning' : ''}`}>{!p.disponible ? 'Retirado del catálogo' : !p.stock ? 'Agotado' : p.stock <= 5 ? 'Stock bajo' : 'Disponible'}</span></td></tr>)}</tbody></table>{!visible.length && <p className="empty">No hay productos para esa búsqueda.</p>}</div>
    <div><h3>Últimos movimientos</h3><p className="operations-help">Hasta 200 registros recientes. Se conservan el motivo, el saldo, el responsable y el pedido asociado.</p></div>
    <div className="table-wrap"><table><thead><tr><th>Fecha</th><th>Producto</th><th>Movimiento</th><th>Cantidad</th><th>Saldo</th><th>Responsable / pedido</th></tr></thead><tbody>{movements.map(m => <tr key={m.id}><td>{new Date(m.fecha).toLocaleString('es-PE')}</td><td>{m.nombreProducto}</td><td><strong>{types[m.tipo] || m.tipo}</strong><p className="operations-help">{m.motivo}</p></td><td className={m.cantidad < 0 ? 'stock-low' : 'stock-entry'}>{m.cantidad > 0 ? '+' : ''}{m.cantidad}</td><td>{m.saldo}</td><td>{m.idUsuario ? `Usuario #${m.idUsuario}` : 'Sistema'}{m.idPedido && <p className="operations-help">Pedido #{m.idPedido}</p>}</td></tr>)}</tbody></table>{!movements.length && <p className="empty">Todavía no hay movimientos.</p>}</div>
  </section>;
}
