import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const isAuthError =
        error.response?.status === 401 ||
        error.response?.data?.message?.toLowerCase().includes('oturum');
      if (isAuthError) {
        localStorage.removeItem('token');
        const onLoginPage = window.location.pathname === '/login';
        if (!onLoginPage) {
          window.location.href = '/login?session=expired';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
