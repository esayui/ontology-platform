import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ruleApi } from '@/api/rule'
import type { RuleDefinition, ViolationResult } from '@/types/rule'

export const useRuleStore = defineStore('rule', () => {
  const rules = ref<RuleDefinition[]>([])
  const currentRule = ref<RuleDefinition | null>(null)
  const loading = ref(false)
  const testResults = ref<ViolationResult[]>([])

  async function fetchRules(page = 1, size = 20) {
    loading.value = true
    try {
      const res = await ruleApi.listRules(page, size)
      rules.value = res.data.records || []
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchRule(id: string) {
    const res = await ruleApi.getRule(id)
    currentRule.value = res.data
    return res.data
  }

  async function executeRules(facts: Record<string, any>) {
    const res = await ruleApi.executeRules(facts)
    testResults.value = res.data || []
    return res.data
  }

  return { rules, currentRule, loading, testResults, fetchRules, fetchRule, executeRules }
})
