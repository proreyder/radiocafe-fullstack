import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import Layout from '../components/Layout';
import FullCatalog from '../components/FullCatalog';
import './Ordering.css';
import './CatalogBrowser.css';

export default function PublicCatalog() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const data = await api('/products');
        if (active) { setProducts(data); setError(''); }
      } catch (err) { if (active) setError(err.message); }
      finally { if (active) setLoading(false); }
    }
    load();
    const timer = window.setInterval(() => { if (!document.hidden) load(); }, 30000);
    window.addEventListener('focus', load);
    return () => { active = false; window.clearInterval(timer); window.removeEventListener('focus', load); };
  }, []);
  return <Layout variant="ordering"><main className="ordering-page public-catalog-page">
    {error && <p className="notice error" role="alert">{error}</p>}
    {loading ? <p className="empty" role="status">Cargando catálogo…</p> : <FullCatalog products={products} readOnly />}
  </main></Layout>;
}
