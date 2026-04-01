import { ConnectionStatus } from '../types'

interface Props {
  connections: Record<string, ConnectionStatus>
  onClear: () => void
}

const STATUS_COLORS: Record<ConnectionStatus, string> = {
  connected: '#059669',
  connecting: '#D97706',
  disconnected: '#DC2626',
}

export function Header({ connections, onClear }: Props) {
  const allConnected = Object.values(connections).every(s => s === 'connected')

  return (
    <header className="header">
      <div className="header-left">
        <img src="/img/BlinkPay-logo.png" alt="BlinkPay" className="header-logo" />
        <h1 className="header-title">Payment Flow Visualizer</h1>
      </div>
      <div className="header-right">
        <div className="live-indicator">
          <span
            className={`live-dot ${allConnected ? 'live-dot-active' : ''}`}
            style={{ background: allConnected ? '#059669' : '#D97706' }}
          />
          <span className="live-label">{allConnected ? 'LIVE' : 'CONNECTING'}</span>
        </div>
        <div className="connection-badges">
          {Object.entries(connections).map(([name, status]) => (
            <span
              key={name}
              className="connection-badge"
              style={{ borderColor: STATUS_COLORS[status] }}
            >
              <span className="badge-dot" style={{ background: STATUS_COLORS[status] }} />
              {name}
            </span>
          ))}
        </div>
        <button className="clear-btn" onClick={onClear}>Clear</button>
      </div>
    </header>
  )
}
