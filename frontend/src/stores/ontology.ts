import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ontologyApi } from '@/api/ontology'
import type { CapabilityIndicator, CapabilityRelationship } from '@/types/ontology'

const DOMAIN_NAMES: Record<string, string> = {
  SurfaceWarfare: '水面作战域',
  UnderwaterWarfare: '水下作战域',
  LandWarfare: '陆上作战域',
  LowAltitudeWarfare: '低空作战域',
  HighAltitudeWarfare: '高空作战域',
}

const CATEGORY_NAMES: Record<string, string> = {
  Ship: '舰艇',
  Radar: '雷达系统',
  WeaponSystem: '武器系统',
  Submarine: '潜艇',
  UnderwaterDetection: '水下探测',
  Tank: '坦克',
  Artillery: '火炮系统',
  Helicopter: '直升机',
  UAV: '无人机',
  Fighter: '歼击机',
  Bomber: '轰炸机',
  AWACS: '预警机',
}

export const useOntologyStore = defineStore('ontology', () => {
  const indicators = ref<CapabilityIndicator[]>([])
  const relationships = ref<CapabilityRelationship[]>([])
  const loading = ref(false)
  const treeData = ref<any[]>([])
  const expandedKeys = ref<string[]>([])

  async function fetchIndicators() {
    loading.value = true
    try {
      const res = await ontologyApi.getIndicators()
      indicators.value = res.data
      buildTreeData()
    } finally {
      loading.value = false
    }
  }

  async function fetchRelationships() {
    const res = await ontologyApi.getRelationships()
    relationships.value = res.data
  }

  function buildTreeData() {
    const domainMap = new Map<string, Map<string, CapabilityIndicator[]>>()
    for (const ind of indicators.value) {
      const domain = ind.domain || '未分类'
      const category = ind.category || '未分类'
      if (!domainMap.has(domain)) domainMap.set(domain, new Map())
      const catMap = domainMap.get(domain)!
      if (!catMap.has(category)) catMap.set(category, [])
      catMap.get(category)!.push(ind)
    }

    const keys: string[] = []
    treeData.value = Array.from(domainMap.entries()).map(([domain, catMap]) => {
      const domainKey = domain
      keys.push(domainKey)
      return {
        key: domainKey,
        title: DOMAIN_NAMES[domain] || domain,
        children: Array.from(catMap.entries()).map(([category, inds]) => {
          const catKey = `${domainKey}-${category}`
          keys.push(catKey)
          return {
            key: catKey,
            title: CATEGORY_NAMES[category] || category,
            children: inds.map((ind) => ({
              key: ind.id,
              title: `${ind.name} (${ind.thresholdMin}~${ind.thresholdMax} ${ind.unit || ''})`,
              isLeaf: true,
              data: ind,
            })),
          }
        }),
      }
    })
    expandedKeys.value = keys
  }

  return { indicators, relationships, loading, treeData, expandedKeys, fetchIndicators, fetchRelationships }
})
