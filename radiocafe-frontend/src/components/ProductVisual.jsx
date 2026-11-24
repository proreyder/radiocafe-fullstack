import { useId, useState } from 'react';

/** Local product artwork, used when the catalog has no uploaded image. */
export default function ProductVisual({ product }) {
  const uid = useId().replaceAll(':', '');
  const [failedImage, setFailedImage] = useState(null);
  const cold = /cold|frapp|helad/i.test(product.nombre);
  const pastry = product.categoria === 'PASTELERIA';
  const beans = /grano/i.test(product.nombre);
  if (product.imagenUrl && failedImage !== product.imagenUrl) return <img className="order-product-photo" src={product.imagenUrl} alt={product.nombre} referrerPolicy="no-referrer" onError={() => setFailedImage(product.imagenUrl)} />;
  return <svg className="order-product-art" viewBox="0 0 500 360" role="img" aria-label={`Ilustración de ${product.nombre}`}>
    <defs>
      <linearGradient id={`${uid}-coffee`} x1="0" x2="1"><stop stopColor="#b3845b"/><stop offset=".35" stopColor="#e1bb91"/><stop offset=".8" stopColor="#b68254"/><stop offset="1" stopColor="#93633d"/></linearGradient>
      <linearGradient id={`${uid}-cup`} x1="0" x2="1"><stop stopColor="#e4dfd4"/><stop offset=".4" stopColor="#fffdf8"/><stop offset="1" stopColor="#ddd7ca"/></linearGradient>
      <linearGradient id={`${uid}-bread`} x1="0" y1="0" x2=".5" y2="1"><stop stopColor="#f7ca76"/><stop offset=".5" stopColor="#d99637"/><stop offset="1" stopColor="#8e4e1f"/></linearGradient>
      <radialGradient id={`${uid}-shadow`}><stop stopColor="#312819" stopOpacity=".22"/><stop offset="1" stopColor="#312819" stopOpacity="0"/></radialGradient>
    </defs>
    <ellipse cx="254" cy="303" rx="113" ry="22" fill={`url(#${uid}-shadow)`}/>
    {beans ? <g><path d="M184 94h133l18 200q-83 28-168 0z" fill="#c7a374"/><path d="M184 94h133l-13 29H194z" fill="#937047"/><path d="M178 161h149l6 99H171z" fill="#b45221"/><text x="252" y="202" fill="#fffaf0" textAnchor="middle" fontSize="16" fontWeight="700" letterSpacing="3">RADIOCAFÉ</text><text x="252" y="229" fill="#e5d7bb" textAnchor="middle" fontSize="11" letterSpacing="2">ORIGEN PERUANO</text></g>
      : pastry ? <g><ellipse cx="250" cy="286" rx="129" ry="17" fill="#e3ddd2"/><ellipse cx="250" cy="281" rx="125" ry="17" fill="#fffdfa"/><path d="M132 259q15-49 53-67 16-43 61-36 48-11 72 29 42 17 53 72l-38 17-44-12-43 10-41-11-42 14z" fill={`url(#${uid}-bread)`}/><path d="m185 192 19 69m42-105 0 115m72-86-29 77m-157-3 31 16m208-18-38 17" fill="none" stroke="#a76027" strokeWidth="5" opacity=".55"/><path d="m209 176 16 79m41-82-5 81" stroke="#ffe3a0" strokeWidth="6" opacity=".65"/></g>
      : cold ? <g><path d="m286 45-11 96" stroke="#c69d77" strokeWidth="9"/><path d="M185 115h132l-18 175q-44 18-96 0z" fill={`url(#${uid}-coffee)`}/><path d="M187 136h128l-5 29H191z" fill="#684331" opacity=".8"/><path d="m205 169 23 3-6 26-25-3zm57-7 23 5-8 25-23-8zm-20 48 26-1 1 27-27 1z" fill="#f1d8b2" opacity=".55"/><ellipse cx="251" cy="115" rx="66" ry="13" fill="#61412b"/><ellipse cx="251" cy="112" rx="69" ry="11" fill="none" stroke="#f0e7d9" strokeWidth="4"/><path d="m194 141 14 137" stroke="#fff" opacity=".36" strokeWidth="5"/><circle cx="251" cy="229" r="30" fill="#be581f"/><text x="251" y="234" textAnchor="middle" fontSize="11" fontWeight="800" fill="#fff">RC</text></g>
      : <g><ellipse cx="250" cy="290" rx="101" ry="14" fill="#e1dcd2"/><ellipse cx="250" cy="285" rx="99" ry="14" fill="#fffdfa"/><path d="M307 160q61-9 55 37-4 33-52 29" fill="none" stroke="#ddd7cb" strokeWidth="17"/><path d="M166 151h158l-16 104q-10 28-64 28-56-1-64-28z" fill={`url(#${uid}-cup)`}/><ellipse cx="245" cy="151" rx="79" ry="23" fill="#fffdf6"/><ellipse cx="245" cy="151" rx="67" ry="17" fill="#ac6c36"/><path d="M216 153q7-22 28-9 22-13 29 1 4 12-28 19-20-6-29-11" fill="#f9eed5"/><path d="m245 160-1-20" stroke="#ae743e" strokeWidth="2"/><text x="245" y="218" textAnchor="middle" fill="#ae4c1a" fontSize="15" fontWeight="800" letterSpacing="2">RADIOCAFÉ</text><path d="M227 97q-11-17 2-31m25 31q-11-17 2-31" stroke="#c9bfb0" strokeWidth="3" opacity=".6" fill="none"/></g>}
  </svg>;
}
