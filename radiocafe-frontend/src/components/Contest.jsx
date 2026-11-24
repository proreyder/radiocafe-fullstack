import { useEffect, useRef, useState } from 'react';
import { api } from '../lib/api';
import { Heart, Trophy, ImagePlus, CalendarDays } from 'lucide-react';
import { Link } from 'react-router-dom';
import '../pages/Experience.css';
function PrizeQr({ id }) {
  const [url, setUrl] = useState(''), [error, setError] = useState('');
  useEffect(() => {
    const controller = new AbortController(); let objectUrl;
    api(`/kappu/${id}/qr`, { signal: controller.signal, responseType: 'blob' }).then(blob => {
      if (!controller.signal.aborted) { objectUrl = URL.createObjectURL(blob); setUrl(objectUrl); }
    }).catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => { controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [id]);
  return error ? <p role="alert">{error}</p> : url ? <img className="qr" src={url} alt="Código QR para recoger el premio"/> : <p role="status">Cargando QR…</p>;
}
export default function Contest({ user, action, busy }) {
 const [items,setItems]=useState([]),[status,setStatus]=useState(null),[view,setView]=useState('gallery'),[loading,setLoading]=useState(true),[initialError,setInitialError]=useState('');
 const [form,setForm]=useState({tituloObra:'',nombreParticipante:user?.nombre||'',descripcion:'',imagenUrl:''}),[uploading,setUploading]=useState(false),[voted,setVoted]=useState(false);
 const [schedule,setSchedule]=useState({inicio:'',fin:''});
 const scheduleInitialized=useRef(false);
 const [code,setCode]=useState(''),[scanError,setScanError]=useState(''),[scanning,setScanning]=useState(false);const scanner=useRef(null);
 const client=user?.role==='CLIENTE',admin=user?.role==='ADMINISTRADOR',cashier=admin||user?.role==='BARISTA';
 const open=status?.fase==='ABIERTO';const winner=items.find(i=>i.esGanador);
 async function load(signal) {
  const gallery=await api('/kappu/gallery',{signal});
  const privateItems=user?await api('/kappu',{signal}):[];
  const vote=client?await api('/kappu/my-vote',{signal}):{votoRegistrado:false};
  if(signal?.aborted)return;
  setItems(gallery.ilustraciones.map(i=>({...i,...privateItems.find(p=>p.idIlustracion===i.idIlustracion)})));setStatus(gallery.estado);setVoted(vote.votoRegistrado);setInitialError('');
  if(!scheduleInitialized.current){if(gallery.estado.inicio&&gallery.estado.fin)setSchedule({inicio:gallery.estado.inicio.slice(0,16),fin:gallery.estado.fin.slice(0,16)});scheduleInitialized.current=true;}
 }
 async function refresh() {await load();}
 useEffect(()=>{const controller=new AbortController();void load(controller.signal).catch(e=>{if(!controller.signal.aborted)setInitialError(e.message);}).finally(()=>{if(!controller.signal.aborted)setLoading(false);});const timer=setInterval(()=>{if(document.visibilityState==='visible')void load(controller.signal).catch(()=>{});},15000);return()=>{controller.abort();clearInterval(timer);};},[user?.idUsuario]);
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

 async function upload(e) {
  const file=e.target.files?.[0];e.target.value='';if(!file)return;
  if(!['image/jpeg','image/png'].includes(file.type)||file.size>5*1024*1024){setInitialError('Sube una imagen JPG o PNG de hasta 5 MB.');return;}
  setUploading(true);setInitialError('');try {const body=new FormData();body.append('file',file);const saved=await api('/kappu/images',{method:'POST',body});setForm(p=>({...p,imagenUrl:saved.url}));}catch(e){setInitialError(e.message);}finally {setUploading(false);}
 }
 async function participate(e) {e.preventDefault();if(!form.imagenUrl){setInitialError('Sube la imagen de tu ilustración.');return;}await action(async()=>{await api('/kappu/entries',{method:'POST',body:form});setForm({tituloObra:'',nombreParticipante:user?.nombre||'',descripcion:'',imagenUrl:''});setView('gallery');await refresh();},'Tu ilustración está inscrita y el administrador recibió la notificación.');}
 async function vote(id) {await action(async()=>{await api(`/kappu/${id}/votes`,{method:'POST'});await refresh();},'¡Tu like quedó registrado!');}
 async function choose(id) {if(!window.confirm('¿Publicar esta obra como ganadora? La elección es definitiva y se generará su premio.'))return;await action(async()=>{await api(`/kappu/select-winner/${id}`,{method:'POST'});await refresh();},'Ganador publicado en la galería pública.');}
 async function close() {if(!window.confirm('¿Cerrar las inscripciones y los likes ahora? Los tres finalistas quedarán fijados.'))return;await action(async()=>{await api('/kappu/close',{method:'POST'});await refresh();},'Concurso cerrado. Selecciona al ganador entre los finalistas.');}
 async function redeem(e) {e.preventDefault();await action(async()=>{await api('/kappu/redeem-prize',{method:'POST',body:{codigoPremio:code.trim()}});setCode('');await refresh();},'Premio canjeado correctamente.');}
 const field=name=>({value:form[name],onChange:e=>setForm({...form,[name]:e.target.value})});
 return <section className="kappu-experience stack"><div className="kappu-hero"><img src="/assets/kappu-mascota.png" alt="Kappu, el zorrito mascota de RadioCafé"/><div><p className="eyebrow">EL ZORRITO QUE INSPIRA TU CREATIVIDAD</p><h1>Imagina a Kappu.<br/>Comparte tu arte.</h1><p>Participa en el concurso de nuestra mascota Kappu y llévate un premio. Crea tu propia ilustración del zorrito y compártela con la comunidad.</p><div className="actions"><button className={`button ${view==='gallery'?'':'secondary'}`} onClick={()=>setView('gallery')}>Ver galería</button><button className={`button ${view==='participate'?'':'secondary'}`} onClick={()=>setView('participate')}>Participar</button></div></div></div>
 {initialError&&<p className="notice error" role="alert">{initialError}</p>}
 {loading?<p role="status">Preparando la galería de Kappu…</p>:<><div className="kappu-rules"><span><CalendarDays size={18}/>{status?.fase==='ABIERTO'?'Inscripciones y likes abiertos':status?.fase==='PROXIMAMENTE'?'El concurso abre próximamente':status?.fase==='GANADOR'?'Ganador publicado':'Concurso cerrado · finalistas definidos'}</span><span>{status?.fin?`Cierre: ${new Date(status.fin).toLocaleString('es-PE')} · Lima`:'Fecha de cierre por anunciar'}</span><small>Un like por cuenta. Hasta tres finalistas por likes; empates por orden de inscripción.</small></div>
 {admin&&!status?.cerrado&&<form className="card stack kappu-admin-schedule" onSubmit={e=>{e.preventDefault();void action(async()=>{await api('/kappu/schedule',{method:'PUT',body:schedule});await refresh();},'Fechas del concurso actualizadas.');}}><h2>Administrar fechas y cierre</h2><div className="form-grid"><label>Inicio (hora de Lima)<input required type="datetime-local" value={schedule.inicio} onChange={e=>setSchedule({...schedule,inicio:e.target.value})}/></label><label>Cierre (hora de Lima)<input required type="datetime-local" value={schedule.fin} onChange={e=>setSchedule({...schedule,fin:e.target.value})}/></label><button className="button" disabled={busy}>Guardar fechas</button></div><button type="button" className="button secondary" disabled={busy} onClick={()=>void close()}>Cerrar inscripciones y likes ahora</button></form>}
 {winner&&<section className="kappu-winner-announcement"><Trophy size={38}/><div><p className="eyebrow">¡LA COMUNIDAD YA TIENE GANADOR!</p><h2>{winner.nombreParticipante||`Participante #${winner.idUsuario}`}</h2><p>Su obra «{winner.tituloObra}» fue elegida ganadora del concurso Kappu.</p></div><img src={winner.imagenUrl} alt={`Obra ganadora: ${winner.tituloObra}`}/></section>}
 {view==='participate'?client?(open?<form className="card kappu-entry-form" onSubmit={participate}><div><p className="eyebrow">DALE TU ESTILO A NUESTRO ZORRITO</p><h2>Inscribe tu ilustración</h2><p>El nombre del participante, la descripción y la obra se publicarán en la galería.</p></div><label>Nombre del participante o nombre artístico<input required maxLength={100} {...field('nombreParticipante')}/></label><label>Nombre de tu ilustración<input required maxLength={150} {...field('tituloObra')}/></label><label>Descripción de tu arte<textarea required minLength={5} maxLength={2000} {...field('descripcion')}/></label><div className="kappu-image-upload"><ImagePlus size={28}/><label>Subir imagen de Kappu<input type="file" accept="image/png,image/jpeg,.png,.jpg,.jpeg" disabled={uploading||busy} onChange={e=>void upload(e)}/></label><small>JPG o PNG · Hasta 5 MB · Máximo 12 megapíxeles</small>{form.imagenUrl&&<img src={form.imagenUrl} alt="Vista previa de tu ilustración de Kappu"/>}</div><button className="button" disabled={busy||uploading||!form.imagenUrl}>{uploading?'Subiendo ilustración…':'Inscribir mi arte'}</button></form>:<p className="notice">El concurso no acepta inscripciones en este momento. Puedes disfrutar la galería.</p>):<div className="card stack"><h2>Únete al concurso</h2><p>{user?'La participación está disponible para las cuentas de cliente.':'Inicia sesión con tu cuenta de cliente para subir tu ilustración.'}</p>{!user&&<Link className="button" to="/">Iniciar sesión para participar</Link>}</div>:<>
 <div className="section-heading"><div><p className="eyebrow">CREADO POR NUESTRA COMUNIDAD</p><h2>{status?.cerrado&&!winner?'Galería y finalistas de Kappu':'Galería de ilustraciones'}</h2><p>{voted?'Ya registraste tu like en este concurso.':'Descubre las obras y apoya a tu favorita.'}</p></div><button className="button secondary" disabled={busy} onClick={()=>action(refresh)}>Actualizar galería</button></div>
 {!items.length&&<div className="kappu-gallery-empty"><img src="/assets/kappu-mascota.png" alt="Nuestro zorrito Kappu"/><h3>La próxima versión de Kappu puede ser la tuya</h3><p>La galería está esperando el arte de nuestra comunidad.</p><button className="button" onClick={()=>setView('participate')}>Quiero participar</button></div>}
 <div className="kappu-gallery">{items.map(i=><article className={`kappu-art-card ${i.esGanador?'winner':i.esFinalista?'finalist':''}`} key={i.idIlustracion}><img className="kappu-art-image" src={i.imagenUrl} alt={i.tituloObra} loading="lazy" referrerPolicy="no-referrer"/><div className="kappu-art-details"><span className="badge">{i.esGanador?'Ganador':i.esFinalista?'Finalista':`${i.votosCount} likes`}</span><h3>{i.tituloObra}</h3><strong>{i.nombreParticipante||`Participante #${i.idUsuario}`}</strong><p className="commerce-text">{i.descripcion||'Una interpretación de nuestro zorrito Kappu.'}</p><span className="kappu-like-count"><Heart size={17}/>{i.votosCount} likes</span>{client&&open&&<button className="button secondary" disabled={busy||voted} onClick={()=>void vote(i.idIlustracion)}><Heart size={17}/>{voted?'Like registrado':'Me gusta'}</button>}{!user&&open&&<Link className="text-button" to="/">Inicia sesión para dar tu like</Link>}{admin&&status?.cerrado&&!winner&&i.esFinalista&&<button className="button" disabled={busy} onClick={()=>void choose(i.idIlustracion)}>Elegir como ganador</button>}{i.esGanador&&i.codigoPremio&&<div className="prize"><strong>{i.estadoPremio==='RECLAMADO'?'Premio ya recogido':'Presenta este QR en caja'}</strong>{i.estadoPremio!=='RECLAMADO'&&<PrizeQr id={i.idIlustracion}/>}<code>{i.codigoPremio}</code></div>}</div></article>)}</div></>}
 {cashier&&<form className="card stack" onSubmit={redeem}><h2>Canjear premio en caja</h2><p>Escanea el QR del ganador o escribe su código. El canje se realiza al confirmar.</p><label>Código del premio<input required maxLength={50} value={code} onChange={e=>setCode(e.target.value)} placeholder="KAPPU-WIN-…"/></label><div className="actions"><button className="button" disabled={busy}>Confirmar canje</button><button type="button" className="button secondary" disabled={busy} onClick={()=>{if(scanning)void stopScanner().catch(e=>setScanError(e.message));else void scan();}}>{scanning?'Cerrar cámara':'Escanear QR'}</button></div>{scanError&&<p className="notice error" role="alert">{scanError}</p>}<div id="qr-reader" className={scanning?'scanner':'scanner hidden'}/></form>}
 </>}</section>;
}

export function KappuWinnerBanner() {
 const [winner,setWinner]=useState(null);
 useEffect(()=>{let active=true;const load=()=>api('/kappu/gallery').then(data=>{if(active)setWinner(data.ilustraciones.find(i=>i.esGanador)||null);}).catch(()=>{});void load();const timer=setInterval(()=>{if(document.visibilityState==='visible')void load();},30000);return()=>{active=false;clearInterval(timer);};},[]);
 return winner&&<section className="home-kappu-winner"><Trophy size={32}/><div><p className="eyebrow">GANADOR DEL CONCURSO KAPPU</p><h2>¡Felicidades, {winner.nombreParticipante||'artista ganador'}!</h2><p>«{winner.tituloObra}» es la nueva obra ganadora de nuestra comunidad.</p></div><Link className="button" to="/kappu">Conoce la obra ganadora</Link></section>;
}
