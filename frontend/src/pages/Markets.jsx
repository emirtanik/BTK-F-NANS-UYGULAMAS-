import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import { formatTry, getAssetName } from '../utils/format';
import Sparkline from '../components/Sparkline';
import './Markets.css';

const Markets = () => {
  const [prices, setPrices] = useState([]);
  const [sparklines, setSparklines] = useState({});
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const wsRef = useRef(null);
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await api.get('/market/prices');
        setPrices(data);
      } catch (err) {
        console.error('Fiyatlar yüklenemedi', err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  useEffect(() => {
    if (prices.length === 0) return;
    const loadSparklines = async () => {
      const result = {};
      const chunks = [];
      for (let i = 0; i < prices.length; i += 4) chunks.push(prices.slice(i, i + 4));

      for (const chunk of chunks) {
        await Promise.all(
          chunk.map(async (p) => {
            try {
              const { data } = await api.get(`/market/history/${p.symbol}?interval=1h&limit=24`);
              if (Array.isArray(data) && data.length > 0) {
                result[p.symbol] = data.map((c) => Number(c.close));
              }
            } catch {}
          })
        );
      }
      setSparklines(result);
    };
    loadSparklines();
  }, [prices.length]);

  useEffect(() => {
    const wsUrl = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') +
                  window.location.host + '/ws/prices';
    let ws;
    try {
      ws = new WebSocket(wsUrl);
      wsRef.current = ws;
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (Array.isArray(data) && data.length > 0) {
            setPrices((prev) => {
              if (prev.length === 0) return data;
              const updateMap = new Map(data.map((p) => [p.symbol, p]));
              return prev.map((p) => updateMap.get(p.symbol) || p);
            });
          }
        } catch {}
      };
      ws.onerror = () => {};
    } catch {}

    return () => {
      if (ws) try { ws.close(); } catch {}
    };
  }, []);

  const filtered = search
    ? prices.filter(
        (p) =>
          p.symbol.toLowerCase().includes(search.toLowerCase()) ||
          getAssetName(p.symbol).toLowerCase().includes(search.toLowerCase())
      )
    : prices;

  const today = new Date().toLocaleDateString('tr-TR', { day: 'numeric', month: 'long' });

  const sparkChange = (symbol) => {
    const arr = sparklines[symbol];
    if (!arr || arr.length < 2) return 0;
    const first = arr[0];
    const last = arr[arr.length - 1];
    if (first === 0) return 0;
    return ((last - first) / first) * 100;
  };

  return (
    <div className="markets-page">
      <header className="markets-header">
        <div className="markets-header-top">
          <div>
            <h1 className="markets-title">Borsa</h1>
            <p className="markets-date">{today}</p>
          </div>
          <div className="markets-header-actions">
            <div className="markets-search">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
                <path d="M21 21L17 17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              <input type="text" placeholder="Ara..." value={search}
                onChange={(e) => setSearch(e.target.value)} />
            </div>
            <button className="markets-menu-btn" onClick={() => {
              if (window.confirm('Çıkış yapmak istediğine emin misin?')) {
                logout();
                navigate('/login');
              }
            }} title="Çıkış">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <circle cx="5" cy="12" r="2" fill="currentColor" />
                <circle cx="12" cy="12" r="2" fill="currentColor" />
                <circle cx="19" cy="12" r="2" fill="currentColor" />
              </svg>
            </button>
          </div>
        </div>

        <button className="markets-portfolio-btn" onClick={() => navigate('/portfolio')}>
          <div className="markets-portfolio-btn-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M3 7H21V19C21 19.5 20.5 20 20 20H4C3.5 20 3 19.5 3 19V7Z"
                stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
              <path d="M3 7V5C3 4.5 3.5 4 4 4H20C20.5 4 21 4.5 21 5V7"
                stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
              <path d="M9 4V2H15V4" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
            </svg>
          </div>
          <div className="markets-portfolio-btn-text">
            <span className="markets-portfolio-btn-title">Varlıklarım</span>
            <span className="markets-portfolio-btn-sub">Portföyümü görüntüle</span>
          </div>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M9 6L15 12L9 18" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </header>

      {loading ? (
        <div className="markets-loading">
          <div className="spinner" />
          <p>Fiyatlar yükleniyor...</p>
        </div>
      ) : (
        <div className="markets-list fade-in">
          {filtered.length === 0 && (
            <p className="markets-empty">"{search}" için sonuç bulunamadı.</p>
          )}
          {filtered.map((p) => {
            const change = sparkChange(p.symbol);
            const positive = change >= 0;
            return (
              <div key={p.symbol} className="markets-row">
                <div className="markets-row-left">
                  <div className="markets-row-symbol">{p.symbol}</div>
                  <div className="markets-row-name">{getAssetName(p.symbol)}</div>
                </div>

                <div className="markets-row-spark">
                  <Sparkline data={sparklines[p.symbol] || []} positive={positive}
                    width={120} height={48} />
                </div>

                <div className="markets-row-right">
                  <div className="markets-row-price">
                    {formatTry(p.priceTry, p.priceTry < 100 ? 4 : 2)}
                  </div>
                  <div className={positive ? 'green-badge' : 'red-badge'}>
                    {change === 0 ? '—' : (positive ? '+' : '') + change.toFixed(2) + '%'}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default Markets;
