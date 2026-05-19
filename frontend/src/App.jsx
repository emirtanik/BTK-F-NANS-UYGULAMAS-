import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ChatProvider } from './context/ChatContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Welcome from './pages/Welcome';
import Markets from './pages/Markets';
import Portfolio from './pages/Portfolio';
import ChatWidget from './components/ChatWidget';

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

const AppShell = ({ children }) => (
  <>
    {children}
    <ChatWidget />
  </>
);

const App = () => (
  <AuthProvider>
    <ChatProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/welcome" element={
            <ProtectedRoute><AppShell><Welcome /></AppShell></ProtectedRoute>
          } />
          <Route path="/markets" element={
            <ProtectedRoute><AppShell><Markets /></AppShell></ProtectedRoute>
          } />
          <Route path="/portfolio" element={
            <ProtectedRoute><AppShell><Portfolio /></AppShell></ProtectedRoute>
          } />
          <Route path="/" element={<Navigate to="/welcome" replace />} />
          <Route path="*" element={<Navigate to="/welcome" replace />} />
        </Routes>
      </Router>
    </ChatProvider>
  </AuthProvider>
);

export default App;
