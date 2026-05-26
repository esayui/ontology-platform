export interface KillChainPath {
  id: string
  nodes: PathNode[]
  edges: PathEdge[]
  satisfactionRate: number    // 0-100
  riskLevel: 'low' | 'medium' | 'high'
}

export interface PathNode {
  id: string
  name: string
  type: string
  satisfied: boolean
  risk: boolean
  gapIndicators: string[]
}

export interface PathEdge {
  id: string
  source: string
  target: string
  type: string
}

export interface CapLayer {
  level: 'capability' | 'platform' | 'equipment'
  nodes: NetNode[]
}

export interface NetNode {
  id: string
  name: string
  level: 'capability' | 'platform' | 'equipment'
  category: string
  satisfied: boolean
  score: number
  linkedIds: string[]
}

export interface NetEdge {
  id: string
  source: string
  target: string
  type: string
}

export interface SatisfactionMatrix {
  platforms: string[]
  indicators: string[]
  matrix: Record<string, Record<string, { satisfied: boolean; score: number }>>
}

export interface AnalysisData {
  chains: KillChainPath[]
  netNodes: NetNode[]
  netEdges: NetEdge[]
  matrix: SatisfactionMatrix
}
