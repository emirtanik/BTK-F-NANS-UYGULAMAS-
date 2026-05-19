import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import { extractErrorMessage } from '../api/apiError';
import { formatTry, formatPercent, formatAmount, formatDate, getAssetName, getAssetGroup } from '../utils/format';
import './Portfolio.css';

const ASSET_OPTIONS = [
  { group: 'Kripto Para', items: ['BTC', 'ETH', 'BNB', 'SOL', 'XRP', 'ADA'] },
  { group: 'Kıymetli Maden', items: ['GRAM_ALTIN', 'CEYREK_ALTIN', 'YARIM_ALTIN', 'TAM_ALTIN', 'GRAM_GUMUS'] },
  { group: 'Döviz', items: ['USD', 'EUR', 'GBP'] },
];

const Portfolio = () => {
  const navigate = useNavigate();
  const [portfolio, setPortfolio] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showAddModal, setShowAddModal] = useState(false);
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState('');
  const [form, setForm] = useState({
    assetSymbol: 'BTC',
    amount: '',
    buyPriceTry: '',
    buyDate: new Date().toISOString().split('T')[0],
  });

  const load = async () => {
    try {
      const [portfolioRes, analysisRes] = await Promise.all([
        api.get('/portfolio'),
        api.get('/portfolio/analysis').catch(() => ({ data: null })),
      ]);
      setPortfolio(portfolioRes.data);
      setAnalysis(analysisRes.data);
    } catch (err) {
      console.error(err);
      setError(extractErrorMessage(err, 'Portföy yüklenemedi.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    setAddError('');
    setAdding(true);
    try {
      await api.post('/portfolio/items', {
        assetSymbol: form.assetSymbol,
        amount: parseFloat(form.amount),
        buyPriceTry: parseFloat(form.buyPriceTry),
        buyDate: form.buyDate,
      });
      setShowAddModal(false);
      setForm({
        assetSymbol: 'BTC',
        amount: '',
        buyPriceTry: '',
        buyDate: new Date().toISOString().split('T')[0],
      });
      await load();
    } catch (err) {
      setAddError(extractErrorMessage(err, 'Yatırım eklenemedi.'));
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (itemId) => {
    if (!window.confirm('Bu yatırımı silmek istediğine emin misin?')) return;
    try {
      await api.delete(`/portfolio/items/${itemId}`);
      await load();
    } catch (err) {
      alert(extractErrorMessage(err, 'Silinemedi.'));
    }
  };

  if (loading) {
    return (
      <div className="portfolio-page">
        <div className="portfolio-loading">
          <div className="spinner" />
          <p>Portföyün yükleniyor...</p>
        </div>
      </div>
    );
  }

  const items = portfolio?.items || [];
  const totalValue = portfolio?.totalValueTry || 0;
  const totalCost = portfolio?.totalCostTry || 0;
  const totalPnl = portfolio?.totalProfitLossTry || 0;
  const totalPnlPercent = portfolio?.totalProfitLossPercent || 0;
  const isPositive = Number(totalPnl) >= 0;

  return (
    <div className="portfolio-page">
      <header className="portfolio-header">
        <button className="portfolio-back" onClick={() => navigate('/markets')}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M15 6L9 12L15 18" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          Borsa
        </button>
        <h1 className="portfolio-title">Varlıklarım</h1>
        <button className="portfolio-add-btn" onClick={() => setShowAddModal(true)}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M12 5V19M5 12H19" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" />
          </svg>
          Ekle
        </button>
      </header>

      {error && <div className="portfolio-error">{error}</div>}

      <section className="portfolio-overview">
        <div className="portfolio-overview-card">
          <span className="portfolio-overview-label">Toplam Değer</span>
          <span className="portfolio-overview-value">{formatTry(totalValue)} ₺</span>
        </div>
        <div className="portfolio-overview-card">
          <span className="portfolio-overview-label">Toplam Maliyet</span>
          <span className="portfolio-overview-value">{formatTry(totalCost)} ₺</span>
        </div>
        <div className="portfolio-overview-card">
          <span className="portfolio-overview-label">Kar / Zarar</span>
          <span className={`portfolio-overview-value ${isPositive ? 'green-text' : 'red-text'}`}>
            {isPositive ? '+' : ''}{formatTry(totalPnl)} ₺
          </span>
          <span className={`portfolio-overview-percent ${isPositive ? 'green-badge' : 'red-badge'}`}>
            {formatPercent(totalPnlPercent)}
          </span>
        </div>
      </section>

      {analysis && (
        <section className="portfolio-analysis">
          <div className="portfolio-analysis-row">
            <div className="portfolio-analysis-item">
              <span className="portfolio-analysis-label">Risk Skoru</span>
              <span className="portfolio-analysis-value">{analysis.riskScore || '—'}/100</span>
              <span className="portfolio-analysis-sub">{analysis.riskLevel}</span>
            </div>
            <div className="portfolio-analysis-item">
              <span className="portfolio-analysis-label">Çeşitlendirme</span>
              <span className="portfolio-analysis-value">{analysis.diversificationScore || '—'}/100</span>
              <span className="portfolio-analysis-sub">{analysis.totalAssets} varlık</span>
            </div>
          </div>
          {analysis.recommendations?.length > 0 && (
            <div className="portfolio-analysis-list">
              <h4>Öneriler</h4>
              <ul>
                {analysis.recommendations.slice(0, 3).map((r, i) => <li key={i}>{r}</li>)}
              </ul>
            </div>
          )}
        </section>
      )}

      <section className="portfolio-items">
        <h2 className="portfolio-section-title">Yatırımlarım</h2>

        {items.length === 0 ? (
          <div className="portfolio-empty">
            <p>Henüz yatırımın yok.</p>
            <button className="portfolio-empty-btn" onClick={() => setShowAddModal(true)}>
              İlk yatırımını ekle
            </button>
          </div>
        ) : (
          <div className="portfolio-items-list">
            {items.map((item) => {
              const pnl = Number(item.profitLossTry) || 0;
              const pnlPct = Number(item.profitLossPercent) || 0;
              const positive = pnl >= 0;
              return (
                <div key={item.id} className="portfolio-item">
                  <div className="portfolio-item-main">
                    <div className="portfolio-item-symbol">{item.assetSymbol}</div>
                    <div className="portfolio-item-name">{getAssetName(item.assetSymbol)}</div>
                    <div className="portfolio-item-meta">
                      <span>{formatAmount(item.amount, item.assetSymbol)} adet</span>
                      <span>•</span>
                      <span>{formatDate(item.buyDate)}</span>
                    </div>
                  </div>

                  <div className="portfolio-item-stats">
                    <div className="portfolio-item-stat">
                      <span className="portfolio-item-stat-label">Alış</span>
                      <span className="portfolio-item-stat-value">{formatTry(item.buyPriceTry)} ₺</span>
                    </div>
                    <div className="portfolio-item-stat">
                      <span className="portfolio-item-stat-label">Güncel</span>
                      <span className="portfolio-item-stat-value">
                        {item.currentPriceTry ? formatTry(item.currentPriceTry) + ' ₺' : '—'}
                      </span>
                    </div>
                    <div className="portfolio-item-stat">
                      <span className="portfolio-item-stat-label">K/Z</span>
                      <span className={`portfolio-item-stat-value ${positive ? 'green-text' : 'red-text'}`}>
                        {positive ? '+' : ''}{formatTry(pnl)} ₺
                      </span>
                    </div>
                  </div>

                  <div className="portfolio-item-right">
                    <div className={positive ? 'green-badge' : 'red-badge'}>
                      {formatPercent(pnlPct)}
                    </div>
                    <button className="portfolio-item-delete" onClick={() => handleDelete(item.id)}
                      title="Sil">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M3 6H21M8 6V4C8 3.5 8.5 3 9 3H15C15.5 3 16 3.5 16 4V6M5 6L6 20C6 20.5 6.5 21 7 21H17C17.5 21 18 20.5 18 20L19 6"
                          stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {showAddModal && (
        <div className="portfolio-modal-backdrop" onClick={() => setShowAddModal(false)}>
          <div className="portfolio-modal" onClick={(e) => e.stopPropagation()}>
            <div className="portfolio-modal-header">
              <h3>Yeni Yatırım Ekle</h3>
              <button className="portfolio-modal-close" onClick={() => setShowAddModal(false)}>×</button>
            </div>

            {addError && <div className="auth-error">{addError}</div>}

            <form onSubmit={handleAdd} className="portfolio-form">
              <div className="auth-field">
                <label>Varlık</label>
                <select value={form.assetSymbol}
                  onChange={(e) => setForm({ ...form, assetSymbol: e.target.value })}>
                  {ASSET_OPTIONS.map((g) => (
                    <optgroup key={g.group} label={g.group}>
                      {g.items.map((s) => (
                        <option key={s} value={s}>{getAssetName(s)} ({s})</option>
                      ))}
                    </optgroup>
                  ))}
                </select>
              </div>

              <div className="auth-field">
                <label>Miktar</label>
                <input type="number" step="any" min="0" placeholder="0.025" value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })} required />
              </div>

              <div className="auth-field">
                <label>Alış Fiyatı (₺)</label>
                <input type="number" step="any" min="0" placeholder="3500000" value={form.buyPriceTry}
                  onChange={(e) => setForm({ ...form, buyPriceTry: e.target.value })} required />
              </div>

              <div className="auth-field">
                <label>Alış Tarihi</label>
                <input type="date" value={form.buyDate}
                  onChange={(e) => setForm({ ...form, buyDate: e.target.value })} required />
              </div>

              <button type="submit" className="auth-submit" disabled={adding}>
                {adding ? 'Ekleniyor...' : 'Yatırımı Ekle'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Portfolio;
