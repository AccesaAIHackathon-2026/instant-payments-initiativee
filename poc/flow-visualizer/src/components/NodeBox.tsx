import { NodeState } from '../types'

interface Props {
  node: NodeState
}

const NODE_ICONS: Record<NodeState['type'], string> = {
  mobile: '\uD83D\uDCF1',
  bank: '\uD83C\uDFE6',
  fips: '\u26A1',
  retail: '\uD83D\uDED2',
}

const NODE_WIDTH = 140
const NODE_HEIGHT = 70

export const NODE_DIMENSIONS = { width: NODE_WIDTH, height: NODE_HEIGHT }

export function NodeBox({ node }: Props) {
  const age = Date.now() - node.lastSeen
  const opacity = age > 60000 ? 0.4 : 1

  return (
    <g
      transform={`translate(${node.x}, ${node.y})`}
      style={{ opacity, transition: 'opacity 0.5s' }}
      className="node-group"
    >
      <rect
        x={-NODE_WIDTH / 2}
        y={-NODE_HEIGHT / 2}
        width={NODE_WIDTH}
        height={NODE_HEIGHT}
        rx={node.type === 'fips' ? 16 : 8}
        className={`node-rect node-${node.type}`}
      />
      <text
        y={-8}
        textAnchor="middle"
        className="node-icon"
        fontSize={20}
      >
        {NODE_ICONS[node.type]}
      </text>
      <text
        y={18}
        textAnchor="middle"
        className="node-label"
      >
        {node.label}
      </text>
    </g>
  )
}
