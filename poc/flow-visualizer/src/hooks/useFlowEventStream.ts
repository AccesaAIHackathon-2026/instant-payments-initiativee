import { useEffect, useRef, useState, useCallback } from 'react'
import { FlowEvent, ConnectionStatus } from '../types'

const BANK_A_URL = import.meta.env.VITE_BANK_A_URL || 'http://localhost:8080'
const BANK_B_URL = import.meta.env.VITE_BANK_B_URL || 'http://localhost:8082'
const FIPS_URL = import.meta.env.VITE_FIPS_URL || 'http://localhost:8081'

const SSE_ENDPOINTS = [
  { name: 'bank-a', url: `${BANK_A_URL}/bank/flow-events` },
  { name: 'bank-b', url: `${BANK_B_URL}/bank/flow-events` },
  { name: 'fips', url: `${FIPS_URL}/fips/flow-events` },
]

const MAX_EVENTS = 500

export function useFlowEventStream() {
  const [events, setEvents] = useState<FlowEvent[]>([])
  const [connections, setConnections] = useState<Record<string, ConnectionStatus>>({
    'bank-a': 'connecting',
    'bank-b': 'connecting',
    fips: 'connecting',
  })
  const sourcesRef = useRef<EventSource[]>([])

  const clearEvents = useCallback(() => setEvents([]), [])

  useEffect(() => {
    const sources: EventSource[] = []

    for (const endpoint of SSE_ENDPOINTS) {
      const source = new EventSource(endpoint.url)

      source.addEventListener('flow', (e: MessageEvent) => {
        try {
          const event: FlowEvent = JSON.parse(e.data)
          setEvents(prev => {
            const next = [event, ...prev]
            return next.length > MAX_EVENTS ? next.slice(0, MAX_EVENTS) : next
          })
        } catch {
          // ignore malformed events
        }
      })

      source.onopen = () => {
        setConnections(prev => ({ ...prev, [endpoint.name]: 'connected' }))
      }

      source.onerror = () => {
        setConnections(prev => ({
          ...prev,
          [endpoint.name]: source.readyState === EventSource.CONNECTING ? 'connecting' : 'disconnected',
        }))
      }

      sources.push(source)
    }

    sourcesRef.current = sources

    return () => {
      sources.forEach(s => s.close())
    }
  }, [])

  return { events, connections, clearEvents }
}
