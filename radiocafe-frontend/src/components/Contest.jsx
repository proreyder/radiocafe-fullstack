import { useEffect, useRef, useState } from 'react';
import { api } from '../lib/api';
export default function Contest({ user, action, busy }) {
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState('');
  const [image, setImage] = useState('');
  const [code, setCode] = useState('');
  const [initialError, setInitialError] = useState('');
  const [scanError, setScanError] = useState('');
  const [scanning, setScanning] = useState(false);
  const scanner = useRef(null);
  const client = user.role === 'CLIENTE';
  const admin = user.role === 'ADMINISTRADOR';
  const cashier = admin || user.role === 'BARISTA';
  const closed = items.some(i => i.esGanador);
  async function refresh() { setItems(await api('/kappu')); }
  useEffect(() => { let active = true; api('/kappu').then(data => { if (active) setItems(data); }).catch(e => { if (active) setInitialError(e.message); }); return () => { active = false; }; }, []);
  async function stopScanner() { const current = scanner.current; scanner.current = null; if (current) { if (current.isScanning) await current.stop(); current.clear(); } setScanning(false); }
  useEffect(() => () => { const current = scanner.current; if (current?.isScanning) current.stop().then(() => current.clear()).catch(() => {}); }, []);
  async function scan() {
    setScanError(''); setScanning(true);
    try {
      const { Html5Qrcode } = await import('html5-qrcode');
      const instance = new Html5Qrcode('qr-reader'); scanner.current = instance;
      await instance.start({ facingMode: 'environment' }, { fps: 8, qrbox: { width: 220, height: 220 } }, decoded => { setCode(decoded); void stopScanner().catch(e => setScanError(e.message)); }, () => {});
    } catch { setScanError('No se pudo abrir la cámara. Permite el acceso o escribe el código del premio.'); setScanning(false); scanner.current = null; }
  }
  async function participate(e) { e.preventDefault(); await action(async () => { await api('/kappu', { method: 'POST', body: { tituloObra: title, imagenUrl: image } }); setTitle(''); setImage(''); await refresh(); }, 'Tu ilustración ya participa.'); }
  async function vote(id) { await api(`/kappu/${id}/votes`, { method: 'POST' }); await refresh(); }
  async function choose(id) { if (!window.confirm('¿Confirmar este ganador? El concurso se cerrará y no se podrá cambiar.')) return; await action(async () => { await api(`/kappu/select-winner/${id}`, { method: 'POST' }); await refresh(); }, 'Ganador confirmado. El premio ya tiene un QR.'); }
  async function redeem(e) { e.preventDefault(); await action(async () => { await api('/kappu/redeem-prize', { method: 'POST', body: { codigoPremio: code.trim() } }); setCode(''); await refresh(); }, 'Premio canjeado correctamente.'); }
  return <section className="stack"><div className="section-heading"><div><p className="eyebrow">ARTE EN COMUNIDAD</p><h2>Concurso Kappu</h2><p>Un voto por cuenta. Un ganador y un premio para recoger en RadioCafé.</p></div><button className="button secondary" disabled={busy} onClick={() => action(refresh)}>Actualizar</button></div>
    {initialError && <p role="alert" className="notice error">{initialError}</p>}
    {closed && <p className="notice">El concurso está cerrado. Ya tenemos una ilustración ganadora.</p>}
    {client && !closed && <form className="card form-grid" onSubmit={participate}><label>Título de tu ilustración<input required maxLength={150} value={title} onChange={e => setTitle(e.target.value)} /></label><label>Enlace HTTPS a la imagen<input required type="url" pattern="https://.*" maxLength={2000} placeholder="https://…" value={image} onChange={e => setImage(e.target.value)} /></label><button className="button" disabled={busy}>Participar</button></form>}
    {items.length === 0 && <p className="empty">Aún no hay ilustraciones. Comparte la primera.</p>}
    <div className="product-grid">{items.map(i => <article className={`card illustration ${i.esGanador ? 'winner' : ''}`} key={i.idIlustracion}>
      <img src={i.imagenUrl} alt={i.tituloObra} loading="lazy" referrerPolicy="no-referrer" onError={e => { e.currentTarget.alt = 'Imagen no disponible'; }} />
      <span className="badge">{i.esGanador ? 'Ganador Kappu' : `${i.votosCount} votos`}</span><h3>{i.tituloObra}</h3><p className="muted">Participante #{i.idUsuario}</p>
      {client && !closed && <button className="button secondary" disabled={busy} onClick={() => action(() => vote(i.idIlustracion), 'Tu voto fue registrado.')}>Votar</button>}
      {admin && !closed && <button className="button" disabled={busy} onClick={() => choose(i.idIlustracion)}>Elegir ganador</button>}
      {i.esGanador && i.codigoPremio && <div className="prize"><p><strong>{i.estadoPremio === 'RECLAMADO' ? 'Premio ya recogido' : 'Presenta este QR en caja'}</strong></p>{i.estadoPremio !== 'RECLAMADO' && <img className="qr" src={`/api/v1/kappu/${i.idIlustracion}/qr`} alt="Código QR para recoger el premio" />}<code>{i.codigoPremio}</code></div>}
    </article>)}</div>
    {cashier && <form className="card stack" onSubmit={redeem}><h3>Canjear premio en caja</h3><p>Escanea el QR del ganador o escribe su código. El canje se realiza al confirmar.</p><label>Código del premio<input required maxLength={50} value={code} onChange={e => setCode(e.target.value)} placeholder="KAPPU-WIN-…" /></label>
      <div className="actions"><button className="button" disabled={busy}>Confirmar canje</button><button type="button" className="button secondary" disabled={busy} onClick={() => { if (scanning) void stopScanner().catch(e => setScanError(e.message)); else void scan(); }}>{scanning ? 'Cerrar cámara' : 'Escanear QR'}</button></div>
      {scanError && <p role="alert" className="notice error">{scanError}</p>}<div id="qr-reader" className={scanning ? 'scanner' : 'scanner hidden'} /></form>}
  </section>;
}
