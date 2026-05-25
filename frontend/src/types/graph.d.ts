export interface GraphNode {
  id: string
  iri: string
  name: string
  domain: string
  category: string
  unit: string
  labels: string[]
  properties: Record<string, any>
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  type: string
  ruleName: string
  weight: number
  enabled: boolean
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export type LayoutType = 'force' | 'dag' | 'tree' | 'circle' | 'grid'
