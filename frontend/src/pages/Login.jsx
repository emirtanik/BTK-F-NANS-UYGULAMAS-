import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Auth.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    if (searchParams.get('session') === 'expired') {
      setInfo('Oturum süresi doldu, lütfen tekrar giriş yapın.');
    }
  }, [searchParams]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setSubmitting(true);
    try {
      await login({ email, password });
      navigate('/welcome');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemo = () => {
    setEmail('demo@finportfolio.com');
    setPassword('Demo1234');
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-orb auth-bg-orb-1" />
      <div className="auth-bg-orb auth-bg-orb-2" />

      <div className="auth-card slide-up">
        <div className="auth-brand">
          <div className="auth-logo">F</div>
          <h1>FinPortfolio</h1>
        </div>

        <h2 className="auth-title">Hoş geldin</h2>
        <p className="auth-subtitle">Portföyünü ve piyasayı takip etmek için giriş yap.</p>

        {info && <div className="auth-info">{info}</div>}
        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-field">
            <label>E-posta</label>
            <input type="email" placeholder="ornek@finportfolio.com" value={email}
              onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="auth-field">
            <label>Şifre</label>
            <input type="password" placeholder="••••••••" value={password}
              onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </div>
          <button type="submit" className="auth-submit" disabled={submitting}>
            {submitting ? 'Giriş yapılıyor...' : 'Giriş Yap'}
          </button>
        </form>

        <button type="button" className="auth-demo" onClick={fillDemo}>
          Demo hesap bilgilerini doldur
        </button>

        <div className="auth-divider">veya</div>

        <Link to="/register" className="auth-alt-link">
          Hesabın yok mu? <span>Kayıt ol</span>
        </Link>
      </div>
    </div>
  );
};

export default Login;
