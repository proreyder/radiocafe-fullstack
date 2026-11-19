import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { staffRoles } from '../lib/api';
export default function ProtectedRoute({ children, allowedRoles }) {
  const { user, loading } = useAuth();
  if (loading) return <p className="loading" role="status">Comprobando sesión…</p>;
  if (!user) return <Navigate to="/" replace />;
  if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to={staffRoles.includes(user.role) ? '/admin' : '/catalogo'} replace />;
  return children;
}
