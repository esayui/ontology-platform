export interface KillChainTask {
  id: string
  name: string
  description: string
  modelData: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface KillChainNode {
  id: string
  label: string
  type: NodeType
  platform: string
  position: { x: number; y: number }
  properties: Record<string, any>
}

export interface KillChainEdge {
  id: string
  source: string
  target: string
  label: string
  type: EdgeType
  properties: Record<string, any>
}

export type NodeType =
  | 'reconnaissance' | 'detection' | 'command'
  | 'control' | 'communication' | 'strike' | 'assessment' | 'target'

export type EdgeType = 'communication' | 'command' | 'support' | 'feedback' | 'detection' | 'strike'

export interface KillChainModel {
  nodes: KillChainNode[]
  edges: KillChainEdge[]
  metadata: {
    name: string
    version: string
    createdAt: string
    updatedAt: string
  }
}

export const NODE_TYPES: { value: NodeType; label: string; color: string; shape: string; icon: string }[] = [
  { value: 'reconnaissance', label: '侦察节点', color: '#1890ff', shape: 'round-rectangle', icon: 'eye' },
  { value: 'detection', label: '探测节点', color: '#faad14', shape: 'diamond', icon: 'radar-chart' },
  { value: 'command', label: '指挥节点', color: '#722ed1', shape: 'rectangle', icon: 'deployment-unit' },
  { value: 'control', label: '控制节点', color: '#eb2f96', shape: 'diamond', icon: 'control' },
  { value: 'communication', label: '通信节点', color: '#13c2c2', shape: 'ellipse', icon: 'wifi' },
  { value: 'strike', label: '打击节点', color: '#f5222d', shape: 'triangle', icon: 'thunderbolt' },
  { value: 'assessment', label: '评估节点', color: '#52c41a', shape: 'hexagon', icon: 'check-circle' },
  { value: 'target', label: '目标节点', color: '#fa541c', shape: 'star', icon: 'aim' },
]

export const EDGE_TYPES: { value: EdgeType; label: string; color: string; style: string }[] = [
  { value: 'communication', label: '通信', color: '#1890ff', style: 'solid' },
  { value: 'command', label: '指挥', color: '#722ed1', style: 'solid' },
  { value: 'support', label: '支援', color: '#52c41a', style: 'dashed' },
  { value: 'feedback', label: '反馈', color: '#faad14', style: 'dotted' },
  { value: 'detection', label: '探测', color: '#faad14', style: 'solid' },
  { value: 'strike', label: '打击', color: '#f5222d', style: 'solid' },
]
