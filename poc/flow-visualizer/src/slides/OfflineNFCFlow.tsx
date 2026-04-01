import { useFlowAnimation } from './useFlowAnimation'
import { FlowControls } from './FlowControls'

const steps = [
  'Devices brought together',
  'NFC handshake established',
  'Biometric confirmation',
  'Digital Euro transferred',
  'Both wallets updated',
]

// Devices centered at top
const device1 = { x: 200, y: 60, w: 160, h: 70 }
const device2 = { x: 660, y: 60, w: 160, h: 70 }

// NFC waves center
const nfcX = (device1.x + device1.w + device2.x) / 2
const nfcY = device1.y + 35

// Bank + FIPS as greyed-out infrastructure foundation at bottom
const infra = { x: 130, y: 310, w: 740, h: 110 }

const highlightMap: Record<number, string[]> = {
  0: ['device1', 'device2'],
  1: ['device1', 'device2'],
  2: ['device1'],
  3: ['device1', 'device2'],
  4: ['device1', 'device2'],
}

export function OfflineNFCFlow({ active }: { active: boolean }) {
  const { currentStep, playing, finished, play, pause, resume, reset, prevStep, nextStep } = useFlowAnimation(steps.length, active, 3500)
  const highlighted = highlightMap[currentStep] ?? []

  return (
    <div className="slide-flow">
      <h2>Scenario 3 — Offline NFC Transfer</h2>
      <p className="flow-subtitle">
        Digital Euro transferred directly between devices via NFC. Completely offline — no bank or FIPS involved.
      </p>

      <div className="flow-diagram">
        <svg viewBox="0 0 1060 460" xmlns="http://www.w3.org/2000/svg">
          {/* NFC waves between devices */}
          {(currentStep === 1 || currentStep === 3) && (
            <g>
              <circle cx={nfcX} cy={nfcY} r={18} fill="none" stroke="#0097B9" strokeWidth={2}>
                <animate attributeName="r" from="18" to="50" dur="1.5s" repeatCount="indefinite" />
                <animate attributeName="opacity" from="0.8" to="0" dur="1.5s" repeatCount="indefinite" />
              </circle>
              <circle cx={nfcX} cy={nfcY} r={18} fill="none" stroke="#0097B9" strokeWidth={2}>
                <animate attributeName="r" from="18" to="65" dur="1.5s" begin="0.3s" repeatCount="indefinite" />
                <animate attributeName="opacity" from="0.6" to="0" dur="1.5s" begin="0.3s" repeatCount="indefinite" />
              </circle>
              <circle cx={nfcX} cy={nfcY} r={18} fill="none" stroke="#0097B9" strokeWidth={2}>
                <animate attributeName="r" from="18" to="80" dur="1.5s" begin="0.6s" repeatCount="indefinite" />
                <animate attributeName="opacity" from="0.4" to="0" dur="1.5s" begin="0.6s" repeatCount="indefinite" />
              </circle>
            </g>
          )}

          {/* NFC label */}
          <text
            x={nfcX} y={nfcY - 55}
            textAnchor="middle"
            className={`flow-step-text ${currentStep >= 1 && currentStep <= 3 ? 'active' : ''}`}
            style={{ fontSize: 13 }}
          >
            NFC
          </text>

          {/* Transfer arrow (step 3) */}
          <line
            x1={device1.x + device1.w} y1={nfcY} x2={device2.x} y2={nfcY}
            stroke="#0097B9"
            strokeWidth={3}
            className={`flow-arrow ${currentStep === 3 ? 'active' : currentStep > 3 ? 'done' : ''}`}
          />
          {currentStep === 3 && (
            <text x={nfcX} y={nfcY + 50} textAnchor="middle" className="flow-step-text active">
              Digital Euro transfer
            </text>
          )}

          {/* Biometric indicator on device 1 */}
          {currentStep === 2 && (
            <g>
              <circle cx={device1.x + device1.w / 2} cy={device1.y + device1.h + 18} r={16} fill="none" stroke="#0097B9" strokeWidth={2}>
                <animate attributeName="opacity" from="1" to="0.3" dur="0.8s" repeatCount="indefinite" />
              </circle>
              <text x={device1.x + device1.w / 2} y={device1.y + device1.h + 23} textAnchor="middle" style={{ fontSize: 16 }}>👆</text>
              <text x={device1.x + device1.w / 2} y={device1.y + device1.h + 50} textAnchor="middle" className="flow-step-text active">
                Biometric SCA
              </text>
            </g>
          )}

          {/* Success checkmarks (step 4) */}
          {currentStep >= 4 && (
            <>
              <text x={device1.x + device1.w / 2} y={device1.y + device1.h + 30} textAnchor="middle" style={{ fontSize: 28, fill: '#059669' }}>✓</text>
              <text x={device2.x + device2.w / 2} y={device2.y + device2.h + 30} textAnchor="middle" style={{ fontSize: 28, fill: '#059669' }}>✓</text>
            </>
          )}

          {/* "Offline" badge */}
          <rect x={nfcX - 100} y={190} width={200} height={32} rx={16} fill="rgba(0,151,185,0.15)" stroke="#0097B9" strokeWidth={1} />
          <text x={nfcX} y={211} textAnchor="middle" style={{ fontSize: 13, fontWeight: 700, fill: '#0097B9', fontFamily: 'Open Sans' }}>
            COMPLETELY OFFLINE
          </text>

          {/* ─── Infrastructure foundation layer (greyed out) ─── */}
          <rect
            x={infra.x} y={infra.y} width={infra.w} height={infra.h} rx={18}
            fill="#E2E8F0"
            opacity={0.12}
            stroke="#94A3B8"
            strokeWidth={1}
            strokeDasharray="6 4"
          />

          {/* Bank sub-box inside infra */}
          <rect x={infra.x + 30} y={infra.y + 16} width={180} height={56} rx={10} fill="#6366F1" opacity={0.1} stroke="#94A3B8" strokeWidth={1} />
          <text x={infra.x + 48} y={infra.y + 42} fill="#94A3B8" fontSize={12} fontWeight={600} opacity={0.4}>
            🏦 Bank
          </text>
          <text x={infra.x + 48} y={infra.y + 58} fill="#94A3B8" fontSize={10} opacity={0.3}>
            Not involved
          </text>

          {/* FIPS sub-box inside infra */}
          <rect x={infra.x + 240} y={infra.y + 16} width={470} height={56} rx={10} fill="#D97706" opacity={0.06} stroke="#94A3B8" strokeWidth={1} />
          <text x={infra.x + 475} y={infra.y + 42} textAnchor="middle" fill="#94A3B8" fontSize={12} fontWeight={600} opacity={0.4}>
            🔄 FIPS — Settlement Infrastructure
          </text>
          <text x={infra.x + 475} y={infra.y + 58} textAnchor="middle" fill="#94A3B8" fontSize={10} opacity={0.3}>
            Not involved — transfer is peer-to-peer
          </text>

          {/* X marks — dashed lines from devices down to infra, crossed out */}
          <line x1={device1.x + device1.w / 2} y1={device1.y + device1.h + 60} x2={infra.x + 120} y2={infra.y} stroke="#94A3B8" strokeWidth={1} strokeDasharray="4 4" opacity={0.15} />
          <line x1={device2.x + device2.w / 2} y1={device2.y + device2.h + 60} x2={infra.x + infra.w - 120} y2={infra.y} stroke="#94A3B8" strokeWidth={1} strokeDasharray="4 4" opacity={0.15} />
          <text x={(device1.x + device1.w / 2 + infra.x + 120) / 2} y={250} textAnchor="middle" style={{ fontSize: 20, fill: '#D32F2F', opacity: 0.45 }}>✕</text>
          <text x={(device2.x + device2.w / 2 + infra.x + infra.w - 120) / 2} y={250} textAnchor="middle" style={{ fontSize: 20, fill: '#D32F2F', opacity: 0.45 }}>✕</text>

          {/* ─── Device nodes ─── */}
          <g className={`flow-node ${highlighted.includes('device1') ? 'highlight' : ''}`} transform={`translate(${device1.x}, ${device1.y})`}>
            <rect width={device1.w} height={device1.h} fill="#2563EB" opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>📱</text>
            <text className="node-label" x={56} y={38}>Sender Device</text>
          </g>

          <g className={`flow-node ${highlighted.includes('device2') ? 'highlight' : ''}`} transform={`translate(${device2.x}, ${device2.y})`}>
            <rect width={device2.w} height={device2.h} fill="#059669" opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>📱</text>
            <text className="node-label" x={56} y={38}>Receiver Device</text>
          </g>
        </svg>
      </div>

      {/* Current action callout */}
      <div className={`flow-action-box ${currentStep >= 0 && currentStep < steps.length ? 'visible' : ''}`}>
        <span className="flow-action-step">Step {currentStep + 1} of {steps.length}</span>
        <span className="flow-action-label">{steps[Math.max(0, Math.min(currentStep, steps.length - 1))]}</span>
      </div>

      <div className="flow-steps">
        {steps.map((label, i) => (
          <div
            key={i}
            className={`flow-step ${currentStep === i ? 'active' : currentStep > i ? 'done' : ''}`}
          >
            <span className="flow-step-dot" />
            {label}
          </div>
        ))}
      </div>

      <FlowControls
        currentStep={currentStep}
        stepCount={steps.length}
        playing={playing}
        finished={finished}
        onPrev={prevStep}
        onNext={nextStep}
        onPause={pause}
        onResume={resume}
        onReset={reset}
      />
    </div>
  )
}
