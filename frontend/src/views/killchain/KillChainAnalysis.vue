<template>
  <div class="analysis-page">
    <div class="analysis-toolbar">
      <a-space>
        <a-button size="small" @click="router.push('/killchain')">← 返回列表</a-button>
        <a-button size="small" @click="router.push('/killchain/modeler/' + taskId)">建模画布</a-button>
        <a-divider type="vertical" />
        <span style="font-weight:bold">{{ taskName }}</span>
        <a-divider type="vertical" />
        <a-tag color="green">整体满足率: {{ overallRate }}%</a-tag>
        <a-tag v-if="riskCount > 0" color="red">风险节点: {{ riskCount }}</a-tag>
        <a-tag v-if="gapCount > 0" color="orange">指标缺口: {{ gapCount }}</a-tag>
      </a-space>
    </div>

    <a-row :gutter="8" style="flex:1;overflow:hidden;padding:8px">
      <!-- Left: Kill Chain Navigation -->
      <a-col :span="6" class="analysis-panel">
        <a-card title="杀伤链导航" size="small" :body-style="{ padding:'8px', height:'calc(100vh-180px)', overflow:'auto' }">
          <div v-for="(chain, ci) in chains" :key="chain.id" class="chain-section" :class="{ active: activeChain === ci }" @click="selectChain(ci)">
            <div class="chain-title">
              链{{ ci+1 }} (满足率: {{ chain.satisfactionRate }}%)
              <a-tag v-if="chain.riskLevel === 'high'" color="red" size="small">高风险</a-tag>
              <a-tag v-else-if="chain.riskLevel === 'medium'" color="orange" size="small">中风险</a-tag>
            </div>
            <div class="chain-flow">
              <template v-for="(n, ni) in chain.nodes" :key="n.id">
                <div class="flow-node" :class="{ satisfied: n.satisfied, risk: n.risk, active: activeNode === n.id }" @click.stop="clickChainNode(n)">
                  <span class="flow-node-icon">{{ nodeIcon(n.type) }}</span>
                  <span class="flow-node-label">{{ n.name }}</span>
                </div>
                <div v-if="ni < chain.nodes.length - 1" class="flow-arrow">→</div>
              </template>
            </div>
          </div>
          <a-empty v-if="chains.length === 0" description="未找到杀伤链路径" />
        </a-card>
      </a-col>

      <!-- Center: Capability Relations Table -->
      <a-col :span="10" class="analysis-panel">
        <a-card title="能力关系表" size="small" :body-style="{ padding:'8px', height:'calc(100vh-180px)', overflow:'auto' }">
          <a-table
            v-if="capRelations.length > 0"
            :columns="capRelColumns"
            :data-source="capRelations"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'srcNode'">
                <span>{{ nodeIcon(record.srcType) }} {{ record.srcNode }}</span>
              </template>
              <template v-if="column.key === 'tgtNode'">
                <span>{{ nodeIcon(record.tgtType) }} {{ record.tgtNode }}</span>
              </template>
            </template>
          </a-table>
          <a-empty v-else description="点击左侧杀伤链查看能力关系" />
        </a-card>
      </a-col>

      <!-- Right: Indicator Analysis -->
      <a-col :span="8" class="analysis-panel">
        <a-card title="指标分析" size="small" :body-style="{ padding:'8px', height:'calc(100vh-180px)', overflow:'auto' }">
          <!-- Indicator tree -->
          <div v-if="activeAnalysis" class="indicator-section">
            <div class="section-title">关联指标体系</div>
            <div v-for="ind in activeAnalysis.indicators" :key="ind.id" class="indicator-row" :class="{ satisfied: ind.satisfied }">
              <span :style="{color: ind.satisfied ? '#52c41a' : '#ff4d4f'}">{{ ind.satisfied ? '✓' : '✗' }}</span>
              <span>{{ ind.name }}</span>
              <a-tag size="small" :color="ind.satisfied ? 'green' : 'red'">{{ ind.score }}%</a-tag>
            </div>
          </div>

          <!-- Satisfaction matrix -->
          <div v-if="activeAnalysis" class="matrix-section">
            <div class="section-title">满足度矩阵</div>
            <div class="mini-matrix">
              <table>
                <thead><tr><th></th><th v-for="h in activeAnalysis.matrixHeaders" :key="h">{{ h }}</th></tr></thead>
                <tbody>
                  <tr v-for="r in activeAnalysis.matrixRows" :key="r.id">
                    <td>{{ r.name }}</td>
                    <td v-for="h in activeAnalysis.matrixHeaders" :key="h"
                      :style="{ background: getMatrixColor(r.scores[h] || 0) }"
                      class="matrix-dot"
                    >{{ r.scores[h] || '-' }}%</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Gap analysis -->
          <div v-if="activeAnalysis?.gaps?.length" class="gap-section">
            <div class="section-title">指标缺口 ({{ activeAnalysis.gaps.length }})</div>
            <div v-for="g in activeAnalysis.gaps" :key="g.id" class="gap-item">
              <a-tag color="orange">{{ g.name }}</a-tag>
              <span style="font-size:11px;color:#999">{{ g.reason }}</span>
            </div>
          </div>

          <a-empty v-if="!activeAnalysis" description="点击节点查看" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { killChainApi } from '@/api/killchain'

