<template>
  <div class="knowledge-graph-page">
    <GraphToolbar
      :layout="graphStore.currentLayout"
      :selected-node="graphStore.selectedNodeId"
      @layout-change="handleLayoutChange"
      @search="handleSearch"
      @filter-change="handleFilterChange"
      @fit="handleFit"
      @refresh="loadGraphData"
    />

    <a-row :gutter="0" style="flex: 1; overflow: hidden">
      <a-col :span="5" class="side-panel">
        <OntologyTree
          @node-select="handleOntologyNodeSelect"
          @node-drag="handleOntologyNodeDrag"
        />
      </a-col>

      <a-col :span="14" class="graph-panel">
        <CytoscapeCanvas
          ref="cyCanvasRef"
          :elements="graphElements"
          :layout="graphStore.currentLayout"
          :highlight-path="graphStore.highlightPath"
          @node-click="handleNodeClick"
          @edge-click="handleEdgeClick"
        />
      </a-col>

      <a-col :span="5" class="side-panel">
        <a-card title="详情" size="small" class="detail-card">
          <!-- Node selected -->
          <div v-if="selectedNodeData">
            <a-descriptions :column="1" size="small" bordered>
              <a-descriptions-item label="名称">{{ selectedNodeData.label || selectedNodeData.name }}</a-descriptions-item>
              <a-descriptions-item label="域">{{ selectedNodeData.domain }}</a-descriptions-item>
              <a-descriptions-item label="类别">{{ selectedNodeData.category }}</a-descriptions-item>
              <a-descriptions-item label="单位">{{ selectedNodeData.unit || '-' }}</a-descriptions-item>
            </a-descriptions>
            <a-divider />
            <a-button v-if="selectedEdgeData" size="small" block type="primary" @click="openRelatedRule">
              查看关联规则
            </a-button>
          </div>
          <!-- Edge selected (no node) -->
          <div v-else-if="selectedEdgeData">
            <a-descriptions :column="1" size="small" bordered>
              <a-descriptions-item label="关系类型">
                <a-tag :color="edgeTypeColor(selectedEdgeData.label)">
                  {{ edgeTypeLabel(selectedEdgeData.label) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="关联规则">{{ selectedEdgeData.ruleName || '无' }}</a-descriptions-item>
              <a-descriptions-item label="权重">{{ selectedEdgeData.weight || '-' }}</a-descriptions-item>
              <a-descriptions-item label="源节点">{{ edgeSourceName }}</a-descriptions-item>
              <a-descriptions-item label="目标节点">{{ edgeTargetName }}</a-descriptions-item>
            </a-descriptions>
            <a-divider />
            <a-space direction="vertical" size="small" style="width: 100%">
              <a-button size="small" block @click="jumpToNode(selectedEdgeData.source)">定位源节点</a-button>
              <a-button size="small" block @click="jumpToNode(selectedEdgeData.target)">定位目标节点</a-button>
              <a-button v-if="selectedEdgeData.ruleName" size="small" block type="primary" @click="openRelatedRule">
                查看关联规则
              </a-button>
            </a-space>
          </div>
          <a-empty v-else description="点击节点或边查看详情" />
        </a-card>

        <a-card title="分析" size="small" class="detail-card" style="margin-top: 8px">
          <a-space direction="vertical" size="small" style="width: 100%">
            <a-button size="small" block @click="runAnalysis('upstream')" :disabled="!selectedNodeData">上游影响分析</a-button>
            <a-button size="small" block @click="runAnalysis('downstream')" :disabled="!selectedNodeData">下游影响分析</a-button>
            <a-button size="small" block @click="runAnalysis('path')">路径查询</a-button>
            <a-button size="small" block @click="runAnalysis('violation')">违规链路分析</a-button>
            <a-button size="small" block @click="runAnalysis('centrality')">中心性分析</a-button>
            <a-button size="small" block @click="runAnalysis('isolated')">孤立指标检测</a-button>
          </a-space>
        </a-card>

        <a-card title="分析结果" size="small" class="detail-card" style="margin-top: 8px">
          <a-spin :spinning="analysisLoading">
            <div v-if="analysisResults" class="analysis-output">
              <!-- Upstream / Downstream -->
              <template v-if="analysisType === 'upstream' || analysisType === 'downstream'">
                <p style="color:#999;font-size:12px">
                  {{ analysisType === 'upstream' ? '上游影响' : '下游影响' }}
                  共 {{ analysisResults.length }} 条路径
                </p>
                <div v-for="(item, i) in analysisResults" :key="i" style="margin-bottom:8px;padding:6px;background:#fafafa;border-radius:4px">
                  <div><strong>{{ item.node?.name || item.node?.id }}</strong></div>
                  <div style="font-size:11px;color:#666">
                    距离: {{ item.distance }} |
                    路径: <a-tag v-for="(r,j) in (item.rels||[])" :key="j" size="small" style="margin:1px">{{ r.type }}</a-tag>
                  </div>
                </div>
              </template>

              <!-- Path -->
              <template v-else-if="analysisType === 'path'">
                <div v-if="analysisResults.length > 0 && analysisResults[0].nodes">
                  <p>路径长度: {{ analysisResults[0].length }}</p>
                  <a-timeline>
                    <a-timeline-item v-for="(n, i) in analysisResults[0].nodes" :key="i">
                      {{ n.name }} <a-tag size="small">{{ n.domain }}</a-tag>
                    </a-timeline-item>
                  </a-timeline>
                </div>
                <a-empty v-else description="未找到路径">
                  <template #description>
                    <span>未找到路径，请先点击节点选择起点</span>
                    <a-input v-model:value="pathTarget" placeholder="输入目标节点ID" size="small" style="width:180px;margin-top:4px" />
                    <a-button size="small" type="primary" @click="doPathSearch" style="margin-top:4px">查询</a-button>
                  </template>
                </a-empty>
              </template>

              <!-- Violation -->
              <template v-else-if="analysisType === 'violation'">
                <!-- Step 1: Input facts -->
                <div v-if="!analysisResults">
                  <p style="font-size:12px;color:#666;margin-bottom:8px">输入指标数据以进行违规分析和传播追溯：</p>
                  <div v-for="(item, i) in violationFacts" :key="i" style="display:flex;gap:4px;margin-bottom:4px">
                    <a-input v-model:value="item.name" placeholder="指标ID" size="small" style="flex:1" />
                    <a-input-number v-model:value="item.value" placeholder="值" size="small" style="width:80px" />
                    <a-button size="small" danger type="text" @click="violationFacts.splice(i,1)">×</a-button>
                  </div>
                  <a-space size="small" style="margin-bottom:8px">
                    <a-button size="small" dashed @click="violationFacts.push({name:'',value:0})">+ 添加</a-button>
                    <a-button size="small" @click="loadViolationPreset('radar')">雷达</a-button>
                    <a-button size="small" @click="loadViolationPreset('submarine')">潜艇</a-button>
                    <a-button size="small" @click="loadViolationPreset('tank')">坦克</a-button>
                  </a-space>
                  <a-button type="primary" size="small" block @click="runViolationAnalysis" :loading="analysisLoading">执行违规分析</a-button>
                </div>
                <!-- Step 2: Results -->
                <div v-else>
                  <p style="font-size:12px;color:#666">{{ analysisResults.summary || '' }}</p>
                  <div v-if="analysisResults.paths?.length > 0">
                    <div v-for="(p, i) in analysisResults.paths.slice(0, 10)" :key="i" style="margin-bottom:8px;padding:6px;background:#fff2f0;border-radius:4px">
                      <div style="font-size:11px;color:#cf1322;font-weight:bold">
                        {{ p.violation?.ruleName }} — {{ p.violation?.indicatorId }}
                      </div>
                      <div style="font-size:11px;color:#999">传播链 (深度{{ p.depth }}):</div>
                      <div style="font-size:11px;color:#333">
                        {{ (p.nodes||[]).map((n:any) => n.name).join(' → ') }}
                      </div>
                    </div>
                  </div>
                  <div v-else style="color:#52c41a;font-size:12px">无违规或无法追溯传播路径</div>
                </div>
              </template>

              <!-- Centrality -->
              <template v-else-if="analysisType === 'centrality'">
                <p style="color:#999;font-size:12px">共 {{ analysisResults.totalNodes || 0 }} 个节点</p>
                <div v-for="(n, i) in (analysisResults.topNodes || []).slice(0, 10)" :key="i" style="font-size:11px;margin:2px 0">
                  {{ i+1 }}. {{ n }}
                </div>
              </template>

              <!-- Isolated -->
              <template v-else-if="analysisType === 'isolated'">
                <div v-if="(analysisResults||[]).length === 0">
                  <a-tag color="green">无孤立指标</a-tag>
                </div>
                <div v-else>
                  <p style="color:#999;font-size:12px">共 {{ analysisResults.length }} 个孤立指标</p>
                  <a-tag v-for="(n, i) in analysisResults" :key="i" color="orange" style="margin:2px">{{ n }}</a-tag>
                </div>
              </template>
            </div>
            <a-empty v-else description="点击分析按钮查看结果" />
          </a-spin>
        </a-card>
      </a-col>
    </a-row>

    <RelationshipEditor
      :visible="graphStore.showRelationshipEditor"
      :relationship="editingRelationship"
      @save="handleRelationshipSave"
      @close="graphStore.showRelationshipEditor = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import axios from 'axios'
import CytoscapeCanvas from '@/components/graph/CytoscapeCanvas.vue'
import GraphToolbar from '@/components/graph/GraphToolbar.vue'
import RelationshipEditor from '@/components/graph/RelationshipEditor.vue'
import OntologyTree from '@/views/ontology/OntologyTree.vue'
import { useGraphStore } from '@/stores/graph'
import { graphApi } from '@/api/graph'
import { ontologyApi } from '@/api/ontology'
import type { LayoutType } from '@/types/graph'
import type { CapabilityRelationship } from '@/types/ontology'

const router = useRouter()
const graphStore = useGraphStore()
const cyCanvasRef = ref()

const graphElements = ref<any[]>([])
const selectedNodeData = ref<any>(null)
const selectedEdgeData = ref<any>(null)
const editingRelationship = ref<CapabilityRelationship | null>(null)

async function loadGraphData() {
  try {
    const res = await graphApi.getAllNodes()
    const nodes: any[] = []
    const edges: any[] = []

    for (const item of res.data) {
      const node = item.node
      nodes.push({
        data: {
          id: node.id,
          label: node.name || node.id,
          domain: node.domain,
          category: node.category,
          unit: node.unit,
        },
        classes: [
          node.domain ? `domain-${node.domain}` : '',
          node.category ? `category-${node.category}` : '',
        ].filter(Boolean),
      })

      if (item.relationships) {
        for (const rel of item.relationships) {
          if (!rel || !rel.id) continue
          edges.push({
            data: {
              id: rel.id,
              source: node.id,
              target: rel.targetId,
              label: rel.type,
              ruleName: rel.ruleName,
              weight: rel.weight,
            },
            classes: [rel.type || ''],
          })
        }
      }
    }

    graphElements.value = [...nodes, ...edges]
  } catch (e: any) {
    console.error('Failed to load graph data:', e)
  }
}

function handleLayoutChange(layout: LayoutType) {
  graphStore.setLayout(layout)
}

function handleSearch(query: string) {
  if (!query || !cyCanvasRef.value) return
  const cy = cyCanvasRef.value.getCy()
  if (!cy) return
  const matches = cy.nodes().filter((n: any) => {
    const label = n.data('label') || ''
    return label.includes(query) || n.id() === query
  })
  if (matches.length === 0) { message.info('未找到匹配节点'); return }
  cy.elements().removeClass('highlighted dimmed')
  matches.addClass('highlighted')
  cy.nodes().not(matches).addClass('dimmed')
  cy.edges().addClass('dimmed')
  cy.animate({ fit: { eles: matches, padding: 100 }, duration: 500 })
  graphStore.selectNode(matches.first().id())
}

function handleFilterChange(type: string) {
  if (!cyCanvasRef.value) return
  const cy = cyCanvasRef.value.getCy()
  if (!cy) return
  if (!type) {
    cy.elements().removeClass('dimmed').style('display', 'element')
  } else {
    cy.edges().forEach((edge: any) => {
      if (edge.data('label') === type) {
        edge.removeClass('dimmed').style('display', 'element')
      } else {
        edge.addClass('dimmed').style('display', 'none')
      }
    })
  }
}

function handleFit() {
  cyCanvasRef.value?.getCy()?.fit()
}

function handleNodeClick(nodeId: string) {
  graphStore.selectNode(nodeId || null)
  selectedNodeData.value = graphElements.value.find(
    (e) => e.data.id === nodeId
  )?.data || null
  selectedEdgeData.value = null
}

function handleEdgeClick(edgeId: string) {
  graphStore.selectEdge(edgeId || null)
  selectedEdgeData.value = graphElements.value.find(
    (e) => e.data.id === edgeId
  )?.data || null
}

function handleOntologyNodeSelect(node: any) {
  if (node.data) {
    graphStore.selectNode(node.data.id)
  }
}

function handleOntologyNodeDrag(_node: any) {}

const analysisType = ref('')
const analysisResults = ref<any>(null)
const analysisLoading = ref(false)
const pathTarget = ref('')
const violationFacts = ref([{ name: 'Radar-DetectionRange', value: 80 }, { name: 'Weapon-ReactionTime', value: 45 }])

function loadViolationPreset(type: string) {
  const presets: Record<string, any[]> = {
    radar: [{ name: 'Radar-DetectionRange', value: 80 }, { name: 'Weapon-ReactionTime', value: 45 }, { name: 'Radar-Resolution', value: 45 }, { name: 'Weapon-Accuracy', value: 0.5 }],
    submarine: [{ name: 'Torpedo-Speed', value: 25 }, { name: 'Submarine-UnderwaterSpeed', value: 28 }, { name: 'Submarine-SilentLevel', value: 150 }],
    tank: [{ name: 'Tank-ArmorThickness', value: 1000 }, { name: 'Tank-MaxSpeed', value: 75 }],
  }
  violationFacts.value = [...(presets[type] || [])]
}

async function runViolationAnalysis() {
  const facts: Record<string, any> = {}
  for (const f of violationFacts.value) { if (f.name) facts[f.name] = f.value }
  if (Object.keys(facts).length === 0) { message.warning('请添加指标数据'); return }
  analysisLoading.value = true
  try {
    const res = await axios.post('/api/graph/analysis/violation', facts)
    analysisResults.value = res.data.code === 200 ? res.data.data : null
  } catch (e: any) { message.error('分析失败'); analysisResults.value = null }
  finally { analysisLoading.value = false }
}

async function runAnalysis(type: string) {
  analysisLoading.value = true
  analysisType.value = type
  try {
    const nodeId = graphStore.selectedNodeId
    switch (type) {
      case 'upstream':
        if (!nodeId) { message.warning('请先选择节点'); return }
        analysisResults.value = (await graphApi.getUpstream(nodeId)).data
        break
      case 'downstream':
        if (!nodeId) { message.warning('请先选择节点'); return }
        analysisResults.value = (await graphApi.getDownstream(nodeId)).data
        break
      case 'path':
        if (!nodeId) { message.warning('请先选择节点作为起点'); return }
        pathTarget.value = ''
        analysisResults.value = [] // trigger empty state with target input
        break
      case 'violation':
        analysisResults.value = (await graphApi.getViolationPaths()).data
        break
      case 'centrality':
        analysisResults.value = (await graphApi.analyzeCentrality()).data
        break
      case 'isolated':
        analysisResults.value = (await graphApi.findIsolated()).data
        break
    }
  } catch (e: any) {
    message.error(e.message || '分析失败')
    analysisResults.value = null
  } finally { analysisLoading.value = false }
}

async function doPathSearch() {
  if (!graphStore.selectedNodeId || !pathTarget.value) return
  analysisLoading.value = true
  try {
    analysisResults.value = (await graphApi.findPath(graphStore.selectedNodeId!, pathTarget.value)).data
  } catch (e: any) {
    message.error(e.message || '路径查询失败')
  } finally { analysisLoading.value = false }
}

function openRelatedRule() {
  if (selectedEdgeData.value?.ruleName) {
    router.push(`/rules?ruleName=${selectedEdgeData.value.ruleName}`)
  }
}

function edgeTypeColor(type: string) {
  const m: Record<string, string> = { directlyAffects: 'red', indirectlyAffects: 'orange', constrainedBy: 'blue' }
  return m[type] || 'default'
}
function edgeTypeLabel(type: string) {
  const m: Record<string, string> = { directlyAffects: '直接影响', indirectlyAffects: '间接影响', constrainedBy: '阈值约束' }
  return m[type] || type
}
const edgeSourceName = computed(() => {
  if (!selectedEdgeData.value) return '-'
  const n = graphElements.value.find((e: any) => e.data.id === selectedEdgeData.value.source)
  return n?.data?.label || selectedEdgeData.value.source?.slice(0, 8) || '-'
})
const edgeTargetName = computed(() => {
  if (!selectedEdgeData.value) return '-'
  const n = graphElements.value.find((e: any) => e.data.id === selectedEdgeData.value.target)
  return n?.data?.label || selectedEdgeData.value.target?.slice(0, 8) || '-'
})
function jumpToNode(nodeId: string) {
  if (!nodeId || !cyCanvasRef.value) return
  const cy = cyCanvasRef.value.getCy()
  if (!cy) return
  const node = cy.getElementById(nodeId)
  if (node.length) {
    cy.animate({ fit: { eles: node, padding: 100 }, duration: 500 })
    graphStore.selectNode(nodeId)
  }
}

function handleRelationshipSave(data: CapabilityRelationship) {
  ontologyApi.saveRelationship(data).then(() => {
    message.success('关系保存成功')
    graphStore.showRelationshipEditor = false
    loadGraphData()
  }).catch((e: any) => {
    message.error('保存失败: ' + e.message)
  })
}

onMounted(() => {
  loadGraphData()
})
</script>

<style scoped>
.knowledge-graph-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
}
.side-panel {
  height: 100%;
  overflow: auto;
  border-left: 1px solid #e8e8e8;
  border-right: 1px solid #e8e8e8;
}
.graph-panel {
  height: 100%;
}
.detail-card {
  height: fit-content;
}
.detail-card :deep(.ant-card-body) {
  max-height: 300px;
  overflow: auto;
}
</style>
