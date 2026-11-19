import { useLayoutEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home';
import AdminDashboard from './pages/AdminDashboard';
import ClientCatalog from './pages/ClientCatalog';

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

          {/* Ruta Protegida: Acceso para Clientes y Administradores */}
          <Route 
            path="/catalogo" 
            element={
              <ProtectedRoute allowedRoles={['CLIENTE', 'ADMINISTRADOR']}>
                <ClientCatalog />
              </ProtectedRoute>
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