const router = useRouter()
const route = useRoute()
const taskId = route.params.id as string
const taskName = ref('')
const activeNode = ref('')
const activeChain = ref<number | null>(null)
const activeAnalysis = ref<any>(null)

// Mock data
const chains = ref<any[]>([])
const ontologyRels = ref<any[]>([])
const allOntologyIndicators = ref<any[]>([])
const netNodes = ref<any[]>([])
const riskCount = computed(() => netNodes.value.filter(n => !n.satisfied && n.level === 'capability').length)
const gapCount = computed(() => {
  if (!activeAnalysis.value?.gaps) return 0
  return activeAnalysis.value.gaps.length
})
const overallRate = computed(() => {
  if (netNodes.value.length === 0) return 0
  const satisfied = netNodes.value.filter(n => n.satisfied).length
  return Math.round((satisfied / netNodes.value.length) * 100)
})

const capRelations = ref<any[]>([])
const capRelColumns = [
  { title: '所属节点(源)', key: 'srcNode', width: 110 },
  { title: '能力指标(源)', dataIndex: 'srcCap', width: 100 },
  { title: '关系', dataIndex: 'relation', width: 140 },
  { title: '所属节点(目标)', key: 'tgtNode', width: 110 },
  { title: '能力指标(目标)', dataIndex: 'tgtCap', width: 100 },
]

const NODE_ICONS: Record<string, string> = {
  reconnaissance: '👁', detection: '📡', command: '🎯', control: '🕹',
  communication: '📶', strike: '💥', assessment: '📊', target: '🎪',
}

function nodeIcon(type: string) { return NODE_ICONS[type] || '●' }

