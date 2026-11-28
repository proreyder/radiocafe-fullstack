import { useEffect, useState } from 'react';
import { AuthContext } from './authContextStore';
import { api } from '../lib/api';
import { tabId } from '../lib/tabSession';
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    const initialWindow = tabId();
    api('/auth/me').then(data => { if (active && tabId() === initialWindow) setUser(data); }).catch(() => {}).finally(() => { if (active) setLoading(false); });
    const expired = () => setUser(null);
    window.addEventListener('session-expired', expired);
    return () => { active = false; window.removeEventListener('session-expired', expired); };
  }, []);
  async function login(correo, password) {
    const data = await api('/auth/login', { method: 'POST', body: { correo, password } });
    if (['CLIENTE','ADMINISTRADOR'].includes(data.role)) {
      try {
        const source=JSON.parse(sessionStorage.getItem('radiocafe.cart.guest')||'[]');
        const targetKey=`radiocafe.cart.${data.idUsuario}`;
        const existing=JSON.parse(sessionStorage.getItem(targetKey)||'[]');
        if(Array.isArray(source)&&source.length&&Array.isArray(existing)){
          const merged=[...existing];
          for(const line of source){const index=merged.findIndex(x=>x.key===line.key);if(index>=0)merged[index]={...merged[index],cantidad:Math.min(99,merged[index].cantidad+line.cantidad)};else merged.push(line);}
          sessionStorage.setItem(targetKey,JSON.stringify(merged.slice(0,50)));
          sessionStorage.removeItem('radiocafe.cart.guest');
        }
      } catch { /* La sesión sigue siendo válida si el almacenamiento no está disponible. */ }
    }
    setUser(data);
    return data;
  }
  async function register(nombre, correo, password) {
    await api('/auth/register', { method: 'POST', body: { nombre, correo, password } });
    return login(correo, password);
  }
  async function logout() { await api('/auth/logout', { method: 'POST' }); Object.keys(sessionStorage).filter(key=>key.startsWith('radiocafe.cart.')).forEach(key=>sessionStorage.removeItem(key)); setUser(null); }
  return <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser: setUser }}>{children}</AuthContext.Provider>;
}
