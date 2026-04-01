const FLOW_COLORS = [
  '#2563EB', // blue
  '#DC2626', // red
  '#059669', // emerald
  '#D97706', // amber
  '#7C3AED', // violet
  '#DB2777', // pink
  '#0891B2', // cyan
  '#65A30D', // lime
]

let colorIndex = 0
const uetrColorMap = new Map<string, string>()

export function getFlowColor(uetr: string): string {
  let color = uetrColorMap.get(uetr)
  if (!color) {
    color = FLOW_COLORS[colorIndex % FLOW_COLORS.length]
    colorIndex++
    uetrColorMap.set(uetr, color)
  }
  return color
}

export function resetColors(): void {
  colorIndex = 0
  uetrColorMap.clear()
}