function findKillChains(modelNodes: any[], modelEdges: any[]) {
  // Build adjacency list respecting edge direction:
  // forward: data source -> data target
  // reverse: data target -> data source
  // bidirectional: both directions
  const adj = new Map<string, { target: string; edge: any }[]>()
  function addEdge(src: string, tgt: string, edge: any) {
    if (!adj.has(src)) adj.set(src, [])
    adj.get(src)!.push({ target: tgt, edge })
  }
  for (const e of modelEdges) {
    const ds: string = e.source || e.data?.source
    const dt: string = e.target || e.data?.target
    const dir: string = e.direction || e.data?.direction || 'forward'
    if (!ds || !dt) continue
    if (dir === 'forward') {
      addEdge(ds, dt, e)
    } else if (dir === 'reverse') {
      addEdge(dt, ds, e)
    } else if (dir === 'bidirectional') {
      addEdge(ds, dt, e)
      addEdge(dt, ds, e)
    }
  }

  // Build node map
  const nodeMap = new Map(modelNodes.map(n => {
    const id: string = n.id || n.data?.id
    const type: string = n.type || n.data?.type || ''
    const label: string = n.label || n.data?.label || id
    return [id, { id, name: label, type }]
  }))

  // Find all target-type nodes
  const targetNodes = [...nodeMap.values()].filter(n => n.type === 'target')
  if (targetNodes.length === 0) return []

  // For each target, find all closed-loop paths: target -> ... -> same target
  // Each node appears at most once per path (excluding the target which appears twice: start and end)
  const chains: any[] = []
  let chainIdx = 0

  for (const targetNode of targetNodes) {
    const targetId = targetNode.id
    // DFS: start from target's neighbors (first step away)
    const startEdges = adj.get(targetId) || []
    const stack: { nodeId: string; path: string[]; edges: any[] }[] = []

    for (const se of startEdges) {
      stack.push({
        nodeId: se.target,
        path: [targetId, se.target],
        edges: [se.edge],
      })
    }

    while (stack.length > 0) {
      const { nodeId, path, edges } = stack.pop()!
      // Check if we've returned to the starting target (closed loop)
      if (nodeId === targetId && path.length > 2) {
        chainIdx++
        const chainNodes = path.slice(0, -1).map((id: string) => {
          const raw = modelNodes.find((mn: any) => (mn.id || mn.data?.id) === id)
          const capabilities = raw?.capabilities || raw?.data?.capabilities || ''
          const n = nodeMap.get(id)!
          const risky = n.type === 'communication' || n.type === 'control'
          return { id: n.id, name: n.name, type: n.type, satisfied: !risky, risk: risky, capabilities, gapIndicators: risky ? [n.name + '-指标'] : [] }
        })
        const satisfied = chainNodes.filter(n => n.satisfied).length
        chains.push({
          id: 'chain-' + chainIdx,
          satisfactionRate: chainNodes.length > 0 ? Math.round((satisfied / chainNodes.length) * 100) : 0,
          riskLevel: chainNodes.some(n => n.risk) ? 'medium' : 'low',
          nodes: chainNodes,
          edges: edges.map((e, i) => ({
            id: e.id || (e.data?.id) || 'ce' + i,
            source: e.source || e.data?.source,
            target: e.target || e.data?.target,
            type: e.type || e.data?.type || 'command',
          })),
        })
        continue
      }
      // Continue exploring neighbors (avoid revisiting nodes within this path)
      const neighbors = adj.get(nodeId) || []
      for (const { target, edge } of neighbors) {
        if (target === targetId || !path.includes(target)) {
          stack.push({ nodeId: target, path: [...path, target], edges: [...edges, edge] })
        }
      }
    }
  }
  return chains
}

function generateMockData() {
  netNodes.value = [
    { id: 'nn1', name: '探测距离', level: 'capability', category: '雷达', satisfied: true, score: 85, linkedIds: ['nn8','nn9'] },
    { id: 'nn2', name: '响应时间', level: 'capability', category: '指挥', satisfied: false, score: 45, linkedIds: ['nn10'] },
    { id: 'nn3', name: '通信带宽', level: 'capability', category: '通信', satisfied: false, score: 30, linkedIds: ['nn11'] },
    { id: 'nn4', name: '命中精度', level: 'capability', category: '武器', satisfied: true, score: 90, linkedIds: ['nn12'] },
    { id: 'nn5', name: '续航能力', level: 'capability', category: '无人机', satisfied: true, score: 75, linkedIds: ['nn13'] },
    { id: 'nn8', name: '346A雷达', level: 'platform', category: '雷达', satisfied: true, score: 85, linkedIds: ['nn16'] },
    { id: 'nn9', name: '382雷达', level: 'platform', category: '雷达', satisfied: false, score: 60, linkedIds: ['nn17'] },
    { id: 'nn10', name: '指挥系统', level: 'platform', category: '指挥', satisfied: false, score: 40, linkedIds: [] },
    { id: 'nn11', name: '通信链路', level: 'platform', category: '通信', satisfied: false, score: 35, linkedIds: ['nn18'] },
    { id: 'nn12', name: '火控系统', level: 'platform', category: '武器', satisfied: true, score: 88, linkedIds: ['nn19'] },
    { id: 'nn13', name: '无侦-8', level: 'platform', category: '无人机', satisfied: true, score: 80, linkedIds: [] },
    { id: 'nn16', name: '055驱逐舰', level: 'equipment', category: '舰艇', satisfied: true, score: 90, linkedIds: [] },
    { id: 'nn17', name: '052D驱逐舰', level: 'equipment', category: '舰艇', satisfied: false, score: 55, linkedIds: [] },
    { id: 'nn18', name: '通信中继站', level: 'equipment', category: '通信', satisfied: false, score: 30, linkedIds: [] },
    { id: 'nn19', name: '红旗-9B', level: 'equipment', category: '武器', satisfied: true, score: 88, linkedIds: [] },
  ]
}

