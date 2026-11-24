import { useCallback, useRef, useState } from 'react';
export default function useSessionCart(userId) {
 const key=`radiocafe.cart.${userId}`;
 const [cart,setState]=useState(()=>{try {const list=JSON.parse(sessionStorage.getItem(key)||'[]');return Array.isArray(list)?list.filter(p=>p&&typeof p.key==='string'&&Number.isInteger(p.idProducto)&&Number.isInteger(p.cantidad)&&p.cantidad>0&&p.cantidad<=99&&Number.isFinite(p.precio)&&p.precio>0).slice(0,50):[];}catch{return [];}});
 const current=useRef(cart);
 const setCart=useCallback(update=>{const next=typeof update==='function'?update(current.current):update;current.current=next;try{sessionStorage.setItem(key,JSON.stringify(next));}catch{}setState(next);},[key]);
 return [cart,setCart];
}
