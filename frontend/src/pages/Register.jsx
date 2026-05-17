import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { UserPlus } from 'lucide-react';

const Register = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError('');
      await register({ fullName: `${firstName} ${lastName}`.trim(), email, password });
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Kayıt başarısız. Lütfen bilgilerinizi kontrol edin.');
    }
  };

  return (
    <div className="flex-center" style={{ flex: 1, padding: '24px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '400px' }}>
        <h2 style={{ textAlign: 'center', marginBottom: '24px' }}>Kayıt Ol</h2>
        {error && <div className="error-text" style={{ marginBottom: '16px', textAlign: 'center' }}>{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>Ad</label>
            <input type="text" className="input-field" value={firstName} onChange={e => setFirstName(e.target.value)} required />
          </div>
          <div className="input-group">
            <label>Soyad</label>
            <input type="text" className="input-field" value={lastName} onChange={e => setLastName(e.target.value)} required />
          </div>
          <div className="input-group">
            <label>Email</label>
            <input type="email" className="input-field" value={email} onChange={e => setEmail(e.target.value)} required />
          </div>
          <div className="input-group">
            <label>Şifre</label>
            <input type="password" className="input-field" value={password} onChange={e => setPassword(e.target.value)} required />
          </div>
          <button type="submit" className="btn" style={{ marginTop: '16px' }}>
            <UserPlus size={20} />
            Kayıt Ol
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.9rem' }}>
          Zaten hesabınız var mı? <Link to="/login" style={{ color: 'var(--primary)' }}>Giriş Yap</Link>
        </p>
      </div>
    </div>
  );
};

export default Register;
