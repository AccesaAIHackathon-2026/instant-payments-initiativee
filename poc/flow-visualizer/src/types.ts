export interface FlowEvent {
  id: string
  timestamp: string
  uetr: string | null
  step: string
  source: string
  target: string
  sourceType: string
  targetType: string
  actor: string | null
  debtorName: string | null
  debtorIban: string | null
  creditorName: string | null
  creditorIban: string | null
  amount: number | null
  currency: string | null
  status: string | null
  detail: string | null
}

export interface NodeState {
  id: string
  type: 'mobile' | 'bank' | 'fips' | 'retail'
  label: string
  lastSeen: number
  x: number
  y: number
}

export interface Flow {
  uetr: string
  color: string
  debtorName: string
  creditorName: string
  amount: number
  steps: FlowEvent[]
  status: 'active' | 'complete' | 'rejected'
}

export interface EdgeAnimation {
  id: string
  fromNode: string
  toNode: string
  color: string
  label: string
  uetr: string
  createdAt: number
  step: string
}

export interface FlowState {
  events: FlowEvent[]
  nodes: Map<string, NodeState>
  flows: Map<string, Flow>
  edges: EdgeAnimation[]
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'
