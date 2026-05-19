import { useState, useEffect, useRef } from 'react';
import api from '../api/axiosConfig';
import { useChat } from '../context/ChatContext';
import { useAuth } from '../context/AuthContext';
import './ChatWidget.css';

const ChatWidget = () => {
  const { isOpen, toggle, close, messages, setMessages } = useChat();
  const { user } = useAuth();
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef(null);

  // Ilk acilis - karsilama mesaji
  useEffect(() => {
    if (isOpen && messages.length === 0 && user) {
      setMessages([{
        role: 'assistant',
        text: 'Merhaba! Ben FinPortfolio AI asistanın. Portföyün, piyasa fiyatları veya kripto teknik analizi hakkında bana soru sorabilirsin.'
      }]);
    }
  }, [isOpen, user]);

  // Otomatik scroll
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, sending]);

  const handleSend = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setMessages((prev) => [...prev, { role: 'user', text }]);
    setInput('');
    setSending(true);

    try {
      const { data } = await api.post('/chat/message', { message: text });
      setMessages((prev) => [...prev, {
        role: 'assistant',
        text: data.reply || data.message || 'Yanıt alınamadı.'
      }]);
    } catch (err) {
      setMessages((prev) => [...prev, {
        role: 'assistant',
        text: 'Şu an sana ulaşamıyorum. Birazdan tekrar dene.',
        error: true
      }]);
    } finally {
      setSending(false);
    }
  };

  // Login/Register sayfasinda gosterme
  if (!user) return null;

  return (
    <>
      <button
        className={`chat-fab ${isOpen ? 'chat-fab-active' : ''}`}
        onClick={toggle}
        aria-label="AI asistanı aç"
      >
        <div className="chat-fab-glow" />
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L13.5 8.5L20 10L13.5 11.5L12 18L10.5 11.5L4 10L10.5 8.5L12 2Z"
            fill="white" />
        </svg>
      </button>

      {isOpen && (
        <div className="chat-panel">
          <header className="chat-panel-header">
            <div className="chat-panel-header-left">
              <div className="chat-panel-avatar">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L13.5 8.5L20 10L13.5 11.5L12 18L10.5 11.5L4 10L10.5 8.5L12 2Z"
                    fill="white" />
                </svg>
              </div>
              <div>
                <div className="chat-panel-title">AI Asistan</div>
                <div className="chat-panel-status">
                  <span className="chat-status-dot" />
                  Çevrimiçi
                </div>
              </div>
            </div>
            <button className="chat-panel-close" onClick={close} aria-label="Kapat">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M6 6L18 18M6 18L18 6" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" />
              </svg>
            </button>
          </header>

          <div className="chat-panel-messages">
            {messages.map((m, i) => (
              <div key={i} className={`chat-msg chat-msg-${m.role}`}>
                {m.role === 'assistant' && (
                  <div className="chat-msg-avatar">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M12 2L13.5 8.5L20 10L13.5 11.5L12 18L10.5 11.5L4 10L10.5 8.5L12 2Z"
                        fill="white" />
                    </svg>
                  </div>
                )}
                <div className={`chat-msg-bubble ${m.error ? 'chat-msg-error' : ''}`}>
                  {m.text}
                </div>
              </div>
            ))}

            {sending && (
              <div className="chat-msg chat-msg-assistant">
                <div className="chat-msg-avatar">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M12 2L13.5 8.5L20 10L13.5 11.5L12 18L10.5 11.5L4 10L10.5 8.5L12 2Z"
                      fill="white" />
                  </svg>
                </div>
                <div className="chat-msg-bubble chat-msg-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <form className="chat-panel-input" onSubmit={handleSend}>
            <input
              type="text"
              placeholder="Bir şey sor..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={sending}
              autoFocus
            />
            <button type="submit" disabled={!input.trim() || sending} aria-label="Gönder">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M5 12L19 12M19 12L13 6M19 12L13 18" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </form>
        </div>
      )}
    </>
  );
};

export default ChatWidget;
