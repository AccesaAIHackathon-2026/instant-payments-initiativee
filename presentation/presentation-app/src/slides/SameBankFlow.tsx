import { useFlowAnimation } from './useFlowAnimation'
import { FlowControls } from './FlowControls'

const steps = [
  'Customer scans QR code',
  'Payment request sent to Bank',
  'Bank debits customer account',
  'Bank credits retailer account',
  'Retailer receives confirmation',
]

// Layout constants
const clientNode =   { x: 60,  y: 60,  label: 'Client App',   icon: '\u{1F4F1}', color: '#2563EB' }
const retailerNode = { x: 840, y: 60,  label: 'Retailer POS', icon: '\u{1F3EA}', color: '#059669' }
const fipsNode =     { x: 450, y: 420, label: 'FIPS',          icon: '\u{1F504}', color: '#D97706' }

// Bank box dimensions (large, contains two accounts)
const bank = { x: 200, y: 200, w: 660, h: 160 }
// Sub-accounts inside the bank
const custAcct = { x: 230, y: 240, w: 200, h: 80 }
const retAcct  = { x: 610, y: 240, w: 200, h: 80 }

function arrowPath(fx: number, fy: number, tx: number, ty: number, bend = -30): string {
  const cx = (fx + tx) / 2, cy = (fy + ty) / 2 + bend
  return `M${fx},${fy} Q${cx},${cy} ${tx},${ty}`
}

