import { useFlowEventStream } from './hooks/useFlowEventStream'
import { useGraphLayout } from './hooks/useGraphLayout'
import { Header } from './components/Header'
import { FlowDiagram } from './components/FlowDiagram'
import { LogPanel } from './components/LogPanel'
import { DataPanel } from './components/DataPanel'
import { Legend } from './components/Legend'

export default function App() {
  const { events, connections, clearEvents } = useFlowEventStream()
  const state = useGraphLayout(events)

  return (
    <div className="app">
      <Header connections={connections} onClear={clearEvents} />
      <div className="main-content">
        <div className="left-panel">
          <Legend flows={state.flows} />
          <FlowDiagram state={state} />
          <DataPanel flows={state.flows} />
        </div>
        <LogPanel events={state.events} />
      </div>
    </div>
  )
}
