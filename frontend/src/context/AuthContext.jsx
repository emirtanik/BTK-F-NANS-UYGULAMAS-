import { createContext, useState, useContext, useEffect } from 'react';
import api from '../api/axiosConfig';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token);
      setUser({ token });
    } else {
      localStorage.removeItem('token');
      setUser(null);
    }
    setLoading(false);
  }, [token]);

  const login = async (credentials) => {
    try {
      const { data } = await api.post('/auth/login', credentials);
      setToken(data.accessToken || data.token); // accessToken or token
      return data;
    } catch (err) {
      let msg = 'Giriş başarısız.';
      if (err.response?.data?.message) msg = err.response.data.message;
      throw new Error(msg);
    }
  };

  const register = async (userData) => {
    try {
      const { data } = await api.post('/auth/register', userData);
      setToken(data.accessToken || data.token);
      return data;
    } catch (err) {
      let msg = 'Kayıt başarısız.';
      if (err.response?.data?.details?.length) {
        msg = err.response.data.details.join(', ');
      } else if (err.response?.data?.message) {
        msg = err.response.data.message;
      }
      throw new Error(msg);
    }
  };

  const logout = () => {
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
