import { useFlowAnimation } from './useFlowAnimation'
import { FlowControls } from './FlowControls'

const steps = [
  'Customer scans QR code',
  'Payment sent to Bank A',
  'Bank A forwards to FIPS',
  'FIPS routes to Bank B',
  'Bank B credits retailer',
  'Retailer receives confirmation',
]

// Layout: Apps top row, Banks middle row, FIPS as wide foundation layer at bottom
const clientNode   = { x: 60,  y: 40,  w: 160, h: 70 }
const retailerNode = { x: 840, y: 40,  w: 160, h: 70 }
const bankA        = { x: 100, y: 200, w: 200, h: 90 }
const bankB        = { x: 700, y: 200, w: 200, h: 90 }

// FIPS backbone — wide box spanning beneath both banks
const fips = { x: 130, y: 370, w: 740, h: 100 }

function arrowPath(fx: number, fy: number, tx: number, ty: number, bend = -30): string {
  const cx = (fx + tx) / 2, cy = (fy + ty) / 2 + bend
  return `M${fx},${fy} Q${cx},${cy} ${tx},${ty}`
}

const highlightMap: Record<number, string[]> = {
  0: ['client', 'retailer'],
  1: ['client', 'bankA'],
  2: ['bankA', 'fips'],
  3: ['fips', 'bankB'],
  4: ['bankB', 'retailer'],
  5: ['retailer'],
}

