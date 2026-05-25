import request from './request'
import type { RuleDefinition } from '@/types/rule'

export const ruleApi = {
  listRules(page = 1, size = 20, keyword?: string, status?: string, sortBy?: string, sortOrder?: string) {
    return request.get<any, { data: any }>('/rules', {
      params: { page, size, keyword, status, sortBy, sortOrder },
    })
  },

  getRule(id: string) {
    return request.get<any, { data: RuleDefinition }>(`/rules/${id}`)
  },

  getActiveRules() {
    return request.get<any, { data: RuleDefinition[] }>('/rules/active')
  },

  createRule(data: Partial<RuleDefinition>) {
    return request.post('/rules', data)
  },

  updateRule(data: RuleDefinition) {
    return request.put('/rules', data)
  },

  publishRule(id: string) {
    return request.post(`/rules/${id}/publish`)
  },

  disableRule(id: string) {
    return request.post(`/rules/${id}/disable`)
  },

  deleteRule(id: string) {
    return request.delete(`/rules/${id}`)
  },

  getVersions(ruleName: string) {
    return request.get<any, { data: RuleDefinition[] }>(`/rules/${ruleName}/versions`)
  },

  rollback(ruleName: string, versionId: string) {
    return request.post(`/rules/${ruleName}/rollback/${versionId}`)
  },

  executeRules(facts: Record<string, any>) {
    return request.post<any, { data: any[] }>('/rules/execute', facts)
  },
}
