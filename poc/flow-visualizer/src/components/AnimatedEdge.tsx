import { EdgeAnimation, NodeState } from '../types'
import { NODE_DIMENSIONS } from './NodeBox'

interface Props {
  edge: EdgeAnimation
  nodes: Map<string, NodeState>
}

const ANIMATION_DURATION_MS = 800
const FADE_DURATION_MS = 4000

export function AnimatedEdge({ edge, nodes }: Props) {
  const from = nodes.get(edge.fromNode)
  const to = nodes.get(edge.toNode)
  if (!from || !to) return null

  const age = Date.now() - edge.createdAt
  const opacity = age < ANIMATION_DURATION_MS ? 1 : Math.max(0, 1 - (age - ANIMATION_DURATION_MS) / (FADE_DURATION_MS - ANIMATION_DURATION_MS))

  // Compute edge endpoints at node borders
  const dx = to.x - from.x
  const dy = to.y - from.y
  const angle = Math.atan2(dy, dx)

  const hw = NODE_DIMENSIONS.width / 2
  const hh = NODE_DIMENSIONS.height / 2

  const x1 = from.x + Math.cos(angle) * hw
  const y1 = from.y + Math.sin(angle) * hh
  const x2 = to.x - Math.cos(angle) * hw
  const y2 = to.y - Math.sin(angle) * hh

  const pathId = `path-${edge.id}`
  const midX = (x1 + x2) / 2
  const midY = (y1 + y2) / 2

  return (
    <g style={{ opacity, transition: 'opacity 0.3s' }}>
      {/* Static trail line */}
      <line
        x1={x1} y1={y1} x2={x2} y2={y2}
        stroke={edge.color}
        strokeWidth={2}
        strokeOpacity={0.3}
        strokeDasharray="6 4"
      />

      {/* Animated path for the pulse */}
      <path
        id={pathId}
        d={`M ${x1} ${y1} L ${x2} ${y2}`}
        fill="none"
        stroke="none"
      />

      {/* Traveling pulse dot */}
      {age < ANIMATION_DURATION_MS && (
        <circle r={6} fill={edge.color} filter="url(#glow)">
          <animateMotion
            dur={`${ANIMATION_DURATION_MS}ms`}
            fill="freeze"
            path={`M ${x1} ${y1} L ${x2} ${y2}`}
          />
        </circle>
      )}

      {/* Solid arrow line after pulse completes */}
      {age >= ANIMATION_DURATION_MS && (
        <line
          x1={x1} y1={y1} x2={x2} y2={y2}
          stroke={edge.color}
          strokeWidth={2.5}
          strokeOpacity={opacity * 0.6}
          markerEnd="url(#arrowhead)"
        />
      )}

      {/* Label */}
      {edge.label && (
        <text
          x={midX}
          y={midY - 10}
          textAnchor="middle"
          className="edge-label"
          fill={edge.color}
          style={{ opacity }}
        >
          {edge.label}
        </text>
      )}

      {/* Step label */}
      <text
        x={midX}
        y={midY + 14}
        textAnchor="middle"
        className="edge-step"
        fill={edge.color}
        style={{ opacity: opacity * 0.7 }}
      >
        {formatStep(edge.step)}
      </text>
    </g>
  )
}

function formatStep(step: string): string {
  return step.replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase())
}
