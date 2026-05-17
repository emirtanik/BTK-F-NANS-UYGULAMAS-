import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut, LayoutDashboard, PieChart, MessageCircle, Lightbulb } from 'lucide-react';

const Navbar = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="flex-center" style={{ gap: '12px' }}>
        <div style={{ background: 'var(--primary)', padding: '8px', borderRadius: '8px' }}>
          <PieChart size={24} color="white" />
        </div>
        <h2 style={{ margin: 0, color: 'white' }}>FinPortfolio</h2>
      </div>
      <div className="nav-links">
        <Link 
          to="/dashboard" 
          className={`nav-link ${location.pathname === '/dashboard' ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <LayoutDashboard size={18} />
          Dashboard
        </Link>
        <Link 
          to="/analysis" 
          className={`nav-link ${location.pathname === '/analysis' ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <PieChart size={18} />
          Analiz
        </Link>
        <Link
          to="/chat"
          className={`nav-link ${location.pathname === '/chat' ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <MessageCircle size={18} />
          Sohbet
        </Link>
        <Link
          to="/advisor"
          className={`nav-link ${location.pathname === '/advisor' ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <Lightbulb size={18} />
          Danışman
        </Link>
        <button className="btn btn-secondary" onClick={handleLogout} style={{ padding: '8px 16px', width: 'auto' }}>
          <LogOut size={18} />
          Logout
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
