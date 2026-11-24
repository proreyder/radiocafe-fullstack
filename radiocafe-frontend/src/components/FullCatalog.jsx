import ProductVisual from './ProductVisual';
import { money } from '../lib/api';
import '../pages/FullCatalog.css';
const categories = { CAFE: 'Cafés y bebidas', PASTELERIA: 'Panadería y pastelería', COMBO: 'Combos' };
export default function FullCatalog({ products, onSelect, readOnly = false }) {
  const Card = readOnly ? 'article' : 'button';
  return <section className="full-catalog">
    <header><p className="eyebrow">TODAS NUESTRAS VARIEDADES</p><h1>El catálogo de RadioCafé</h1><p>{readOnly ? 'Conoce nuestros productos y precios. Inicia sesión para hacer un pedido.' : 'Descubre los productos que vamos sumando a la carta, con sus fotos, precios y disponibilidad.'}</p></header>
    <nav className="full-catalog-index" aria-label="Categorías del catálogo">
      {Object.entries(categories).filter(([id]) => products.some(p => p.categoria === id)).map(([id, label]) => <a key={id} href={`#variedades-${id.toLowerCase()}`}>{label}</a>)}
    </nav>
    {Object.entries(categories).map(([id, label]) => {
      const list = products.filter(p => p.categoria === id);
      return list.length > 0 && <section className="full-catalog-category" id={`variedades-${id.toLowerCase()}`} key={id}>
        <div className="section-heading"><h2>{label}</h2><span>{list.length} {list.length === 1 ? 'variedad' : 'variedades'}</span></div>
        <div className="catalog-results-grid">{list.map(p => <Card className="catalog-result-card" key={p.idProducto} {...(readOnly ? {} : { type: 'button', onClick: () => onSelect(p) })}>
          <span className="catalog-result-image"><ProductVisual product={p} /></span><strong>{p.nombre}</strong>
          {!readOnly && <span className="catalog-result-description">{p.descripcion}</span>}
          <span className="catalog-result-price">{!readOnly && 'Desde '}{money(p.precio)}</span>
          {!readOnly && <span className={`catalog-availability ${p.stock ? 'available' : 'sold-out'}`}>{p.stock ? 'Disponible' : 'Agotado'}</span>}
        </Card>)}</div>
      </section>;
    })}
    {!products.length && <p className="empty">Pronto encontrarás nuevas variedades en nuestro catálogo.</p>}
  </section>;
}