export function SameBankFlow({ active }: { active: boolean }) {
  const { currentStep, playing, finished, play, pause, resume, reset, prevStep, nextStep } = useFlowAnimation(steps.length, active)

  const hlClient   = [0, 1].includes(currentStep)
  const hlRetailer = [0, 4].includes(currentStep)
  const hlBank     = [1, 2, 3, 4].includes(currentStep)
  const hlCustAcct = [2].includes(currentStep)
  const hlRetAcct  = [3].includes(currentStep)
  const hlTransfer = currentStep === 2 || currentStep === 3

  return (
    <div className="slide-flow">
      <h2>Scenario 1 — Same-Bank Payment</h2>
      <p className="flow-subtitle">Customer and retailer share the same bank. No FIPS routing needed.</p>

      <div className="flow-diagram">
        <svg viewBox="0 0 1060 520" xmlns="http://www.w3.org/2000/svg">
          {/* ─── QR scan dashed line ─── */}
          <line
            x1={220} y1={95} x2={840} y2={95}
            stroke="#0097B9"
            strokeWidth={2}
            strokeDasharray="6 4"
            className={`flow-arrow ${currentStep === 0 ? 'active' : currentStep > 0 ? 'done' : ''}`}
          />
          <text
            x={530} y={82}
            className={`flow-step-text ${currentStep === 0 ? 'active' : currentStep > 0 ? 'done' : ''}`}
          >
            Scans QR Code
          </text>

          {/* ─── Arrow: Client → Bank ─── */}
          <path
            d={arrowPath(140, 130, custAcct.x + custAcct.w / 2, custAcct.y)}
            className={`flow-arrow ${currentStep === 1 ? 'active' : currentStep > 1 ? 'done' : ''}`}
            stroke={currentStep === 1 ? '#0097B9' : currentStep > 1 ? '#059669' : '#546E7A'}
          />
          <text
            x={180} y={185}
            className={`flow-step-text ${currentStep === 1 ? 'active' : currentStep > 1 ? 'done' : ''}`}
          >
            POST /bank/pay
          </text>

          {/* ─── Arrow: Internal transfer (customer acct → retailer acct) ─── */}
          <path
            d={arrowPath(custAcct.x + custAcct.w, custAcct.y + custAcct.h / 2, retAcct.x, retAcct.y + retAcct.h / 2, -30)}
            className={`flow-arrow ${hlTransfer ? 'active' : currentStep > 3 ? 'done' : ''}`}
            stroke={hlTransfer ? '#0097B9' : currentStep > 3 ? '#059669' : '#546E7A'}
          />
          <text
            x={(custAcct.x + custAcct.w + retAcct.x) / 2}
            y={custAcct.y + 20}
            className={`flow-step-text ${hlTransfer ? 'active' : currentStep > 3 ? 'done' : ''}`}
          >
            {currentStep === 2 ? '−€25.00' : currentStep === 3 ? '+€25.00' : 'Internal transfer'}
          </text>

          {/* ─── Arrow: Bank → Retailer ─── */}
          <path
            d={arrowPath(retAcct.x + retAcct.w / 2, retAcct.y, 920, 130, -30)}
            className={`flow-arrow ${currentStep === 4 ? 'active' : ''}`}
            stroke={currentStep === 4 ? '#059669' : '#546E7A'}
          />
          <text
            x={870} y={185}
            className={`flow-step-text ${currentStep === 4 ? 'active' : ''}`}
          >
            Confirmed
          </text>

          {/* ─── Bank outer box ─── */}
          <rect
            x={bank.x} y={bank.y} width={bank.w} height={bank.h} rx={16}
            fill="#6366F1"
            opacity={hlBank ? 0.18 : 0.08}
            stroke={hlBank ? '#6366F1' : '#546E7A'}
            strokeWidth={hlBank ? 2 : 1}
          />
          <text x={bank.x + 16} y={bank.y + 24} fill="#C7D2FE" fontSize={15} fontWeight={600}>
            🏦 Bank
          </text>

          {/* ─── Customer Account sub-box ─── */}
          <rect
            x={custAcct.x} y={custAcct.y} width={custAcct.w} height={custAcct.h} rx={10}
            fill={hlCustAcct ? '#2563EB' : '#1E293B'}
            opacity={hlCustAcct ? 0.9 : 0.7}
            stroke={hlCustAcct ? '#60A5FA' : '#475569'}
            strokeWidth={hlCustAcct ? 2 : 1}
          />
          <text x={custAcct.x + 14} y={custAcct.y + 28} fill="#E0E7FF" fontSize={13} fontWeight={600}>
            👤 Alice — Customer
          </text>
          <text x={custAcct.x + 14} y={custAcct.y + 50} fill="#94A3B8" fontSize={11}>
            DE89…013001
          </text>
          <text x={custAcct.x + 14} y={custAcct.y + 66} fill={currentStep >= 2 ? '#F87171' : '#94A3B8'} fontSize={12} fontWeight={500}>
            {currentStep >= 2 ? '€975.00 (−€25.00)' : '€1,000.00'}
          </text>

          {/* ─── Retailer Account sub-box ─── */}
          <rect
            x={retAcct.x} y={retAcct.y} width={retAcct.w} height={retAcct.h} rx={10}
            fill={hlRetAcct ? '#059669' : '#1E293B'}
            opacity={hlRetAcct ? 0.9 : 0.7}
            stroke={hlRetAcct ? '#34D399' : '#475569'}
            strokeWidth={hlRetAcct ? 2 : 1}
          />
          <text x={retAcct.x + 14} y={retAcct.y + 28} fill="#E0E7FF" fontSize={13} fontWeight={600}>
            🏪 Retail Store GmbH
          </text>
          <text x={retAcct.x + 14} y={retAcct.y + 50} fill="#94A3B8" fontSize={11}>
            DE89…013099
          </text>
          <text x={retAcct.x + 14} y={retAcct.y + 66} fill={currentStep >= 3 ? '#34D399' : '#94A3B8'} fontSize={12} fontWeight={500}>
            {currentStep >= 3 ? '€25.00 (+€25.00)' : '€0.00'}
          </text>

          {/* ─── Client node ─── */}
          <g
            className={`flow-node ${hlClient ? 'highlight' : ''}`}
            transform={`translate(${clientNode.x}, ${clientNode.y})`}
          >
            <rect width={160} height={70} fill={clientNode.color} opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>{clientNode.icon}</text>
            <text className="node-label" x={56} y={38}>{clientNode.label}</text>
          </g>

          {/* ─── Retailer node ─── */}
          <g
            className={`flow-node ${hlRetailer ? 'highlight' : ''}`}
            transform={`translate(${retailerNode.x}, ${retailerNode.y})`}
          >
            <rect width={160} height={70} fill={retailerNode.color} opacity={0.85} rx={12} />
            <text className="node-icon" x={20} y={44}>{retailerNode.icon}</text>
            <text className="node-label" x={56} y={38}>{retailerNode.label}</text>
          </g>

          {/* ─── FIPS node (greyed out) ─── */}
          <g
            className="flow-node inactive"
            transform={`translate(${fipsNode.x}, ${fipsNode.y})`}
          >
            <rect width={160} height={70} fill={fipsNode.color} opacity={0.12} rx={12} />
            <text className="node-icon" x={20} y={44}>{fipsNode.icon}</text>
            <text className="node-label" x={56} y={38}>{fipsNode.label}</text>
            <text className="node-sublabel" x={56} y={54}>Not involved</text>
          </g>
        </svg>
      </div>

      {/* Current action callout */}
      <div className={`flow-action-box ${currentStep >= 0 && currentStep < steps.length ? 'visible' : ''}`}>
        <span className="flow-action-step">Step {currentStep + 1} of {steps.length}</span>
        <span className="flow-action-label">{steps[Math.max(0, Math.min(currentStep, steps.length - 1))]}</span>
      </div>

      {/* Step progress */}
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
