import { useEffect, useId, useState } from 'react';
import { Upload, ImagePlus, Check } from 'lucide-react';
import { api } from '../lib/api';

export default function ProductImagesPanel({ value = '', onSelect, disabled = false, onBusyChange, library = false }) {
  const inputId = useId();
  const [images, setImages] = useState([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [limit, setLimit] = useState(12);
  useEffect(() => {
    const controller = new AbortController();
    api('/images', { signal: controller.signal }).then(list => { if (!controller.signal.aborted) setImages(list); })
      .catch(e => { if (!controller.signal.aborted) setError(e.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, []);
  async function upload(e) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError(''); setMessage('');
    if (!['image/png', 'image/jpeg'].includes(file.type) || !file.size || file.size > 5 * 1024 * 1024) {
      setError('Selecciona una imagen JPG o PNG de hasta 5 MB.'); return;
    }
    setBusy(true); onBusyChange?.(true);
    try {
      const body = new FormData(); body.append('file', file);
      const image = await api('/images', { method: 'POST', body });
      setImages(previous => [image, ...previous]);
      if (!library) onSelect(image.url);
      setMessage(library ? 'Imagen subida. Pulsa «Usar en producto» para agregarla al catálogo.' : 'Foto seleccionada. Guarda el producto para publicarla en el menú.');
    } catch (err) { setError(err.message); }
    finally { setBusy(false); onBusyChange?.(false); }
  }
  return <section className="product-images-panel" aria-label="Imágenes de productos">
    <div className="image-upload-intro"><ImagePlus size={24}/><div><h3>{library ? 'Biblioteca de imágenes' : 'Foto del producto'}</h3><p>Sube fotos de café, panes o pasteles. JPG o PNG, máximo 5 MB y 12 megapíxeles.</p></div></div>
    {error && <p className="notice error" role="alert">{error}</p>}
    {message && <p className="notice success" role="status">{message}</p>}
    <label htmlFor={inputId} className="image-upload-label"><Upload size={18}/>{busy ? 'Subiendo imagen…' : 'Subir imagen desde mi equipo'}</label>
    <input id={inputId} type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" disabled={disabled || busy} onChange={upload} aria-label="Subir imagen de producto"/>
    {!library && value && <div className="image-selection"><img src={value} alt="Vista previa de la foto del producto"/><div><strong>Foto que verá el cliente</strong><p>Se publica al guardar el producto.</p><button type="button" className="text-button" disabled={busy || disabled} onClick={() => onSelect('')}>Quitar foto</button></div></div>}
    {loading ? <p role="status">Cargando imágenes…</p> : images.length > 0 ? <><p className="muted">{library ? 'Elige una foto y completa los datos del producto para publicarlo.' : 'También puedes elegir una foto de la biblioteca.'}</p><div className="product-image-library">{images.slice(0, limit).map((image, index) => <button type="button" key={image.url} className={`image-library-item ${value === image.url ? 'selected' : ''}`} disabled={busy || disabled} onClick={() => onSelect(image.url)} aria-label={`${library ? 'Usar en producto' : 'Seleccionar imagen'} ${index + 1}`} aria-pressed={value === image.url}>
      <img src={image.url} alt={`Foto de producto ${index + 1}`} loading="lazy"/><span>{value === image.url ? <><Check size={16}/> Seleccionada</> : library ? 'Usar en producto' : 'Seleccionar'}</span>
    </button>)}</div>{images.length > limit && <button type="button" className="text-button" onClick={() => setLimit(n => n + 12)}>Ver más imágenes</button>}</> : <p className="muted">Todavía no hay imágenes. Sube tu primera foto para usarla en el catálogo.</p>}
  </section>;
}
