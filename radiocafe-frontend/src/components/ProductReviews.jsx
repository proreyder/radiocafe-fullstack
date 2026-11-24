import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import useAction from '../lib/useAction';

export default function ProductReviews({ product, user }) {
  const [reviews, setReviews] = useState([]);
  const [stars, setStars] = useState(5);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [revision, setRevision] = useState(0);
  const { busy, error, message, action } = useAction();
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setLoadError('');
    api(`/commerce/reviews?producto=${product.idProducto}`, { signal: controller.signal })
      .then(list => { if (!controller.signal.aborted) setReviews(list); })
      .catch(e => { if (!controller.signal.aborted) setLoadError(e.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [product.idProducto, revision]);

  const ownReview = reviews.some(r => r.usuario === user.idUsuario);
  const average = reviews.length ? (reviews.reduce((sum,r) => sum + r.estrellas, 0) / reviews.length).toFixed(1) : null;
  async function publish(e) {
    e.preventDefault();
    await action(async () => {
      const review = await api(`/commerce/products/${product.idProducto}/reviews`, {
        method: 'POST', body: { estrellas: Number(stars), comentario: text },
      });
      setReviews(previous => [review, ...previous]); setText('');
    }, 'Tu opinión se publicó correctamente. ¡Gracias por compartirla!');
  }
  return <section id={`product-reviews-${product.idProducto}`} className="product-reviews stack" aria-label={`Opiniones de ${product.nombre}`}>
    <h2>Opiniones de {product.nombre}</h2>
    <p>{average ? `${average}/5 · ${reviews.length} opiniones` : 'Comparte tu experiencia con este producto.'}</p>
    {loading && <p role="status">Cargando opiniones…</p>}
    {loadError && <p role="alert" className="notice error">{loadError} <button className="text-button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></p>}
    {error && <p role="alert" className="notice error">{error}</p>}
    {message && <p role="status" className="notice success">{message}</p>}
    {user.role === 'CLIENTE' ? ownReview ? <p className="notice">Ya compartiste tu opinión sobre este producto.</p> : <form className="card stack" onSubmit={publish}>
      <h3>Escribe tu opinión</h3>
      <label>Valoración<select value={stars} onChange={e => setStars(e.target.value)}>{[5,4,3,2,1].map(n => <option key={n} value={n}>{n} de 5 estrellas</option>)}</select></label>
      <label>Comentario<textarea required minLength={3} maxLength={1500} value={text} onChange={e => setText(e.target.value)} placeholder={`¿Qué te pareció ${product.nombre}?`}/></label>
      <button className="button" disabled={busy || loading}>{busy ? 'Publicando…' : 'Publicar opinión'}</button>
    </form> : <p className="notice">Las opiniones se publican desde una cuenta de cliente.</p>}
    {reviews.map(r => <article className="card" key={r.id}><strong>{r.autor} · {r.estrellas}/5</strong><p className="commerce-text">{r.comentario}</p></article>)}
    {!loading && !reviews.length && <p className="muted">Este producto todavía no tiene opiniones.</p>}
  </section>;
}
