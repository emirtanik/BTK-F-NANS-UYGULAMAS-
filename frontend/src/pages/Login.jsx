import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogIn, UserPlus } from 'lucide-react';

const Login = () => {
  const [isLogin, setIsLogin] = useState(true);
  
  // Form states
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError('');
      if (isLogin) {
        await login({ email, password });
      } else {
        await register({ fullName, email, password });
      }
      navigate('/dashboard');
    } catch (err) {
      // Backend'den gelen spesifik hata mesajını ekrana yansıt
      setError(err.message || (isLogin ? 'Giriş başarısız.' : 'Kayıt başarısız.'));
    }
  };

  return (
    <div className="flex-center" style={{ flex: 1, padding: '24px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '400px', transition: 'all 0.3s ease' }}>
        
        {/* Toggle Buttons */}
        <div style={{ display: 'flex', marginBottom: '24px', background: 'rgba(15, 23, 42, 0.6)', borderRadius: '8px', padding: '4px' }}>
          <button 
            type="button"
            onClick={() => { setIsLogin(true); setError(''); }}
            style={{ 
              flex: 1, padding: '8px', borderRadius: '6px', border: 'none', 
              background: isLogin ? 'var(--primary)' : 'transparent',
              color: isLogin ? 'white' : 'var(--text-secondary)',
              cursor: 'pointer', fontWeight: '600', transition: 'all 0.2s'
            }}
          >
            Giriş Yap
          </button>
          <button 
            type="button"
            onClick={() => { setIsLogin(false); setError(''); }}
            style={{ 
              flex: 1, padding: '8px', borderRadius: '6px', border: 'none', 
              background: !isLogin ? 'var(--primary)' : 'transparent',
              color: !isLogin ? 'white' : 'var(--text-secondary)',
              cursor: 'pointer', fontWeight: '600', transition: 'all 0.2s'
            }}
          >
            Kayıt Ol
          </button>
        </div>

        <h2 style={{ textAlign: 'center', marginBottom: '24px' }}>
          {isLogin ? 'Hoş Geldiniz' : 'Hesap Oluşturun'}
        </h2>
        
        {error && <div className="error-text" style={{ marginBottom: '16px', textAlign: 'center', padding: '10px', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px' }}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <div className="input-group">
              <label>Ad Soyad</label>
              <input 
                type="text" 
                className="input-field" 
                value={fullName} 
                onChange={e => setFullName(e.target.value)} 
                required={!isLogin} 
              />
            </div>
          )}
          <div className="input-group">
            <label>Email</label>
            <input 
              type="email" 
              className="input-field" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
              required 
            />
          </div>
          <div className="input-group">
            <label>Şifre</label>
            <input 
              type="password" 
              className="input-field" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
              required 
            />
            {!isLogin && (
              <small style={{ color: 'var(--text-secondary)', marginTop: '4px', fontSize: '0.8rem' }}>
                En az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 özel karakter içermelidir.
              </small>
            )}
          </div>
          <button type="submit" className="btn" style={{ marginTop: '16px' }}>
            {isLogin ? <LogIn size={20} /> : <UserPlus size={20} />}
            {isLogin ? 'Giriş Yap' : 'Kayıt Ol'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
