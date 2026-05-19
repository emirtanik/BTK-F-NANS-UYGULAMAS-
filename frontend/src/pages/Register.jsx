import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Auth.css';

const Register = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await register({ fullName, email, password });
      navigate('/welcome');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
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

        <h2 className="auth-title">Hesap oluştur</h2>
        <p className="auth-subtitle">Birkaç adımda yatırımlarını takip etmeye başla.</p>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-field">
            <label>Ad soyad</label>
            <input type="text" placeholder="Adınız Soyadınız" value={fullName}
              onChange={(e) => setFullName(e.target.value)} required minLength={2} autoComplete="name" />
          </div>
          <div className="auth-field">
            <label>E-posta</label>
            <input type="email" placeholder="ornek@finportfolio.com" value={email}
              onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="auth-field">
            <label>Şifre</label>
            <input type="password" placeholder="En az 8 karakter" value={password}
              onChange={(e) => setPassword(e.target.value)} required minLength={8} autoComplete="new-password" />
            <span className="auth-hint">En az 8 karakter, içinde harf ve rakam.</span>
          </div>
          <button type="submit" className="auth-submit" disabled={submitting}>
            {submitting ? 'Hesap oluşturuluyor...' : 'Hesap Oluştur'}
          </button>
        </form>

        <div className="auth-divider">veya</div>

        <Link to="/login" className="auth-alt-link">
          Zaten hesabın var mı? <span>Giriş yap</span>
        </Link>
      </div>
    </div>
  );
};

export default Register;
