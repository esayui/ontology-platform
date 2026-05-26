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

      <!-- Center: Capability Network -->
      <a-col :span="10" class="analysis-panel">
        <a-card title="能力-平台-装备关系网络" size="small" :body-style="{ padding:'4px', height:'calc(100vh-180px)' }">
          <div class="layered-network">
            <!-- Capability layer -->
            <div class="layer capability-layer">
              <div class="layer-label">能力层</div>
              <div class="layer-nodes">
                <div v-for="n in capNodes" :key="n.id"
                  class="net-node" :class="{ satisfied: n.satisfied, active: activeNode === n.id }"
                  :style="{ background: n.satisfied ? '#52c41a' : '#ff4d4f' }"
                  @click="clickNetNode(n)">
                  {{ n.name }}
                </div>
              </div>
            </div>
            <!-- Platform layer -->
            <div class="layer platform-layer">
              <div class="layer-label">平台层</div>
              <div class="layer-nodes">
                <div v-for="n in platNodes" :key="n.id"
                  class="net-node" :class="{ satisfied: n.satisfied, active: activeNode === n.id }"
                  :style="{ background: n.satisfied ? '#1890ff' : '#ff4d4f' }"
                  @click="clickNetNode(n)">
                  {{ n.name }}
                </div>
              </div>
            </div>
            <!-- Equipment layer -->
            <div class="layer equipment-layer">
              <div class="layer-label">装备层</div>
              <div class="layer-nodes">
                <div v-for="n in equipNodes" :key="n.id"
                  class="net-node" :class="{ satisfied: n.satisfied, active: activeNode === n.id }"
                  :style="{ background: n.satisfied ? '#722ed1' : '#ff4d4f' }"
                  @click="clickNetNode(n)">
                  {{ n.name }}
                </div>
              </div>
            </div>
          </div>
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
import { ref, computed, onMounted } from 'vue'
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

const capNodes = computed(() => netNodes.value.filter(n => n.level === 'capability'))
const platNodes = computed(() => netNodes.value.filter(n => n.level === 'platform'))
const equipNodes = computed(() => netNodes.value.filter(n => n.level === 'equipment'))

const NODE_ICONS: Record<string, string> = {
  reconnaissance: '👁', detection: '📡', command: '🎯', control: '🕹',
  communication: '📶', strike: '💥', assessment: '📊', target: '🎪',
}

function nodeIcon(type: string) { return NODE_ICONS[type] || '●' }

function generateMockData() {
  // Kill chains
  chains.value = [
    {
      id: 'chain-1', satisfactionRate: 78, riskLevel: 'medium',
      nodes: [
        { id: 'cn1', name: '侦察卫星', type: 'reconnaissance', satisfied: true, risk: false, gapIndicators: [] },
        { id: 'cn2', name: '探测雷达', type: 'detection', satisfied: true, risk: false, gapIndicators: [] },
        { id: 'cn3', name: '指挥中心', type: 'command', satisfied: true, risk: true, gapIndicators: ['resp-time'] },
        { id: 'cn4', name: '通信中继', type: 'communication', satisfied: false, risk: true, gapIndicators: ['bandwidth', 'latency'] },
        { id: 'cn5', name: '打击单元', type: 'strike', satisfied: true, risk: false, gapIndicators: [] },
        { id: 'cn6', name: '敌方目标', type: 'target', satisfied: false, risk: false, gapIndicators: [] },
      ],
      edges: [
        { id: 'ce1', source: 'cn1', target: 'cn2', type: 'detection' },
        { id: 'ce2', source: 'cn2', target: 'cn3', type: 'communication' },
        { id: 'ce3', source: 'cn3', target: 'cn4', type: 'command' },
        { id: 'ce4', source: 'cn4', target: 'cn5', type: 'communication' },
        { id: 'ce5', source: 'cn5', target: 'cn6', type: 'strike' },
      ],
    },
    {
      id: 'chain-2', satisfactionRate: 45, riskLevel: 'high',
      nodes: [
        { id: 'cn7', name: '无人机侦察', type: 'reconnaissance', satisfied: true, risk: false, gapIndicators: [] },
        { id: 'cn8', name: '地面雷达', type: 'detection', satisfied: false, risk: true, gapIndicators: ['range', 'resolution'] },
        { id: 'cn9', name: '火控系统', type: 'control', satisfied: false, risk: true, gapIndicators: ['accuracy'] },
        { id: 'cn10', name: '导弹发射车', type: 'strike', satisfied: true, risk: false, gapIndicators: [] },
        { id: 'cn11', name: '敌方舰艇', type: 'target', satisfied: false, risk: false, gapIndicators: [] },
      ],
      edges: [
        { id: 'ce6', source: 'cn7', target: 'cn8', type: 'detection' },
        { id: 'ce7', source: 'cn8', target: 'cn9', type: 'command' },
        { id: 'ce8', source: 'cn9', target: 'cn10', type: 'command' },
        { id: 'ce9', source: 'cn10', target: 'cn11', type: 'strike' },
      ],
    },
  ]

  // Network nodes (3 layers)
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

function selectChain(ci: number) {
  activeChain.value = activeChain.value === ci ? null : ci
  const chain = chains.value[ci]
  activeNode.value = ''
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

onMounted(async () => {
  try {
    const res = await killChainApi.getTask(taskId)
    taskName.value = res.data.name
  } catch { message.error('加载任务失败') }
  generateMockData()
})
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