async function selectChain(ci: number) {
  activeChain.value = activeChain.value === ci ? null : ci
  const chain = chains.value[ci]
  activeNode.value = ''
  // Build capability relations from ontology relationships
  capRelations.value = []
  if (chain && chain.edges && ontologyRels.value.length > 0) {
    // Map indicator ID -> name for quick lookup
    const indNameMap = new Map<string, string>()
    for (const ind of allOntologyIndicators.value) {
      indNameMap.set(ind.id, ind.name)
    }
    let relIdx = 0
    for (const edge of chain.edges) {
      const srcNode = chain.nodes.find((n: any) => n.id === edge.source)
      const tgtNode = chain.nodes.find((n: any) => n.id === edge.target)
      if (!srcNode || !tgtNode) continue
      const srcCaps: string[] = (srcNode.capabilities || '').split(',').filter(Boolean)
      const tgtCaps: string[] = (tgtNode.capabilities || '').split(',').filter(Boolean)
      if (srcCaps.length === 0 || tgtCaps.length === 0) continue
      // Cross-reference with ontology relationships (bidirectional match)
      for (const sc of srcCaps) {
        for (const tc of tgtCaps) {
          const match = ontologyRels.value.find((r: any) =>
            (r.sourceIndicatorId === sc && r.targetIndicatorId === tc) ||
            (r.sourceIndicatorId === tc && r.targetIndicatorId === sc))
          if (match) {
            capRelations.value.push({
              id: 'rel-' + (relIdx++),
              srcNode: srcNode.name, srcType: srcNode.type,
              srcCap: indNameMap.get(sc) || sc.slice(0, 8),
              relation: (match.relationshipType || '') + (match.droolsRuleName ? ' / ' + match.droolsRuleName : ''),
              tgtNode: tgtNode.name, tgtType: tgtNode.type,
              tgtCap: indNameMap.get(tc) || tc.slice(0, 8),
            })
          }
        }
      }
    }
  }
  activeAnalysis.value = {
    indicators: [
      { id: 'i1', name: '探测距离', satisfied: true, score: 85 },
      { id: 'i2', name: '响应时间', satisfied: false, score: 45 },
      { id: 'i3', name: '通信带宽', satisfied: false, score: 30 },
      { id: 'i4', name: '命中精度', satisfied: true, score: 90 },
    ],
    matrixHeaders: ['探测距离', '响应时间', '通信带宽', '命中精度'],
    matrixRows: [
      { id: 'r1', name: '055驱逐舰', scores: { '探测距离': 85, '响应时间': 70, '通信带宽': 60, '命中精度': 90 } },
      { id: 'r2', name: '052D驱逐舰', scores: { '探测距离': 65, '响应时间': 55, '通信带宽': 40, '命中精度': 80 } },
    ],
    gaps: [
      { id: 'g1', name: '通信带宽', reason: '052D通信带宽不足(40%), 需要升级' },
      { id: 'g2', name: '响应时间', reason: '指挥系统响应时间未达标(55%)' },
    ],
  }
}

function clickChainNode(node: any) {
  activeNode.value = node.id
  activeAnalysis.value = {
    indicators: [
      { id: 'i1', name: '探测距离', satisfied: node.satisfied, score: node.satisfied ? 85 : 40 },
      { id: 'i2', name: '响应时间', satisfied: node.satisfied, score: node.satisfied ? 75 : 45 },
    ],
    matrixHeaders: ['探测距离', '响应时间'],
    matrixRows: [
      { id: 'r1', name: '346A雷达', scores: { '探测距离': 85, '响应时间': 70 } },
    ],
    gaps: node.gapIndicators.length > 0
      ? [{ id: 'g1', name: node.gapIndicators[0], reason: '指标未达标' }]
      : [],
  }
}

function clickNetNode(node: any) {
  activeNode.value = node.id
  activeAnalysis.value = {
    indicators: [
      { id: 'i1', name: '探测距离', satisfied: node.score >= 80, score: node.score },
      { id: 'i2', name: '响应时间', satisfied: node.score >= 70, score: Math.max(0, node.score - 15) },
    ],
    matrixHeaders: ['指标A', '指标B'],
    matrixRows: [
      { id: 'r1', name: node.name, scores: { '指标A': node.score, '指标B': Math.max(0, node.score - 10) } },
    ],
    gaps: node.score < 60 ? [{ id: 'g1', name: node.name, reason: '满足度低于60%' }] : [],
  }
}

