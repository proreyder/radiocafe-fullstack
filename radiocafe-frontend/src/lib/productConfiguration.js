export function configurationPrice(product, options, choice) {
  if (!product?.personalizable || !options) return Number(product?.precio || 0);
  const groups = [[options.tamanos, choice.tamano], [options.leches, choice.leche], [options.cremas, choice.crema]];
  return Number(product.precio) + groups.reduce((sum, [items, id]) => sum + Number(items.find(i => i.id === id)?.recargo || 0), 0)
    + choice.extras.reduce((sum, id) => sum + Number(options.extras.find(i => i.id === id)?.recargo || 0), 0);
}
export function configurationDescription(product, options, choice) {
  if (!product.personalizable) return '';
  return [...options.tamanos, ...options.leches, ...options.cremas, ...options.extras]
    .filter(o => [choice.tamano, choice.leche, choice.crema, ...choice.extras].includes(o.id)).map(o => o.nombre).join(' · ');
}
