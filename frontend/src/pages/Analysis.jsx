import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../api/axiosConfig';
import { Brain, AlertCircle, ShieldCheck } from 'lucide-react';

const Analysis = () => {
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchAnalysis = async () => {
      try {
        const { data } = await api.get('/portfolio/analysis');
        setAnalysis(data);
      } catch {
        setError('Analiz bilgileri alınamadı. Portföyünüz boş olabilir veya servis geçici olarak kullanım dışı olabilir.');
      } finally {
        setLoading(false);
      }
    };
    fetchAnalysis();
  }, []);

  if (loading) return <div className="flex-center" style={{ flex: 1 }}><Brain size={48} color="var(--primary)" /></div>;

  return (
    <>
      <Navbar />
      <div className="container" style={{ marginTop: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
          <Brain size={32} color="var(--primary)" />
          <h2 style={{ margin: 0 }}>Yapay Zeka Portföy Analizi</h2>
        </div>

        {error ? (
          <div className="glass-panel" style={{ textAlign: 'center', color: 'var(--danger)' }}>
            <AlertCircle size={48} style={{ margin: '0 auto 16px auto' }} />
            <p>{error}</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div className="glass-panel" style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
              <div style={{ 
                background: analysis?.riskScore > 70 ? 'rgba(239, 68, 68, 0.2)' : analysis?.riskScore > 40 ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)', 
                padding: '24px', 
                borderRadius: '50%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                width: '120px',
                height: '120px'
              }}>
                <span style={{ fontSize: '2rem', fontWeight: 'bold' }}>{analysis?.riskScore || 0}</span>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Risk Skoru</span>
              </div>
              <div style={{ flex: 1 }}>
                <h3>Risk Seviyesi: {analysis?.riskLevel || '—'}</h3>
                <p>
                  Çeşitlendirme skoru: {analysis?.diversificationScore ?? 0}/100.
                  Bu skor, varlıklarınızın dağılımı ve piyasa koşulları değerlendirilerek oluşturulmuştur.
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                  <ShieldCheck size={24} color="var(--primary)" />
                  <h3 style={{ margin: 0 }}>Uyarılar</h3>
                </div>
                {analysis?.warnings?.length > 0 ? (
                  <ul style={{ paddingLeft: '20px' }}>
                    {analysis.warnings.map((warning, index) => (
                      <li key={index} style={{ marginBottom: '8px', color: 'var(--text-secondary)' }}>{warning}</li>
                    ))}
                  </ul>
                ) : (
                  <p>Uyarı bulunmuyor.</p>
                )}
              </div>

              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                  <AlertCircle size={24} color="var(--primary)" />
                  <h3 style={{ margin: 0 }}>Genel Öneriler</h3>
                </div>
                {analysis?.recommendations?.length > 0 ? (
                  <ul style={{ paddingLeft: '20px' }}>
                    {analysis.recommendations.map((rec, index) => (
                      <li key={index} style={{ marginBottom: '8px', color: 'var(--text-secondary)' }}>{rec}</li>
                    ))}
                  </ul>
                ) : (
                  <p>Yeterli öneri bulunamadı.</p>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default Analysis;