export function CrossBankFlow({ active }: { active: boolean }) {
  const { currentStep, playing, finished, play, pause, resume, reset, prevStep, nextStep } = useFlowAnimation(steps.length, active)
  const hl = highlightMap[currentStep] ?? []

  const isHl = (key: string) => hl.includes(key)

  return (
    <div className="slide-flow">
      <h2>Scenario 2 — Cross-Bank Payment</h2>
      <p className="flow-subtitle">Customer and retailer use different banks. Payment routed through the FIPS backbone.</p>

      <div className="flow-diagram">
        <svg viewBox="0 0 1060 510" xmlns="http://www.w3.org/2000/svg">

          {/* ─── FIPS backbone layer ─── */}
          <rect
            x={fips.x} y={fips.y} width={fips.w} height={fips.h} rx={18}
            fill={isHl('fips') ? '#D97706' : '#E2E8F0'}
            opacity={isHl('fips') ? 0.25 : 0.15}
            stroke={isHl('fips') ? '#F59E0B' : '#94A3B8'}
            strokeWidth={isHl('fips') ? 2.5 : 1.5}
          />
          <text x={fips.x + fips.w / 2} y={fips.y + 36} textAnchor="middle" fill="#F59E0B" fontSize={18} fontWeight={700}>
            🔄 FIPS — Settlement Infrastructure
          </text>
          <text x={fips.x + fips.w / 2} y={fips.y + 58} textAnchor="middle" fill="#64748B" fontSize={12}>
            TIPS / SCT Inst clearing &amp; settlement backbone
          </text>
          <text x={fips.x + fips.w / 2} y={fips.y + 76} textAnchor="middle" fill="#64748B" fontSize={11}>
            Routes pacs.008 messages • Settles in &lt;10 seconds • 24/7/365
          </text>

          {/* ─── Vertical connectors: Banks → FIPS ─── */}
          <line
            x1={bankA.x + bankA.w / 2} y1={bankA.y + bankA.h}
            x2={bankA.x + bankA.w / 2} y2={fips.y}
            stroke={currentStep >= 2 ? (currentStep === 2 ? '#0097B9' : '#059669') : '#CBD5E1'}
            strokeWidth={currentStep === 2 ? 3 : 2}
            strokeDasharray={currentStep < 2 ? '4 4' : 'none'}
          />
          <line
            x1={bankB.x + bankB.w / 2} y1={bankB.y + bankB.h}
            x2={bankB.x + bankB.w / 2} y2={fips.y}
            stroke={currentStep >= 3 ? (currentStep === 3 ? '#0097B9' : '#059669') : '#CBD5E1'}
            strokeWidth={currentStep === 3 ? 3 : 2}
            strokeDasharray={currentStep < 3 ? '4 4' : 'none'}
          />

          {/* ─── QR scan dashed line ─── */}
          <line
            x1={220} y1={75} x2={840} y2={75}
            stroke="#0097B9"
            strokeWidth={2}
            strokeDasharray="6 4"
            className={`flow-arrow ${currentStep === 0 ? 'active' : currentStep > 0 ? 'done' : ''}`}
          />
          <text
            x={530} y={62}
            className={`flow-step-text ${currentStep === 0 ? 'active' : currentStep > 0 ? 'done' : ''}`}
          >
            Scans QR Code
          </text>

          {/* ─── Arrow: Client → Bank A ─── */}
          <path
            d={arrowPath(clientNode.x + clientNode.w / 2, clientNode.y + clientNode.h, bankA.x + bankA.w / 2, bankA.y, -10)}
            className={`flow-arrow ${currentStep === 1 ? 'active' : currentStep > 1 ? 'done' : ''}`}
            stroke={currentStep === 1 ? '#0097B9' : currentStep > 1 ? '#059669' : '#94A3B8'}
          />
          {currentStep >= 1 && (
            <text x={80} y={170} className={`flow-step-text ${currentStep === 1 ? 'active' : 'done'}`}>
              POST /bank/pay
            </text>
          )}

          {/* ─── Arrow: Bank A ↓ into FIPS ─── */}
          {currentStep >= 2 && (
            <text
              x={bankA.x + bankA.w / 2 - 50} y={(bankA.y + bankA.h + fips.y) / 2 + 4}
              className={`flow-step-text ${currentStep === 2 ? 'active' : 'done'}`}
              fontSize={11}
            >
              pacs.008 ↓
            </text>
          )}

          {/* ─── Horizontal arrow inside FIPS ─── */}
          {currentStep >= 3 && (
            <>
              <line
                x1={bankA.x + bankA.w / 2 + 20} y1={fips.y + fips.h / 2 - 10}
                x2={bankB.x + bankB.w / 2 - 20} y2={fips.y + fips.h / 2 - 10}
                stroke={currentStep === 3 ? '#F59E0B' : '#059669'}
                strokeWidth={3}
                markerEnd="url(#fipsArrow)"
                className={`flow-arrow ${currentStep === 3 ? 'active' : 'done'}`}
              />
              <text
                x={fips.x + fips.w / 2} y={fips.y + fips.h / 2 - 18}
                textAnchor="middle"
                className={`flow-step-text ${currentStep === 3 ? 'active' : 'done'}`}
                fontSize={11}
              >
                Route &amp; settle →
              </text>
            </>
          )}

          {/* ─── Label: FIPS ↑ to Bank B ─── */}
          {currentStep >= 3 && (
            <text
              x={bankB.x + bankB.w / 2 + 10} y={(bankB.y + bankB.h + fips.y) / 2 + 4}
              className={`flow-step-text ${currentStep === 3 ? 'active' : 'done'}`}
              fontSize={11}
            >
              ↑ pacs.002
            </text>
          )}

          {/* ─── Arrow: Bank B → Retailer ─── */}
          <path
            d={arrowPath(bankB.x + bankB.w / 2, bankB.y, retailerNode.x + retailerNode.w / 2, retailerNode.y + retailerNode.h, 10)}
            className={`flow-arrow ${currentStep === 4 ? 'active' : currentStep === 5 ? 'done' : ''}`}
            stroke={currentStep >= 4 ? (currentStep === 4 ? '#0097B9' : '#059669') : '#94A3B8'}
          />
          {currentStep >= 4 && (
            <text x={880} y={170} className={`flow-step-text ${currentStep === 4 ? 'active' : 'done'}`}>
              Credit account
            </text>
          )}

          {/* ─── Confirmation label ─── */}
          {currentStep === 5 && (
            <text x={retailerNode.x + retailerNode.w / 2} y={retailerNode.y + retailerNode.h + 20} textAnchor="middle" className="flow-step-text active" fontSize={12}>
              ✓ Settlement confirmed
            </text>
          )}

          {/* ─── Arrow marker ─── */}
          <defs>
            <marker id="fipsArrow" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <path d="M0,0 L8,3 L0,6" fill="#F59E0B" />
            </marker>
          </defs>

          {/* ─── Client node ─── */}
          <g className={`flow-node ${isHl('client') ? 'highlight' : ''}`} transform={`translate(${clientNode.x}, ${clientNode.y})`}>
            <rect width={clientNode.w} height={clientNode.h} fill="#2563EB" opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>📱</text>
            <text className="node-label" x={56} y={38}>Client App</text>
          </g>

          {/* ─── Retailer node ─── */}
          <g className={`flow-node ${isHl('retailer') ? 'highlight' : ''}`} transform={`translate(${retailerNode.x}, ${retailerNode.y})`}>
            <rect width={retailerNode.w} height={retailerNode.h} fill="#059669" opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>🏪</text>
            <text className="node-label" x={56} y={38}>Retailer POS</text>
          </g>

          {/* ─── Bank A node ─── */}
          <g className={`flow-node ${isHl('bankA') ? 'highlight' : ''}`} transform={`translate(${bankA.x}, ${bankA.y})`}>
            <rect width={bankA.w} height={bankA.h} fill="#6366F1" opacity={0.85} rx={14} />
            <text className="node-icon" x={16} y={40}>🏦</text>
            <text className="node-label" x={52} y={34} fontSize={14}>Bank A</text>
            <text x={52} y={54} fill="#4338CA" fontSize={11}>Payer PSP</text>
            <text x={52} y={72} fill="#64748B" fontSize={10}>Alice — DE89…013001</text>
          </g>

          {/* ─── Bank B node ─── */}
          <g className={`flow-node ${isHl('bankB') ? 'highlight' : ''}`} transform={`translate(${bankB.x}, ${bankB.y})`}>
            <rect width={bankB.w} height={bankB.h} fill="#818CF8" opacity={0.85} rx={14} />
            <text className="node-icon" x={16} y={40}>🏦</text>
            <text className="node-label" x={52} y={34} fontSize={14}>Bank B</text>
            <text x={52} y={54} fill="#4338CA" fontSize={11}>Payee PSP</text>
            <text x={52} y={72} fill="#64748B" fontSize={10}>Retail Store — DE89…013099</text>
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
