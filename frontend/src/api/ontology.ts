import request from './request'
import type { CapabilityIndicator, CapabilityRelationship } from '@/types/ontology'

export const ontologyApi = {
  getIndicators() {
    return request.get<any, { data: CapabilityIndicator[] }>('/ontology/indicators')
  },

  getIndicator(id: string) {
    return request.get<any, { data: CapabilityIndicator }>(`/ontology/indicators/${id}`)
  },

  getRelationships() {
    return request.get<any, { data: CapabilityRelationship[] }>('/ontology/relationships')
  },

  getRelationshipsByIndicator(indicatorId: string) {
    return request.get<any, { data: CapabilityRelationship[] }>(
      `/ontology/relationships/indicator/${indicatorId}`)
  },

  saveRelationship(data: CapabilityRelationship) {
    return request.post('/ontology/relationship', data)
  },

  updateRelationship(data: CapabilityRelationship) {
    return request.put('/ontology/relationship', data)
  },

  deleteRelationship(id: string) {
    return request.delete(`/ontology/relationship/${id}`)
  },

  syncToNeo4j() {
    return request.post('/ontology/sync')
  },
}
