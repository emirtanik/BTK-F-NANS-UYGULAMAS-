import { useEffect, useRef, useState } from 'react';
import { Send, Loader2 } from 'lucide-react';
import api from '../api/axiosConfig';

const ChatPanel = ({
  title,
  subtitle,
  icon: Icon,
  welcomeUrl,
  sendUrl,
  placeholder = 'Mesajınızı yazın...',
  accentColor = 'var(--primary)',
}) => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const bottomRef = useRef(null);

  useEffect(() => {
    const loadWelcome = async () => {
      try {
        setError('');
        const { data } = await api.get(welcomeUrl);
        const text = data.botMessage ?? data.message;
        setMessages([{ role: 'bot', text }]);
      } catch {
        setError('Asistan yüklenemedi. Lütfen sayfayı yenileyin.');
      } finally {
        setLoading(false);
      }
    };
    loadWelcome();
  }, [welcomeUrl]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setSending(true);
    setError('');

    try {
      const { data } = await api.post(sendUrl, { message: text });
      setMessages((prev) => [...prev, { role: 'bot', text: data.message }]);
    } catch {
      setError('Yanıt alınamadı. Tekrar deneyin.');
      setMessages((prev) => prev.slice(0, -1));
      setInput(text);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="chat-page">
      <div className="glass-panel chat-header" style={{ borderLeft: `4px solid ${accentColor}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {Icon && (
            <div style={{ background: `${accentColor}33`, padding: '12px', borderRadius: '12px' }}>
              <Icon size={28} color={accentColor} />
            </div>
          )}
          <div>
            <h2 style={{ margin: 0 }}>{title}</h2>
            <p style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>{subtitle}</p>
          </div>
        </div>
      </div>

      {error && <div className="error-text chat-error">{error}</div>}

      <div className="glass-panel chat-messages">
        {loading ? (
          <div className="flex-center" style={{ padding: '48px', gap: '12px' }}>
            <Loader2 size={24} className="spin" color={accentColor} />
            <span>Yükleniyor...</span>
          </div>
        ) : (
          messages.map((msg, i) => (
            <div
              key={i}
              className={`chat-bubble ${msg.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-bot'}`}
              style={msg.role === 'bot' ? { borderLeft: `3px solid ${accentColor}` } : {}}
            >
              {msg.text.split('\n').map((line, j) => (
                <p key={j} style={{ margin: j > 0 ? '8px 0 0' : 0, whiteSpace: 'pre-wrap' }}>{line}</p>
              ))}
            </div>
          ))
        )}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSend} className="chat-input-row">
        <input
          type="text"
          className="input-field"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={placeholder}
          disabled={loading || sending}
        />
        <button type="submit" className="btn" disabled={loading || sending || !input.trim()} style={{ width: 'auto', padding: '12px 20px' }}>
          {sending ? <Loader2 size={20} className="spin" /> : <Send size={20} />}
        </button>
      </form>
    </div>
  );
};

export default ChatPanel;

