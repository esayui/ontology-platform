<template>
  <a-card title="本体指标树" size="small"
    :body-style="{ padding: '8px', maxHeight: 'calc(100vh - 250px)', overflow: 'auto' }">
    <template #extra>
      <a-space size="small">
        <a-button size="small" @click="expandAll">展开全部</a-button>
        <a-button size="small" @click="collapseAll">折叠全部</a-button>
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <div v-if="errorMsg" style="color: red; padding: 8px">{{ errorMsg }}</div>

      <a-tree
        v-if="treeData.length > 0"
        :tree-data="treeData"
        v-model:expandedKeys="expandedKeys"
        :selected-keys="selectedKeys"
        @select="onSelect"
      />

      <a-empty v-else-if="!loading" description="暂无指标数据" />
    </a-spin>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface TreeNode {
  key: string
  title: string
  children?: TreeNode[]
  indicator?: any
}

const treeData = ref<TreeNode[]>([])
const expandedKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const loading = ref(false)
const errorMsg = ref('')

const emit = defineEmits<{
  (e: 'node-select', node: any): void
}>()

const DOMAIN_NAMES: Record<string, string> = {
  SurfaceWarfare: '水面作战域',
  UnderwaterWarfare: '水下作战域',
  LandWarfare: '陆上作战域',
  LowAltitudeWarfare: '低空作战域',
  HighAltitudeWarfare: '高空作战域',
}

const CATEGORY_NAMES: Record<string, string> = {
  Ship: '舰艇', Radar: '雷达系统', WeaponSystem: '武器系统',
  Submarine: '潜艇', UnderwaterDetection: '水下探测',
  Tank: '坦克', Artillery: '火炮系统',
  Helicopter: '直升机', UAV: '无人机',
  Fighter: '歼击机', Bomber: '轰炸机', AWACS: '预警机',
}

function buildTree(indicators: any[]): TreeNode[] {
  const domainMap = new Map<string, Map<string, any[]>>()

  for (const ind of indicators) {
    const domain = ind.domain || '未分类'
    const category = ind.category || '未分类'
    if (!domainMap.has(domain)) domainMap.set(domain, new Map())
    const catMap = domainMap.get(domain)!
    if (!catMap.has(category)) catMap.set(category, [])
    catMap.get(category)!.push(ind)
  }

  const result: TreeNode[] = []
  const allKeys: string[] = []

  for (const [domain, catMap] of domainMap) {
    const domainKey = domain
    allKeys.push(domainKey)
    const catNodes: TreeNode[] = []

    for (const [category, inds] of catMap) {
      const catKey = `${domain}-${category}`
      allKeys.push(catKey)
      catNodes.push({
        key: catKey,
        title: `${CATEGORY_NAMES[category] || category} (${inds.length}个)`,
        children: inds.map((ind) => ({
          key: ind.id,
          title: `${ind.name}`,
          indicator: ind,
        })),
      })
    }

    result.push({
      key: domainKey,
      title: `${DOMAIN_NAMES[domain] || domain} (${catNodes.length}类)`,
      children: catNodes,
    })
  }

  expandedKeys.value = allKeys
  return result
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const response = await axios.get('/api/ontology/indicators')
    const data = response.data

    if (data.code === 200 && Array.isArray(data.data)) {
      console.log('OntologyTree: loaded', data.data.length, 'indicators')
      treeData.value = buildTree(data.data)
    } else {
      errorMsg.value = '数据格式异常: ' + JSON.stringify(data).substring(0, 100)
    }
  } catch (e: any) {
    console.error('OntologyTree load error:', e)
    errorMsg.value = '加载失败: ' + (e.message || 'unknown')
  } finally {
    loading.value = false
  }
}

function expandAll() {
  const keys: string[] = []
  for (const d of treeData.value) {
    keys.push(d.key)
    if (d.children) keys.push(...d.children.map((c) => c.key))
  }
  expandedKeys.value = keys
}

function collapseAll() {
  expandedKeys.value = []
}

function onSelect(keys: (string | number)[], info: any) {
  selectedKeys.value = keys as string[]
  if (info.node?.indicator) {
    emit('node-select', { data: info.node.indicator })
  }
}

onMounted(() => {
  loadData()
})
</script>
