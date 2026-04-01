import { FlowState } from '../types'
import { NodeBox } from './NodeBox'
import { AnimatedEdge } from './AnimatedEdge'

interface Props {
  state: FlowState
}

export function FlowDiagram({ state }: Props) {
  const nodesArray = Array.from(state.nodes.values())

  return (
    <div className="diagram-container">
      {nodesArray.length === 0 && (
        <div className="diagram-empty">
          <p className="diagram-empty-icon">&#9889;</p>
          <p>Waiting for payment events...</p>
          <p className="diagram-empty-hint">Make a payment from the mobile app or retailer POS to see the flow</p>
        </div>
      )}
      <svg className="diagram-svg" viewBox="0 0 1150 600" preserveAspectRatio="xMidYMid meet">
        <defs>
          {/* Glow filter for animated dots */}
          <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          {/* Arrowhead marker */}
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="7"
            refX="10"
            refY="3.5"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#9CA3AF" />
          </marker>
        </defs>

        {/* Edges */}
        {state.edges.map(edge => (
          <AnimatedEdge key={edge.id} edge={edge} nodes={state.nodes} />
        ))}

        {/* Nodes */}
        {nodesArray.map(node => (
          <NodeBox key={node.id} node={node} />
        ))}
      </svg>
    </div>
  )
}
