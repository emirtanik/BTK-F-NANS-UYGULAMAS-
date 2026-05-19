import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      const onAuth = window.location.pathname === '/login' || window.location.pathname === '/register';
      if (!onAuth) window.location.href = '/login?session=expired';
    }
    return Promise.reject(error);
  }
);

export default api;
