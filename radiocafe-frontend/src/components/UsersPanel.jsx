import { useEffect, useState } from 'react';
import { api } from '../lib/api';
const roles = { CLIENTE: 'Cliente', BARISTA: 'Barista', REPARTIDOR: 'Repartidor', ADMINISTRADOR: 'Administrador' };
const blank = { nombre: '', correo: '', password: '', rol: 'BARISTA', telefono: '', activo: true };
export default function UsersPanel({ user, action, busy }) {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(null);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [loadError, setLoadError] = useState('');
  const ownAccount = editing === user.idUsuario;
  async function refresh() { setUsers(await api('/users')); setLoadError(''); }
  useEffect(() => {
    const controller = new AbortController();
    api('/users', { signal: controller.signal }).then(data => { if (!controller.signal.aborted) setUsers(data); })
      .catch(e => { if (!controller.signal.aborted) setLoadError(e.message); });
    return () => controller.abort();
  }, []);
  function edit(account) { setEditing(account.idUsuario); setForm({ nombre: account.nombre, correo: account.correo, password: '', rol: account.role, telefono: account.telefono || '', activo: account.activo }); }
  function cancel() { setEditing(null); setForm(blank); }
  async function save(e) {
    e.preventDefault();
    const body = editing ? { nombre: form.nombre, rol: form.rol, activo: form.activo, telefono: form.telefono } : { nombre: form.nombre, correo: form.correo, password: form.password, rol: form.rol, telefono: form.telefono };
    await action(async () => { await api(`/users${editing ? `/${editing}` : ''}`, { method: editing ? 'PATCH' : 'POST', body }); cancel(); await refresh(); }, editing ? 'Cuenta actualizada. Los cambios de rol o estado revocan las sesiones anteriores.' : 'Cuenta creada. El usuario ya puede iniciar sesión con sus credenciales.');
  }
  async function toggle(account) {
    if (!account.activo || window.confirm(`¿Desactivar la cuenta de ${account.nombre}? Sus sesiones quedarán sin acceso.`)) {
      await action(async () => { await api(`/users/${account.idUsuario}`, { method: 'PATCH', body: { nombre: account.nombre, rol: account.role, activo: !account.activo, telefono: account.telefono } }); await refresh(); }, account.activo ? 'Cuenta desactivada.' : 'Cuenta activada.');
    }
  }
  const field = key => ({ value: form[key], onChange: e => setForm({ ...form, [key]: e.target.value }) });
  const visible = users.filter(u => `${u.nombre} ${u.correo}`.toLowerCase().includes(search.toLowerCase()) && (!roleFilter || u.role === roleFilter));
  return <section className="stack operations-panel"><div className="section-heading"><div><h2>Gestión de usuarios</h2><p>Crea cuentas de trabajo, asigna roles y controla su acceso.</p></div><button className="button secondary" disabled={busy} onClick={() => action(refresh)}>Actualizar usuarios</button></div>
    {loadError && <p className="notice error" role="alert">{loadError}</p>}
    <form className="card form-grid" onSubmit={save}><h3 className="full-width">{editing ? `Editar cuenta: ${form.correo}` : 'Crear una cuenta'}</h3><label>Nombre<input required maxLength={100} {...field('nombre')} autoComplete="off"/></label>
      {!editing && <label>Correo<input required type="email" maxLength={150} {...field('correo')} autoComplete="off"/></label>}
      {!editing && <label>Contraseña inicial<input required type="password" minLength={8} maxLength={72} {...field('password')} autoComplete="new-password"/></label>}
      <label>Rol<select disabled={ownAccount} {...field('rol')}>{Object.entries(roles).map(([id,label]) => <option key={id} value={id}>{label}</option>)}</select></label><label>Teléfono (opcional)<input type="tel" maxLength={20} {...field('telefono')}/></label>
      {editing && <label className="personalizable-toggle"><input type="checkbox" checked={form.activo} disabled={ownAccount} onChange={e => setForm({ ...form, activo: e.target.checked })}/>Cuenta activa</label>}
      <div className="actions"><button className="button" disabled={busy}>{editing ? 'Guardar cuenta' : 'Crear cuenta'}</button>{editing && <button className="button secondary" type="button" disabled={busy} onClick={cancel}>Cancelar edición</button>}</div>
      <p className="operations-help full-width">El correo se mantiene fijo después de crear la cuenta. No puedes desactivar tu propia cuenta ni quitarte el rol de administrador. Las contraseñas se guardan con BCrypt y nunca se muestran aquí.</p></form>
    <div className="users-filters"><label>Buscar usuario<input type="search" placeholder="Nombre o correo" value={search} onChange={e => setSearch(e.target.value)}/></label><label>Filtrar por rol<select value={roleFilter} onChange={e => setRoleFilter(e.target.value)}><option value="">Todos los roles</option>{Object.entries(roles).map(([id,label]) => <option key={id} value={id}>{label}</option>)}</select></label></div>
    <div className="table-wrap"><table><thead><tr><th>Usuario</th><th>Rol</th><th>Acceso</th><th>Acciones</th></tr></thead><tbody>{visible.map(u => <tr key={u.idUsuario}><td><strong>{u.nombre}{u.idUsuario === user.idUsuario ? ' (Tú)' : ''}</strong><p className="operations-help">{u.correo}</p></td><td>{roles[u.role]}</td><td><span className={`operation-status ${u.activo ? '' : 'warning'}`}>{u.activo ? 'Activo' : 'Desactivado'}</span></td><td><div className="actions"><button className="button secondary" disabled={busy} onClick={() => edit(u)}>Editar cuenta</button>{u.idUsuario !== user.idUsuario && <button className="button secondary" disabled={busy} onClick={() => toggle(u)}>{u.activo ? 'Desactivar' : 'Activar'}</button>}</div></td></tr>)}</tbody></table>{!visible.length && <p className="empty">No hay usuarios para esos filtros.</p>}</div>
  </section>;
}
