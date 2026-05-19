import { createContext, useState, useContext, useEffect } from 'react';
import api from '../api/axiosConfig';
import { extractErrorMessage } from '../api/apiError';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      if (!token) {
        localStorage.removeItem('token');
        setUser(null);
        setLoading(false);
        return;
      }
      localStorage.setItem('token', token);
      try {
        await api.get('/portfolio');
        const stored = localStorage.getItem('user');
        if (stored) setUser(JSON.parse(stored));
        else setUser({ token });
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    init();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const login = async (credentials) => {
    try {
      const { data } = await api.post('/auth/login', credentials);
      const newToken = data.accessToken || data.token;
      const userInfo = { userId: data.userId, email: data.email, fullName: data.fullName };
      localStorage.setItem('user', JSON.stringify(userInfo));
      setToken(newToken);
      setUser(userInfo);
      return data;
    } catch (err) {
      throw new Error(extractErrorMessage(err, 'Giriş başarısız.'));
    }
  };

  const register = async (userData) => {
    try {
      const { data } = await api.post('/auth/register', userData);
      const newToken = data.accessToken || data.token;
      const userInfo = { userId: data.userId, email: data.email, fullName: data.fullName };
      localStorage.setItem('user', JSON.stringify(userInfo));
      setToken(newToken);
      setUser(userInfo);
      return data;
    } catch (err) {
      throw new Error(extractErrorMessage(err, 'Kayıt başarısız.'));
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
