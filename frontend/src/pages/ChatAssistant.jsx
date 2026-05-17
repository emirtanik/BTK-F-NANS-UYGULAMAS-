import Navbar from '../components/Navbar';
import ChatPanel from '../components/ChatPanel';
import { MessageCircle } from 'lucide-react';

const ChatAssistant = () => (
  <>
    <Navbar />
    <div className="container" style={{ marginTop: '24px' }}>
      <ChatPanel
        title="Sohbet Asistanı"
        subtitle="Portföyün hakkında serbestçe sohbet et, sorularını sor."
        icon={MessageCircle}
        welcomeUrl="/chat/welcome"
        sendUrl="/chat/message"
        placeholder="Örn: Portföyüm dengeli mi?"
        accentColor="var(--primary)"
      />
    </div>
  </>
);

export default ChatAssistant;
