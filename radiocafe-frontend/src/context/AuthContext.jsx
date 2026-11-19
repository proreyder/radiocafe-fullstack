import { useEffect, useState } from 'react';
import { AuthContext } from './authContextStore';
import { api } from '../lib/api';
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    api('/auth/me').then(data => { if (active) setUser(data); }).catch(() => {}).finally(() => { if (active) setLoading(false); });
    const expired = () => setUser(null);
    window.addEventListener('session-expired', expired);
    return () => { active = false; window.removeEventListener('session-expired', expired); };
  }, []);
  async function login(correo, password) {
    const data = await api('/auth/login', { method: 'POST', body: { correo, password } });
    setUser(data);
    return data;
  }
  async function register(nombre, correo, password) {
    await api('/auth/register', { method: 'POST', body: { nombre, correo, password } });
    return login(correo, password);
  }
  async function logout() { await api('/auth/logout', { method: 'POST' }); setUser(null); }
  return <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>;
}
