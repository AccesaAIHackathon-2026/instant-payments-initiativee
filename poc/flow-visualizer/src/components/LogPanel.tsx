import { useEffect, useRef, useState } from 'react'
import { FlowEvent } from '../types'
import { getFlowColor } from '../utils/colors'

interface Props {
  events: FlowEvent[]
}

export function LogPanel({ events }: Props) {
  const listRef = useRef<HTMLDivElement>(null)
  const [autoScroll, setAutoScroll] = useState(true)

  useEffect(() => {
    if (autoScroll && listRef.current) {
      listRef.current.scrollTop = 0
    }
  }, [events, autoScroll])

  const handleScroll = () => {
    if (listRef.current) {
      setAutoScroll(listRef.current.scrollTop < 10)
    }
  }

  return (
    <div className="log-panel">
      <div className="log-header">
        <span className="log-title">Event Log</span>
        <span className="log-count">{events.length} events</span>
      </div>
      <div className="log-list" ref={listRef} onScroll={handleScroll}>
        {events.map(event => {
          const color = event.uetr ? getFlowColor(event.uetr) : '#6B7280'
          const d = new Date(event.timestamp)
          const time = d.toLocaleTimeString('en-GB', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
          }) + '.' + String(d.getMilliseconds()).padStart(3, '0')

          return (
            <div key={event.id} className="log-entry" style={{ borderLeftColor: color }}>
              <span className="log-time">{time}</span>
              <span className="log-step" style={{ color }}>[{event.step}]</span>
              <span className="log-detail">{event.detail || `${event.source} → ${event.target}`}</span>
              {event.uetr && (
                <span className="log-uetr" title={event.uetr}>
                  {event.uetr.slice(0, 8)}...
                </span>
              )}
            </div>
          )
        })}
        {events.length === 0 && (
          <div className="log-empty">No events yet</div>
        )}
      </div>
    </div>
  )
}
