import { Flow } from '../types'

interface Props {
  flows: Map<string, Flow>
}

export function Legend({ flows }: Props) {
  const active = Array.from(flows.values()).filter(f => f.status === 'active')
  if (active.length === 0) return null

  return (
    <div className="legend">
      {active.map(flow => (
        <span key={flow.uetr} className="legend-item">
          <span className="legend-dot" style={{ background: flow.color }} />
          <span className="legend-label">{flow.debtorName}</span>
        </span>
      ))}
    </div>
  )
}
