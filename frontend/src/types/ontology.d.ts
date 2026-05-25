export interface CapabilityIndicator {
  id: string
  iri: string
  name: string
  domain: string
  category: string
  description: string
  unit: string
  dataType: string
  thresholdMin: number
  thresholdMax: number
  createTime?: string
  updateTime?: string
}

export interface CapabilityRelationship {
  id: string
  sourceIndicatorId: string
  targetIndicatorId: string
  relationshipType: string
  droolsRuleName: string
  weight: number
  priority: number
  influenceDirection: string
  enabled: boolean
}
