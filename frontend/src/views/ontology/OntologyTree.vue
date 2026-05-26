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
      >
        <template #title="{ title, nodeType }">
          <span style="display:inline-flex;align-items:center;gap:4px">
            <GlobalOutlined v-if="nodeType === 'domain'" style="color:#1890ff;font-size:14px" />
            <FolderOutlined v-else-if="nodeType === 'category'" style="color:#faad14;font-size:13px" />
            <ThunderboltOutlined v-else style="color:#52c41a;font-size:12px" />
            <span style="font-size:13px">{{ title }}</span>
          </span>
        </template>
      </a-tree>

      <a-empty v-else-if="!loading" description="暂无指标数据" />
    </a-spin>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { GlobalOutlined, FolderOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'

interface TreeNode {
  key: string
  title: string
  children?: TreeNode[]
  indicator?: any
  nodeType?: string
}

const treeData = ref<TreeNode[]>([])
const expandedKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const loading = ref(false)
const errorMsg = ref('')

const emit = defineEmits<{ (e: 'node-select', node: any): void }>()

const DOMAIN_NAMES: Record<string, string> = {
  SurfaceWarfare: '水面作战域', UnderwaterWarfare: '水下作战域', LandWarfare: '陆上作战域',
  LowAltitudeWarfare: '低空作战域', HighAltitudeWarfare: '高空作战域',
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
    const d = ind.domain || '未分类'
    const c = ind.category || '未分类'
    if (!domainMap.has(d)) domainMap.set(d, new Map())
    const cm = domainMap.get(d)!
    if (!cm.has(c)) cm.set(c, [])
    cm.get(c)!.push(ind)
  }

  const result: TreeNode[] = []
  const allKeys: string[] = []
  for (const [domain, catMap] of domainMap) {
    const dk = domain; allKeys.push(dk)
    const catNodes: TreeNode[] = []
    for (const [category, inds] of catMap) {
      const ck = `${domain}-${category}`; allKeys.push(ck)
      catNodes.push({
        key: ck, nodeType: 'category',
        title: `${CATEGORY_NAMES[category] || category} (${inds.length}个)`,
        children: inds.map((ind) => ({
          key: ind.id, nodeType: 'indicator',
          title: ind.name, indicator: ind,
        })),
      })
    }
    result.push({
      key: dk, nodeType: 'domain',
      title: `${DOMAIN_NAMES[domain] || domain} (${catNodes.length}类)`,
      children: catNodes,
    })
  }
  expandedKeys.value = allKeys
  return result
}

async function loadData() {
  loading.value = true
  try {
    const res = await axios.get('/api/ontology/indicators')
    if (res.data.code === 200) treeData.value = buildTree(res.data.data)
  } catch (e: any) { errorMsg.value = '加载失败' } finally { loading.value = false }
}

function expandAll() {
  const keys: string[] = []
  for (const d of treeData.value) { keys.push(d.key); if (d.children) keys.push(...d.children.map(c => c.key)) }
  expandedKeys.value = keys
}
function collapseAll() { expandedKeys.value = [] }

function onSelect(keys: (string | number)[], info: any) {
  selectedKeys.value = keys as string[]
  if (info.node?.indicator) emit('node-select', { data: info.node.indicator })
}

onMounted(() => loadData())
</script>
