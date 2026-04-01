import { Flow } from '../types'

interface Props {
  flows: Map<string, Flow>
}

const STEP_ORDER = [
  'PAY_INITIATED',
  'SCA_CONFIRMED',
  'SETTLE_DEBIT',
  'FIPS_SUBMIT',
  'FIPS_RECEIVED',
  'FIPS_VALIDATING',
  'FIPS_FORWARD',
  'RECEIVE_CREDIT',
  'FIPS_SETTLED',
  'SETTLE_COMPLETE',
  'SETTLE_INTRA',
]

export function DataPanel({ flows }: Props) {
  const activeFlows = Array.from(flows.values())
    .sort((a, b) => {
      // Active first, then by most recent step
      if (a.status === 'active' && b.status !== 'active') return -1
      if (b.status === 'active' && a.status !== 'active') return 1
      const aLast = a.steps[a.steps.length - 1]?.timestamp || ''
      const bLast = b.steps[b.steps.length - 1]?.timestamp || ''
      return bLast.localeCompare(aLast)
    })
    .slice(0, 6)

  return (
    <div className="data-panel">
      <div className="data-header">Active Flows</div>
      {activeFlows.length === 0 && (
        <div className="data-empty">No active payment flows</div>
      )}
      {activeFlows.map(flow => {
        const completedSteps = new Set(flow.steps.map(s => s.step))
        const statusClass = flow.status === 'complete' ? 'status-acsc'
          : flow.status === 'rejected' ? 'status-rjct'
          : 'status-active'

        return (
          <div key={flow.uetr} className="flow-card" style={{ borderLeftColor: flow.color }}>
            <div className="flow-card-header">
              <span className="flow-amount" style={{ color: flow.color }}>
                &euro;{flow.amount.toFixed(2)}
              </span>
              <span className={`flow-status ${statusClass}`}>
                {flow.status === 'complete' ? 'SETTLED' : flow.status === 'rejected' ? 'REJECTED' : 'ACTIVE'}
              </span>
            </div>
            <div className="flow-names">
              {flow.debtorName} &rarr; {flow.creditorName}
            </div>
            <div className="flow-uetr">{flow.uetr.slice(0, 8)}...</div>
            <div className="flow-steps">
              {STEP_ORDER.filter(s => completedSteps.has(s)).map(step => (
                <span key={step} className="flow-step-dot" style={{ background: flow.color }} title={step} />
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}
