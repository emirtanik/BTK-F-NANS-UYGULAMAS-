import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../api/axiosConfig';
import { DollarSign, TrendingUp, TrendingDown, Plus, Trash2 } from 'lucide-react';

const formatTry = (value) =>
  Number(value ?? 0).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const Dashboard = () => {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [assetSymbol, setAssetSymbol] = useState('');
  const [amount, setAmount] = useState('');
  const [buyPriceTry, setBuyPriceTry] = useState('');
  const [assetType, setAssetType] = useState('CRYPTO');

  const fetchPortfolio = async () => {
    try {
      const { data } = await api.get('/portfolio');
      setPortfolio(data);
      setError('');
    } catch {
      setError('Portföy bilgileri alınamadı.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPortfolio();
  }, []);

  const handleAddItem = async (e) => {
    e.preventDefault();
    try {
      await api.post('/portfolio/items', {
        assetSymbol,
        amount: Number(amount),
        buyPriceTry: Number(buyPriceTry),
        assetType,
      });
      setAssetSymbol('');
      setAmount('');
      setBuyPriceTry('');
      fetchPortfolio();
    } catch {
      setError('Varlık eklenirken bir hata oluştu.');
    }
  };

  const handleDeleteItem = async (itemId) => {
    try {
      await api.delete(`/portfolio/items/${itemId}`);
      fetchPortfolio();
    } catch {
      setError('Varlık silinirken bir hata oluştu.');
    }
  };

  if (loading) return <div className="flex-center" style={{ flex: 1 }}>Yükleniyor...</div>;

  const totalProfitLoss = Number(portfolio?.totalProfitLossTry ?? 0);

  return (
    <>
      <Navbar />
      <div className="container" style={{ marginTop: '32px' }}>
        {error && <div className="error-text" style={{ marginBottom: '16px' }}>{error}</div>}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '24px', marginBottom: '32px' }}>
          <div className="glass-panel" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ background: 'rgba(59, 130, 246, 0.2)', padding: '16px', borderRadius: '12px' }}>
              <DollarSign size={32} color="var(--primary)" />
            </div>
            <div>
              <p style={{ margin: 0, fontSize: '0.9rem' }}>Toplam Değer</p>
              <h2 style={{ margin: 0 }}>₺{formatTry(portfolio?.totalCurrentValueTry)}</h2>
            </div>
          </div>

          <div className="glass-panel" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ background: totalProfitLoss >= 0 ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)', padding: '16px', borderRadius: '12px' }}>
              {totalProfitLoss >= 0 ? <TrendingUp size={32} color="var(--success)" /> : <TrendingDown size={32} color="var(--danger)" />}
            </div>
            <div>
              <p style={{ margin: 0, fontSize: '0.9rem' }}>Toplam Kar/Zarar</p>
              <h2 style={{ margin: 0, color: totalProfitLoss >= 0 ? 'var(--success)' : 'var(--danger)' }}>
                ₺{formatTry(portfolio?.totalProfitLossTry)}
              </h2>
            </div>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px' }}>
          <div className="glass-panel" style={{ alignSelf: 'start' }}>
            <h3 style={{ marginBottom: '16px' }}>Yeni Varlık Ekle</h3>
            <form onSubmit={handleAddItem}>
              <div className="input-group">
                <label>Sembol (Örn: BTC, GRAM_ALTIN)</label>
                <input type="text" className="input-field" value={assetSymbol} onChange={e => setAssetSymbol(e.target.value.toUpperCase())} required />
              </div>
              <div className="input-group">
                <label>Miktar</label>
                <input type="number" step="0.0001" className="input-field" value={amount} onChange={e => setAmount(e.target.value)} required />
              </div>
              <div className="input-group">
                <label>Alış Fiyatı (₺)</label>
                <input type="number" step="0.01" className="input-field" value={buyPriceTry} onChange={e => setBuyPriceTry(e.target.value)} required />
              </div>
              <div className="input-group">
                <label>Tür</label>
                <select className="input-field" value={assetType} onChange={e => setAssetType(e.target.value)}>
                  <option value="CRYPTO">Kripto</option>
                  <option value="CURRENCY">Döviz</option>
                  <option value="GOLD">Altın</option>
                  <option value="SILVER">Gümüş</option>
                </select>
              </div>
              <button type="submit" className="btn" style={{ marginTop: '8px' }}>
                <Plus size={18} /> Ekle
              </button>
            </form>
          </div>

          <div className="glass-panel">
            <h3 style={{ marginBottom: '16px' }}>Portföy Varlıkları</h3>
            {portfolio?.items?.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--surface-border)', color: 'var(--text-secondary)' }}>
                      <th style={{ padding: '12px 8px' }}>Sembol</th>
                      <th style={{ padding: '12px 8px' }}>Miktar</th>
                      <th style={{ padding: '12px 8px' }}>Alış Fiyatı</th>
                      <th style={{ padding: '12px 8px' }}>Güncel Fiyat</th>
                      <th style={{ padding: '12px 8px' }}>Kar/Zarar</th>
                      <th style={{ padding: '12px 8px' }}>İşlem</th>
                    </tr>
                  </thead>
                  <tbody>
                    {portfolio.items.map(item => (
                      <tr key={item.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <td style={{ padding: '12px 8px', fontWeight: '500' }}>{item.assetSymbol}</td>
                        <td style={{ padding: '12px 8px' }}>{item.amount}</td>
                        <td style={{ padding: '12px 8px' }}>₺{formatTry(item.buyPriceTry)}</td>
                        <td style={{ padding: '12px 8px' }}>₺{formatTry(item.currentPriceTry ?? item.buyPriceTry)}</td>
                        <td style={{ padding: '12px 8px', color: Number(item.profitLossTry) >= 0 ? 'var(--success)' : 'var(--danger)' }}>
                          ₺{formatTry(item.profitLossTry)}
                        </td>
                        <td style={{ padding: '12px 8px' }}>
                          <button onClick={() => handleDeleteItem(item.id)} style={{ background: 'none', border: 'none', color: 'var(--danger)', cursor: 'pointer' }}>
                            <Trash2 size={18} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ textAlign: 'center', padding: '32px 0' }}>Portföyünüzde henüz varlık bulunmamaktadır.</p>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard;
