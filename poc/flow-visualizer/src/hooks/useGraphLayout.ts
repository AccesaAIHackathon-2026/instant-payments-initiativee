import { useReducer, useEffect, useRef } from 'react'
import { FlowEvent, FlowState, NodeState, EdgeAnimation, Flow } from '../types'
import { getFlowColor } from '../utils/colors'
import { computeLayout } from '../utils/layout'

const EDGE_LIFETIME_MS = 4000

function nodeTypeFromString(s: string): NodeState['type'] {
  if (s === 'mobile') return 'mobile'
  if (s === 'fips') return 'fips'
  if (s === 'retail') return 'retail'
  return 'bank'
}

function labelFromId(id: string): string {
  if (id.startsWith('mobile-')) return id.slice(7)
  if (id.startsWith('retail-')) return id.slice(7)
  if (id === 'fips') return 'FIPS'
  // bank-a → Bank A
  if (id.startsWith('bank-')) {
    const suffix = id.slice(5).toUpperCase()
    return `Bank ${suffix}`
  }
  return id
}

type Action =
  | { type: 'ADD_EVENT'; event: FlowEvent }
  | { type: 'PRUNE_EDGES' }

function reducer(state: FlowState, action: Action): FlowState {
  switch (action.type) {
    case 'ADD_EVENT': {
      const event = action.event
      const now = Date.now()

      // Ensure source and target nodes exist
      const nodes = new Map(state.nodes)
      if (event.source && !nodes.has(event.source)) {
        nodes.set(event.source, {
          id: event.source,
          type: nodeTypeFromString(event.sourceType),
          label: labelFromId(event.source),
          lastSeen: now,
          x: 0,
          y: 0,
        })
      }
      if (event.target && !nodes.has(event.target)) {
        nodes.set(event.target, {
          id: event.target,
          type: nodeTypeFromString(event.targetType),
          label: labelFromId(event.target),
          lastSeen: now,
          x: 0,
          y: 0,
        })
      }

      // Update lastSeen
      if (event.source && nodes.has(event.source)) {
        const n = nodes.get(event.source)!
        nodes.set(event.source, { ...n, lastSeen: now })
      }
      if (event.target && nodes.has(event.target)) {
        const n = nodes.get(event.target)!
        nodes.set(event.target, { ...n, lastSeen: now })
      }

      // Recompute layout
      const laid = computeLayout(nodes)

      // Create edge animation (skip self-referencing edges like FIPS_VALIDATING)
      const edges = [...state.edges]
      if (event.source && event.target && event.source !== event.target) {
        const color = event.uetr ? getFlowColor(event.uetr) : '#6B7280'
        const label = event.amount ? `€${event.amount}` : ''
        edges.push({
          id: event.id,
          fromNode: event.source,
          toNode: event.target,
          color,
          label,
          uetr: event.uetr || '',
          createdAt: now,
          step: event.step,
        })
      }

      // Track flow per UETR
      const flows = new Map(state.flows)
      if (event.uetr) {
        const existing = flows.get(event.uetr)
        if (existing) {
          const steps = [...existing.steps, event]
          const status = event.status === 'ACSC' ? 'complete' as const
            : event.status === 'RJCT' ? 'rejected' as const
            : existing.status
          flows.set(event.uetr, { ...existing, steps, status })
        } else {
          flows.set(event.uetr, {
            uetr: event.uetr,
            color: getFlowColor(event.uetr),
            debtorName: event.debtorName || 'Unknown',
            creditorName: event.creditorName || 'Unknown',
            amount: event.amount || 0,
            steps: [event],
            status: 'active',
          })
        }
      }

      return {
        events: [event, ...state.events].slice(0, 500),
        nodes: laid,
        flows,
        edges,
      }
    }
    case 'PRUNE_EDGES': {
      const now = Date.now()
      return {
        ...state,
        edges: state.edges.filter(e => now - e.createdAt < EDGE_LIFETIME_MS),
      }
    }
    default:
      return state
  }
}

const initialState: FlowState = {
  events: [],
  nodes: new Map(),
  flows: new Map(),
  edges: [],
}

export function useGraphLayout(events: FlowEvent[]) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const processedRef = useRef(0)

  // Process new events
  useEffect(() => {
    // Events come newest-first, process only new ones
    const newCount = events.length - processedRef.current
    if (newCount <= 0) return

    // Process new events from oldest to newest
    const newEvents = events.slice(0, newCount).reverse()
    for (const event of newEvents) {
      dispatch({ type: 'ADD_EVENT', event })
    }
    processedRef.current = events.length
  }, [events])

  // Prune old edges periodically
  useEffect(() => {
    const interval = setInterval(() => {
      dispatch({ type: 'PRUNE_EDGES' })
    }, 500)
    return () => clearInterval(interval)
  }, [])

  return state
}
