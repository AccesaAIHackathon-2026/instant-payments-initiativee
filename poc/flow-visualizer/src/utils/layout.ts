import { NodeState } from '../types'

/**
 * Tiered layout: mobile | banks | fips | banks | retail
 * Nodes within a tier are spaced vertically.
 */

const TIER_X: Record<string, number> = {
  mobile: 80,
  'bank-left': 320,
  fips: 560,
  'bank-right': 800,
  retail: 1040,
}

const START_Y = 120
const SPACING_Y = 140

type Tier = 'mobile' | 'bank-left' | 'fips' | 'bank-right' | 'retail'

export function computeLayout(nodes: Map<string, NodeState>): Map<string, NodeState> {
  // Group nodes by tier
  const tiers = new Map<Tier, NodeState[]>()

  // Track which bank IDs appear as sources (left) vs targets (right)
  // For simplicity: if a bank node is the source in PAY_INITIATED context, it's left
  // We'll use a heuristic: banks that mobile nodes connect to are on the left
  const allNodes = Array.from(nodes.values())

  for (const node of allNodes) {
    let tier: Tier
    if (node.type === 'mobile') tier = 'mobile'
    else if (node.type === 'fips') tier = 'fips'
    else if (node.type === 'retail') tier = 'retail'
    else if (node.type === 'bank') {
      // Place bank on left by default; we'll adjust if needed
      tier = 'bank-left'
    } else {
      tier = 'bank-left'
    }

    const list = tiers.get(tier) || []
    list.push(node)
    tiers.set(tier, list)
  }

  // If we have 2+ banks, put the second one on the right
  const bankNodes = tiers.get('bank-left') || []
  if (bankNodes.length > 1) {
    const rightBanks = bankNodes.slice(1)
    tiers.set('bank-left', [bankNodes[0]])
    tiers.set('bank-right', rightBanks)
  }

  // Assign positions
  const updated = new Map<string, NodeState>()

  for (const [tier, tierNodes] of tiers) {
    const x = TIER_X[tier]
    const totalHeight = (tierNodes.length - 1) * SPACING_Y
    const startY = START_Y + Math.max(0, (300 - totalHeight) / 2)

    tierNodes.forEach((node, i) => {
      updated.set(node.id, {
        ...node,
        x,
        y: startY + i * SPACING_Y,
      })
    })
  }

  return updated
}
