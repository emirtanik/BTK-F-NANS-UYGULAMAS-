import Navbar from '../components/Navbar';
import ChatPanel from '../components/ChatPanel';
import { Lightbulb } from 'lucide-react';

const AdvisorAssistant = () => (
  <>
    <Navbar />
    <div className="container" style={{ marginTop: '24px' }}>
      <ChatPanel
        title="Portföy Danışmanı"
        subtitle="Risk, çeşitlendirme ve strateji konusunda akıl al."
        icon={Lightbulb}
        welcomeUrl="/chat/advice/welcome"
        sendUrl="/chat/advice"
        placeholder="Örn: Riskimi nasıl azaltabilirim?"
        accentColor="var(--success)"
      />
    </div>
  </>
);

export default AdvisorAssistant;