function getMatrixColor(score: number): string {
  if (score >= 80) return '#d4edda'
  if (score >= 60) return '#fff3cd'
  return '#f8d7da'
}

async function loadAnalysis() {
  chains.value = []
  ontologyRels.value = []
  allOntologyIndicators.value = []
  activeNode.value = ''
  activeChain.value = null
  activeAnalysis.value = null
  try {
    const [taskRes, relsRes, indsRes] = await Promise.all([
      killChainApi.getTask(route.params.id as string),
      fetch('/api/ontology/relationships').then(r => r.json()),
      fetch('/api/ontology/indicators').then(r => r.json()),
    ])
    taskName.value = taskRes.data.name
    if (relsRes.code === 200) ontologyRels.value = relsRes.data || []
    if (indsRes.code === 200) allOntologyIndicators.value = indsRes.data || []

    if (taskRes.data.modelData) {
      try {
        const model = JSON.parse(taskRes.data.modelData)
        chains.value = findKillChains(model.nodes || [], model.edges || [])
      } catch { /* ignore */ }
    }
  } catch { message.error('加载任务失败') }
}

onMounted(loadAnalysis)
watch(() => route.params.id, loadAnalysis)
</script>

<style scoped>
.analysis-page { display:flex; flex-direction:column; height:calc(100vh - 120px); }
.analysis-toolbar { padding:6px 12px; background:#fafafa; border-bottom:1px solid #e8e8e8; display:flex; align-items:center; }
.analysis-panel { height:100%; border-left:1px solid #e8e8e8; }
.chain-section { margin-bottom:12px; padding:8px; border:1px solid #e8e8e8; border-radius:6px; cursor:pointer; transition:border .2s; }
.chain-section:hover { border-color:#1890ff; }
.chain-section.active { border-color:#1890ff; background:#e6f7ff; }
.chain-title { font-weight:600; margin-bottom:6px; font-size:13px; }
.chain-flow { display:flex; flex-wrap:wrap; align-items:center; gap:4px; }
.flow-node { display:flex; flex-direction:column; align-items:center; padding:4px 6px; border-radius:4px; border:1px solid #e8e8e8; min-width:50px; transition:all .2s; cursor:pointer; }
.flow-node.satisfied { border-color:#52c41a; background:#f6ffed; }
.flow-node.risk { border-color:#ff4d4f; background:#fff2f0; }
.flow-node.active { border-color:#1890ff; box-shadow:0 0 6px rgba(24,144,255,.3); }
.flow-node-icon { font-size:16px; }
.flow-node-label { font-size:10px; white-space:nowrap; }
.flow-arrow { color:#999; font-weight:bold; }
.layered-network { display:flex; flex-direction:column; height:100%; gap:8px; padding:4px; }
.layer { flex:1; border:1px solid #e8e8e8; border-radius:6px; padding:6px; overflow:auto; }
.layer-label { font-size:11px; color:#999; margin-bottom:4px; }
.layer-nodes { display:flex; flex-wrap:wrap; gap:4px; }
.capability-layer { background:#f6ffed; }
.platform-layer { background:#e6f7ff; }
.equipment-layer { background:#f9f0ff; }
.net-node { padding:3px 8px; border-radius:4px; color:#fff; font-size:11px; cursor:pointer; transition:all .2s; }
.net-node:hover { opacity:.85; }
.net-node.active { box-shadow:0 0 8px rgba(24,144,255,.5); transform:scale(1.05); }
.section-title { font-weight:600; font-size:12px; margin:10px 0 6px; padding-bottom:4px; border-bottom:1px solid #f0f0f0; }
.indicator-row { display:flex; align-items:center; gap:6px; padding:2px 0; font-size:12px; }
.mini-matrix { overflow:auto; font-size:11px; }
.mini-matrix table { border-collapse:collapse; width:100%; }
.mini-matrix th, .mini-matrix td { border:1px solid #e8e8e8; padding:2px 4px; text-align:center; }
.mini-matrix th { background:#fafafa; font-size:10px; }
.matrix-dot { min-width:32px; }
.gap-item { display:flex; align-items:center; gap:8px; padding:2px 0; }
</style>
