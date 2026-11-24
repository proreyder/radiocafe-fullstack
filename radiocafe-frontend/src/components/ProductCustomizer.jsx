import { ChevronDown, Check, CupSoda } from 'lucide-react';
import { money } from '../lib/api';
import ProductVisual from './ProductVisual';
function Choices({ title, items, value, onChange, required, multi = false, max = 5 }) {
  return <details className="customize-group" open><summary><span>{title}{required && <b> *</b>}</span><span className="group-hint">{required ? 'Requerido' : `Máx. ${max}`} <ChevronDown size={18}/></span></summary>
    <div className="customize-options" role="group" aria-label={title}>{items.map(option => {
      const selected = multi ? value.includes(option.id) : value === option.id;
      const blocked = multi && value.length >= max && !selected;
      return <button type="button" key={option.id} className={`customize-option ${selected ? 'selected' : ''}`} role={multi ? 'checkbox' : 'radio'} aria-checked={selected} disabled={blocked}
        onClick={() => onChange(multi ? selected ? value.filter(id => id !== option.id) : [...value, option.id] : option.id)}>
        <span><strong>{option.nombre}</strong><small>+ {money(option.recargo)}</small></span><span className={`choice-indicator ${multi ? 'square' : ''}`}>{selected && <Check size={13}/>}</span>
      </button>;
    })}</div></details>;
}
export default function ProductCustomizer({ product, options, choice, onChange }) {
  const field = key => value => onChange({ ...choice, [key]: value });
  return <div className="product-detail-layout"><article className="product-story"><div className="product-stage"><ProductVisual product={product}/></div>
    <h1>{product.nombre}</h1><p>{product.descripcion}</p><strong className="starting-price">Desde {money(product.precio)}</strong><span className={`product-stock-status ${!product.stock ? 'sold-out' : ''}`}>{product.stock ? `${product.stock} unidades disponibles` : 'Producto agotado'}</span><a className="product-review-link" href={`#product-reviews-${product.idProducto}`}>Ver opiniones del producto</a><span className="product-origin">Preparado para tu momento RadioCafé.</span></article>
    <section className="product-configuration"><h2>{product.personalizable ? 'Personaliza tu producto' : 'Tu próxima pausa empieza aquí'}</h2>
      {product.personalizable ? <><p className="required-note">* Opciones obligatorias</p><div className="size-selector"><h3>Tamaño <b>*</b></h3><div><div className="size-options" role="group" aria-label="Tamaño">{options.tamanos.map((size,index) => <button type="button" key={size.id} aria-label={size.nombre} aria-pressed={choice.tamano === size.id} className={choice.tamano === size.id ? 'selected' : ''} onClick={() => field('tamano')(size.id)}><CupSoda size={29+index*6} strokeWidth={1.2}/></button>)}</div><p>{options.tamanos.find(s => s.id === choice.tamano)?.nombre} <span>+ {money(options.tamanos.find(s => s.id === choice.tamano)?.recargo)}</span></p></div></div>
        <Choices title="Leche" required items={options.leches} value={choice.leche} onChange={field('leche')}/>
        <Choices title="Crema batida" required items={options.cremas} value={choice.crema} onChange={field('crema')}/>
        <Choices title="Extras" items={options.extras} value={choice.extras} onChange={field('extras')} multi max={options.maxExtras}/>
      </> : <div className="simple-product-info"><CupSoda size={42} strokeWidth={1.2}/><h3>Así de simple, así de rico.</h3><p>Este producto se sirve en su presentación original. Elige la cantidad y agrégalo a tu pedido.</p><div><span>Preparado con cuidado</span><span>Delivery o recojo en local</span></div></div>}
    </section></div>;
}
