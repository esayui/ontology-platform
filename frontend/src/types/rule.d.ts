export interface RuleDefinition {
  id: string
  ruleName: string
  drlContent: string
  version: string
  status: 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'ARCHIVED'
  tags: string
  description: string
  publishTime?: string
  createTime?: string
  updateTime?: string
}

export interface ViolationResult {
  ruleName: string
  indicatorId: string
  reason: string
  expectedMin: number
  expectedMax: number
  actualValue: number
}
