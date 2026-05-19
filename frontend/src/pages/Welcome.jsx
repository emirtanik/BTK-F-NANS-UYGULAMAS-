import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import { formatTry, formatPercent, getAssetName } from '../utils/format';
import './Welcome.css';

const Welcome = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await api.get('/chat/welcome');
        setData(data);
      } catch (err) {
        console.error(err);
        setError('Bağlantı kurulamadı.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) {
    return (
      <div className="welcome-page">
        <div className="welcome-loading">
          <div className="spinner" />
          <p>Portföyün analiz ediliyor...</p>
        </div>
      </div>
    );
  }

  const changes = data?.changes || [];
  const totalProfitLoss = data?.totalProfitLossTry ?? 0;
  const totalPercent = data?.totalProfitLossPercent ?? 0;
  const isPositive = Number(totalProfitLoss) >= 0;

  return (
    <div className="welcome-page">
      <div className="welcome-orb welcome-orb-1" />
      <div className="welcome-orb welcome-orb-2" />

      <div className="welcome-content slide-up">
        <div className="welcome-header">
          <div className="welcome-avatar">
            <div className="welcome-avatar-inner">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L13.5 8.5L20 10L13.5 11.5L12 18L10.5 11.5L4 10L10.5 8.5L12 2Z"
                  fill="url(#gemGrad)" />
                <defs>
                  <linearGradient id="gemGrad" x1="0" y1="0" x2="24" y2="24">
                    <stop offset="0%" stopColor="#4285f4" />
                    <stop offset="50%" stopColor="#9b72cb" />
                    <stop offset="100%" stopColor="#d96570" />
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>
          <div>
            <h2 className="welcome-greeting">
              {user?.fullName ? `Merhaba ${user.fullName.split(' ')[0]}` : 'Merhaba'}
            </h2>
            <p className="welcome-subtitle">FinPortfolio AI Asistanı</p>
          </div>
        </div>

        {error && <div className="welcome-error">{error}</div>}

        <div className="welcome-message">
          <p>{data?.botMessage || 'Hoş geldin!'}</p>
        </div>

        {changes.length > 0 && (
          <>
            <div className="welcome-summary">
              <div className="welcome-summary-row">
                <span className="welcome-summary-label">Son ziyaretten beri</span>
                <span className={`welcome-summary-value ${isPositive ? 'green-text' : 'red-text'}`}>
                  {isPositive ? '+' : ''}{formatTry(totalProfitLoss)} ₺
                </span>
              </div>
              <div className="welcome-summary-row">
                <span className="welcome-summary-label">Yüzdesel değişim</span>
                <span className={`welcome-summary-value ${isPositive ? 'green-text' : 'red-text'}`}>
                  {formatPercent(totalPercent)}
                </span>
              </div>
            </div>

            <div className="welcome-changes">
              <h3>Varlık Bazında Değişim</h3>
              <div className="welcome-changes-list">
                {changes.map((c, i) => {
                  const positive = Number(c.profitLossTry) >= 0;
                  return (
                    <div key={i} className="welcome-change-row">
                      <div className="welcome-change-left">
                        <span className="welcome-change-symbol">{c.symbol}</span>
                        <span className="welcome-change-name">{getAssetName(c.symbol)}</span>
                      </div>
                      <div className="welcome-change-right">
                        <span className={`welcome-change-pnl ${positive ? 'green-text' : 'red-text'}`}>
                          {positive ? '+' : ''}{formatTry(c.profitLossTry)} ₺
                        </span>
                        <span className={positive ? 'green-badge' : 'red-badge'}>
                          {formatPercent(c.changePercent)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}

        <button className="welcome-continue" onClick={() => navigate('/markets')}>
          Devam Et
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M5 12H19M19 12L13 6M19 12L13 18" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>
    </div>
  );
};

export default Welcome;
