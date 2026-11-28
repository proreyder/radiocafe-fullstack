import { useLayoutEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home';
import PublicContest from './pages/PublicContest';
import Profile from './pages/Profile';
import AdminDashboard from './pages/AdminDashboard';
import ClientCatalog from './pages/ClientCatalog';
import GuestTracking from './pages/GuestTracking';
import { useAuth } from './context/useAuth';

function CatalogRoute() {
  const { user, loading } = useAuth();
  if (loading) return <p className="empty" role="status">Cargando catálogo…</p>;
  if (!user) return <ClientCatalog key="guest" />;
  return <ProtectedRoute allowedRoles={['CLIENTE', 'ADMINISTRADOR']}><ClientCatalog key={user.idUsuario} /></ProtectedRoute>;
}

function ScrollToTop() {
  const { pathname } = useLocation();
  useLayoutEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: 'instant' }); }, [pathname]);
  return null;
}

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <ScrollToTop />
        <Routes>
          {/* Ruta Pública: Landing Page e Inicio de Sesión */}
          <Route path="/" element={<Home />} />
          <Route path="/pedido/:id" element={<GuestTracking/>}/><Route path="/mis-pedidos" element={<GuestTracking/>}/><Route path="/kappu" element={<PublicContest/>}/>
          <Route path="/perfil" element={<ProtectedRoute><Profile/></ProtectedRoute>}/>

          {/* La compra como invitado es pública; la cuenta y los paneles conservan su protección. */}
          <Route 
            path="/catalogo" 
            element={
              <CatalogRoute />
            } 
          />

          {/* Ruta Protegida EXCLUSIVA: Bloquea clientes no autorizados */}
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute allowedRoles={['ADMINISTRADOR', 'BARISTA', 'REPARTIDOR']}>
                <AdminDashboard />
              </ProtectedRoute>
            } 
          />

          {/* Redirección por defecto si escriben una ruta inexistente */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}
