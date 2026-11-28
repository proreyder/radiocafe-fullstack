import { useEffect, useId, useState } from 'react';
import { Search, SlidersHorizontal, Coffee } from 'lucide-react';
import { api, money } from '../lib/api';
import ProductVisual from './ProductVisual';
import '../pages/CatalogBrowser.css';

const categories = { CAFE: 'Cafés y bebidas', PASTELERIA: 'Panadería y pastelería', COMBO: 'Combos' };
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export default function CatalogBrowser({ products, onSelect, search, setSearch, onFilteringChange }) {
  const resultsId = useId();
  const [category, setCategory] = useState('');
  const [sort, setSort] = useState('name');
  const [inStock, setInStock] = useState(false);
  const [maxPrice, setMaxPrice] = useState('');
  const [showAll, setShowAll] = useState(false);
  const [recommendations, setRecommendations] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    api('/commerce/recommendations', { signal: controller.signal }).then(list => {
      if (!controller.signal.aborted) setRecommendations(list);
    }).catch(() => {}); // The daily selection also works with the current catalog alone.
    return () => controller.abort();
  }, []);

  const query = normalize(search.trim());
  const filtering = Boolean(query || category || maxPrice !== '' || inStock || sort !== 'name');
  const showingResults = filtering || showAll;
  useEffect(() => { onFilteringChange(showingResults); }, [showingResults, onFilteringChange]);
  const results = products.filter(p =>
    (!category || p.categoria === category) && (!inStock || p.stock > 0) &&
    (maxPrice === '' || Number(p.precio) <= Number(maxPrice)) &&
    normalize(`${p.nombre} ${p.descripcion || ''}`).includes(query)
  ).sort((a,b) => sort === 'price-asc' ? a.precio - b.precio : sort === 'price-desc' ? b.precio - a.precio : a.nombre.localeCompare(b.nombre, 'es'));

  const available = products.filter(p => p.stock > 0);
  const recommendedIds = recommendations.map(r => r.producto.idProducto);
  const pool = [...available].sort((a,b) => {
    const rank = p => { const index = recommendedIds.indexOf(p.idProducto); return index < 0 ? recommendedIds.length : index; };
    return rank(a) - rank(b) || a.idProducto - b.idProducto;
  });
  const now = new Date();
  const day = Math.floor(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86400000);
  const offset = pool.length ? day % pool.length : 0;
  const suggestions = [...pool.slice(offset), ...pool.slice(0, offset)].slice(0, 4);
  const visible = showingResults ? results : suggestions;

  function clear() { setSearch(''); setCategory(''); setSort('name'); setInStock(false); setMaxPrice(''); setShowAll(false); }

  return <div className="catalog-browser">
    <div className="catalog-search-panel" aria-label="Categorías y filtros del menú">
      <form className="catalog-search-form" onSubmit={e => e.preventDefault()}>
        <div className="catalog-category-list" aria-label="Categorías">{[['','Todo'],...Object.entries(categories)].map(([value,label])=><button type="button" key={value} className={category===value?'is-selected':''} aria-pressed={category===value} aria-controls={resultsId} onClick={()=>{setCategory(value);setShowAll(false);}}>{label}</button>)}</div>
        <details className="catalog-extra-filters"><summary><SlidersHorizontal size={17}/> Filtros</summary><div className="catalog-filter-fields">
        <label>Ordenar<select value={sort} onChange={e => { setSort(e.target.value); setShowAll(false); }}><option value="name">Nombre</option><option value="price-asc">Menor precio</option><option value="price-desc">Mayor precio</option></select></label>
        <label>Precio máximo (S/)<input type="number" min="0" step="0.01" value={maxPrice} onChange={e => { setMaxPrice(e.target.value); setShowAll(false); }}/></label>
        <label className="catalog-stock-filter"><input type="checkbox" checked={inStock} onChange={e => { setInStock(e.target.checked); setShowAll(false); }}/>Solo disponibles</label>
        <button type="button" className="button secondary" onClick={clear}>Limpiar filtros</button>
        </div></details>
      </form>
    </div>

    <section id={resultsId} className="catalog-results-panel" aria-labelledby={`${resultsId}-heading`}>
      <header className="catalog-results-heading">
        <div><p className="eyebrow">{showingResults ? 'LA CARTA A TU GUSTO' : 'UNA PAUSA PARA HOY'}</p><h2 id={`${resultsId}-heading`}>{filtering ? 'Resultados de tu búsqueda' : showAll ? 'Todo el menú' : 'Sugerencias del día'}</h2></div>
        {!showingResults && <span className="catalog-day"><Coffee size={17}/>Hecho para disfrutar</span>}
      </header>
      <p className="catalog-result-count" role="status" aria-live="polite">{showingResults ? `${results.length} ${results.length === 1 ? 'producto encontrado' : 'productos encontrados'}${search.trim() ? ` para «${search.trim()}»` : ''}` : 'Una selección de productos disponibles para acompañar tu día.'}</p>
      <div className="catalog-results-grid">{visible.map(p => <button className="catalog-result-card" key={p.idProducto} onClick={() => onSelect(p)}>
        <span className="catalog-result-image"><ProductVisual product={p}/></span>
        <span className="catalog-result-category">{categories[p.categoria]}</span>
        <strong>{p.nombre}</strong><span className="catalog-result-description">{p.descripcion}</span>
        <span className="catalog-result-price">Desde {money(p.precio)}</span>{!showingResults && <small className="catalog-suggestion-reason">{recommendations.find(r => r.producto.idProducto === p.idProducto)?.motivo || 'Selección de hoy'}</small>}<span className="catalog-card-action">Ver producto <span aria-hidden="true">↗</span></span>
        {!p.stock && <span className="catalog-result-stock">Agotado</span>}
      </button>)}</div>
      {!visible.length && <div className="catalog-search-empty"><Search size={30}/><h3>{showingResults ? 'No encontramos coincidencias' : 'Pronto tendremos más sugerencias'}</h3><p>{showingResults ? 'Prueba con otro nombre o cambia los filtros.' : 'Puedes explorar el menú completo para ver todos los productos.'}</p>{showingResults && <button className="button secondary" onClick={clear}>Volver a las sugerencias</button>}</div>}
      {!showingResults && <button className="button secondary catalog-see-all" onClick={() => setShowAll(true)}>Ver todo el menú</button>}
    </section>
  </div>;
}
