import { StrictMode, useState, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'
import type { RetailerConfig } from './App'

const BANK_A_BASE_URL = (import.meta.env.VITE_BANK_A_BASE_URL as string) || 'http://localhost:8080';
const BANK_B_BASE_URL = (import.meta.env.VITE_BANK_B_BASE_URL as string) || 'http://localhost:8082';

const CONFIGS: Record<string, RetailerConfig> = {
  mms: {
    bankBaseUrl: BANK_A_BASE_URL,
    creditorIban: 'DE89370400440532013099',
    creditorName: 'MediaMarkt Saturn',
    bankLabel: 'MediaMarkt Saturn',
    accentColor: '#e3000f',
  },
  rew: {
    bankBaseUrl: BANK_B_BASE_URL,
    creditorIban: 'DE89370400440532014099',
    creditorName: 'REWE Group',
    bankLabel: 'REWE Group',
    accentColor: '#005CA9',
  },
};

function Landing() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: '1.5rem', fontFamily: 'sans-serif' }}>
      <img src="/img/BlinkPay-logo.png" alt="BlinkPay" style={{ height: 64 }} />
      <h2 style={{ margin: 0 }}>Select Retailer Terminal</h2>
      <div style={{ display: 'flex', gap: '1rem' }}>
        <a href="#mms" style={{ padding: '1rem 2rem', background: '#e3000f', color: '#fff', borderRadius: 8, textDecoration: 'none', fontWeight: 600 }}>
          MediaMarkt Saturn<br /><small style={{ fontWeight: 400 }}>MMS</small>
        </a>
        <a href="#rew" style={{ padding: '1rem 2rem', background: '#cc071e', color: '#fff', borderRadius: 8, textDecoration: 'none', fontWeight: 600 }}>
          REWE Group<br /><small style={{ fontWeight: 400 }}>REW</small>
        </a>
      </div>
    </div>
  );
}

function Root() {
  const [hash, setHash] = useState(window.location.hash.replace('#', ''));

  useEffect(() => {
    const onHash = () => setHash(window.location.hash.replace('#', ''));
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  const config = CONFIGS[hash];
  if (config) return <App config={config} />;
  return <Landing />;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
