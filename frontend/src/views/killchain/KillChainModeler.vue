<template>
  <div class="modeler-page" @contextmenu.prevent>
    <!-- Toolbar -->
    <div class="modeler-toolbar">
      <a-space>
        <a-button size="small" @click="router.push('/killchain')">← 返回列表</a-button>
        <a-divider type="vertical" />
        <span style="font-weight:bold">{{ taskName }}</span>
        <a-divider type="vertical" />
        <a-button size="small" @click="handleValidate">校验模型</a-button>
        <a-button size="small" @click="handleFit">自适应</a-button>
        <a-tag v-if="edgeCreationMode" color="processing">连线中... (右键取消)</a-tag>
        <a-tag v-if="saveStatus === 'saving'" color="processing">保存中...</a-tag>
        <a-tag v-else-if="saveStatus === 'saved'" color="green">已保存</a-tag>
        <a-tag v-else-if="saveStatus === 'error'" color="red">保存失败</a-tag>
      </a-space>
    </div>

    <a-row :gutter="0" style="flex:1;overflow:hidden">
      <!-- Left: Node Palette -->
      <a-col :span="3" class="side-panel">
        <div class="palette">
          <div class="palette-title">节点库</div>
          <div
            v-for="nt in NODE_TYPES" :key="nt.value"
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, nt)"
          >
            <span class="palette-dot" :style="{background:nt.color, borderRadius: nt.shape.includes('round')||nt.shape==='ellipse'?'50%':'2px'}"></span>
            {{ nt.label }}
          </div>
        </div>
      </a-col>

      <!-- Center: Canvas -->
      <a-col :span="15" style="height:100%">
        <div class="canvas-wrapper" ref="canvasWrapper">
          <div ref="cyContainer" style="width:100%;height:100%"
            @drop="onDrop" @dragover.prevent
          />

          <!-- Floating delete X button near selected node -->
          <div
            v-if="showEdgePanel && edgePanelPos"
            class="node-delete-btn"
            :style="{ left: (edgePanelPos.x - 30) + 'px', top: (edgePanelPos.y - 40) + 'px' }"
            @click.stop="deleteSelectedNode"
          >
            <CloseCircleFilled style="color:#ff4d4f;font-size:20px;cursor:pointer" />
          </div>

          <!-- Floating edge creation panel near selected node -->
          <div
            v-if="showEdgePanel && edgePanelPos"
            class="edge-creation-panel"
            :style="{ left: edgePanelPos.x + 'px', top: edgePanelPos.y + 'px' }"
          >
            <div class="ecp-title">创建连线</div>
            <div
              v-for="et in EDGE_TYPES" :key="et.value"
              class="ecp-item"
              :style="{ borderLeftColor: et.color }"
              @click.stop="startEdgeCreation(et)"
            >
              {{ et.label }}
            </div>
            <div class="ecp-item ecp-cancel" @click.stop="dismissEdgePanel">取消</div>
          </div>
        </div>
      </a-col>

      <!-- Right: Property Panel -->
      <a-col :span="6" class="side-panel">
        <a-card title="属性" size="small" class="prop-card">
          <div v-if="selectedNode">
            <a-form layout="vertical" size="small">
              <a-form-item label="名称"><a-input v-model:value="selectedNode.label" @change="markDirty" /></a-form-item>
              <a-form-item label="类型">
                <a-select v-model:value="selectedNode.type" @change="markDirty">
                  <a-select-option v-for="nt in NODE_TYPES" :key="nt.value" :value="nt.value">{{ nt.label }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="平台/装备">
                <a-select
                  :value="undefined"
                  placeholder="搜索并添加装备..."
                  :filter-option="false"
                  show-search
                  :open="platformDropdownOpen"
                  class="platform-select"
                  style="width:100%"
                  @search="searchIndicators"
                  @dropdownVisibleChange="onPlatformDropdownChange"
                >
                  <template #option="{ value, label }">
                    <span
                      style="display:flex;align-items:center;justify-content:space-between"
                      @mousedown.prevent="onPlatformOptionClick(); togglePlatform(value as string)"
                    >
                      <span>{{ label || value }}</span>
                      <CheckOutlined v-if="selectedPlatforms.includes(value as string)" style="color:#1890ff;font-size:12px" />
                    </span>
                  </template>
                  <a-select-option v-for="ind in indicatorOptions" :key="ind.id" :value="ind.id" :label="ind.name + ' (' + ind.category + ')'">
                    {{ ind.name }} <span style="color:#999;font-size:11px">{{ ind.category }}</span>
                  </a-select-option>
                </a-select>
                <div v-if="selectedPlatforms.length > 0" style="margin-top:6px">
                  <a-tag
                    v-for="pid in selectedPlatforms" :key="pid"
                    closable size="small" color="blue"
                    style="margin:2px"
                    @close="removePlatformTag(pid)"
                  >
                    {{ platformTagName(pid) }}
                  </a-tag>
                </div>
              </a-form-item>
              <a-form-item label="能力指标">
                <a-tree-select
                  v-model:value="capSelectVal"
                  :tree-data="capabilityTreeData"
                  show-search
                  :open="capDropdownOpen"
                  placeholder="搜索并添加能力指标..."
                  :filter-tree-node="filterCapabilityNode"
                  :dropdown-match-select-width="false"
                  dropdown-class-name="cap-tree-dropdown"
                  style="width:100%"
                  @select="onCapSelect"
                  @dropdownVisibleChange="onCapDropdownChange"
                >
                  <template #title="{ value, title }">
                    <span style="display:inline-flex;align-items:center;gap:4px">
                      <CheckOutlined v-if="selectedCapabilities.includes(value as string)" style="color:#1890ff;font-size:11px" />
                      <span>{{ title }}</span>
                    </span>
                  </template>
                </a-tree-select>
                <div v-if="selectedCapabilities.length > 0" style="margin-top:6px">
                  <a-tag v-for="cid in selectedCapabilities" :key="cid" closable size="small" color="green" style="margin:1px"
                    @close="removeCapabilityTag(cid)">{{ capabilityTagName(cid) }}</a-tag>
                </div>
              </a-form-item>
              <a-button type="primary" size="small" @click="applyNodeProps">应用</a-button>
            </a-form>
          </div>
          <div v-else-if="selectedEdge">
            <a-form layout="vertical" size="small">
              <a-form-item label="连线类型">
                <a-select v-model:value="selectedEdge.type" @change="markDirty">
                  <a-select-option v-for="et in EDGE_TYPES" :key="et.value" :value="et.value">{{ et.label }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="标签"><a-input v-model:value="selectedEdge.label" @change="markDirty" /></a-form-item>
              <a-button type="primary" size="small" @click="applyEdgeProps">应用</a-button>
            </a-form>
          </div>
          <a-empty v-else description="点击节点或边" />
        </a-card>

        <a-card title="校验结果" size="small" class="prop-card" style="margin-top:8px">
          <div v-if="validationResults.length === 0 && validated"><a-tag color="green">模型校验通过</a-tag></div>
          <div v-else-if="validationResults.length > 0">
            <div v-for="(v, i) in validationResults" :key="i" style="margin-bottom:4px">
              <a-tag :color="v.level === 'error' ? 'red' : 'orange'" size="small">{{ v.message }}</a-tag>
            </div>
          </div>
          <span v-else style="color:#999;font-size:12px">点击"校验模型"</span>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { CloseCircleFilled, CheckOutlined } from '@ant-design/icons-vue'
import cytoscape, { type Core } from 'cytoscape'
import { killChainApi } from '@/api/killchain'
import { NODE_TYPES, EDGE_TYPES, type KillChainNode, type KillChainEdge } from '@/types/killchain'

const router = useRouter()
const route = useRoute()
const taskId = route.params.id as string
const taskName = ref('')
const saveStatus = ref('')
const dirty = ref(false)
const validated = ref(false)
const validationResults = ref<{ level: string; message: string }[]>([])

const cyContainer = ref<HTMLDivElement>()
const canvasWrapper = ref<HTMLDivElement>()
let cy: Core | null = null

const selectedNode = ref<any>(null)
const selectedEdge = ref<any>(null)

// Platform/equipment search
const selectedPlatforms = ref<string[]>([])
const platformDropdownOpen = ref(false)
const indicatorOptions = ref<any[]>([])
const allIndicators = ref<any[]>([])
const allCapabilities = ref<any[]>([])

// Capability indicator tree
const selectedCapabilities = ref<string[]>([])
const capSelectVal = ref<string>()
const capDropdownOpen = ref(false)
const capabilityTreeData = ref<any[]>([])

async function loadCapabilityTree() {
  try {
    const res = await fetch('/api/ontology/indicators?entityType=INDICATOR')
    const data = await res.json()
    if (data.code === 200) {
      allCapabilities.value = data.data || []
      buildCapabilityTree(data.data || [])
    }
  } catch { /* ignore */ }
}

function buildCapabilityTree(indicators: any[]) {
  const DOMAIN: Record<string,string> = { SurfaceWarfare:'水面作战域', UnderwaterWarfare:'水下作战域', LandWarfare:'陆上作战域', LowAltitudeWarfare:'低空作战域', HighAltitudeWarfare:'高空作战域' }
  const CAT: Record<string,string> = { Ship:'舰艇', Radar:'雷达系统', WeaponSystem:'武器系统', Submarine:'潜艇', UnderwaterDetection:'水下探测', Tank:'坦克', Artillery:'火炮系统', Helicopter:'直升机', UAV:'无人机', Fighter:'歼击机', Bomber:'轰炸机', AWACS:'预警机' }
  const dm = new Map<string, Map<string, any[]>>()
  for (const ind of indicators) {
    const d = ind.domain || 'other', c = ind.category || 'other'
    if (!dm.has(d)) dm.set(d, new Map())
    const cm = dm.get(d)!
    if (!cm.has(c)) cm.set(c, [])
    cm.get(c)!.push(ind)
  }
  capabilityTreeData.value = Array.from(dm.entries()).map(([d, cm]) => ({
    title: DOMAIN[d] || d, value: d, key: d, selectable: false, disabled: true,
    children: Array.from(cm.entries()).map(([c, inds]) => ({
      title: (CAT[c] || c) + ` (${inds.length})`, value: `${d}/${c}`, key: `${d}/${c}`, selectable: false, disabled: true,
      children: inds.map(ind => ({
        title: ind.name + (ind.unit ? ` (${ind.thresholdMin}~${ind.thresholdMax} ${ind.unit})` : ''),
        value: ind.id, key: ind.id, selectable: true,
      })),
    })),
  }))
}

function filterCapabilityNode(input: string, node: any) {
  return node.title?.toLowerCase().includes(input.toLowerCase())
}

function capabilityTagName(id: string): string {
  return allCapabilities.value.find((i: any) => i.id === id)?.name || id.slice(0, 8)
}

let capClicked = false

function onCapSelect(_val: any, node: any) {
  capClicked = true
  const id = node.value
  const idx = selectedCapabilities.value.indexOf(id)
  if (idx >= 0) selectedCapabilities.value.splice(idx, 1)
  else selectedCapabilities.value.push(id)
  capSelectVal.value = undefined
  markDirty()
}

function onCapDropdownChange(open: boolean) {
  if (capClicked) { capClicked = false; nextTick(() => { capDropdownOpen.value = true }); return }
  capDropdownOpen.value = open
}

function removeCapabilityTag(id: string) {
  selectedCapabilities.value = selectedCapabilities.value.filter(c => c !== id)
  markDirty()
}
async function searchIndicators(keyword: string) {
  try {
    const params = new URLSearchParams({ entityType: 'PLATFORM' })
    if (keyword) params.set('keyword', keyword)
    const res = await fetch('/api/ontology/indicators?' + params.toString())
    const data = await res.json()
    if (data.code === 200) {
      indicatorOptions.value = (data.data || []).slice(0, 30)
      if (!keyword) allIndicators.value = data.data || []
    }
  } catch { indicatorOptions.value = [] }
}

function platformTagName(id: string): string {
  return allIndicators.value.find((i: any) => i.id === id)?.name || id.slice(0, 8)
}

function togglePlatform(id: string) {
  if (!id) return
  const idx = selectedPlatforms.value.indexOf(id)
  if (idx >= 0) {
    selectedPlatforms.value.splice(idx, 1)
  } else {
    selectedPlatforms.value.push(id)
  }
  markDirty()
}

let platformOptionClicked = false
function onPlatformOptionClick() { platformOptionClicked = true }
function onPlatformDropdownChange(open: boolean) {
  if (platformOptionClicked) {
    platformOptionClicked = false
    nextTick(() => { platformDropdownOpen.value = true })
    return
  }
  platformDropdownOpen.value = open
}

function removePlatformTag(id: string) {
  selectedPlatforms.value = selectedPlatforms.value.filter(p => p !== id)
  markDirty()
}
function getIndicatorName(id: string): string {
  const found = allIndicators.value.find((i: any) => i.id === id)
  return found ? found.name : id.slice(0, 8)
}
// Edge creation
const edgeCreationMode = ref(false)
const edgeCreationSource = ref('')
const showEdgePanel = ref(false)
const edgePanelPos = ref<{ x: number; y: number } | null>(null)

let saveTimer: any = null

function markDirty() { dirty.value = true }

function dismissEdgePanel() {
  showEdgePanel.value = false
  edgePanelPos.value = null
}

function deleteSelectedNode() {
  if (!cy || !selectedNode.value) return
  cy.getElementById(selectedNode.value.id).remove()
  selectedNode.value = null
  dismissEdgePanel()
  markDirty()
}

async function loadTask() {
  try {
    const res = await killChainApi.getTask(taskId)
    taskName.value = res.data.name
    initCytoscape()
    if (res.data.modelData) {
      try { loadModel(JSON.parse(res.data.modelData)) } catch { /* ignore */ }
    }
  } catch { message.error('加载任务失败') }
}

function initCytoscape() {
  if (!cyContainer.value) return
  cy = cytoscape({
    container: cyContainer.value,
    elements: [],
    style: [
      { selector: 'node', style: { label: 'data(label)', 'font-size': '12px', 'text-valign': 'bottom', 'text-halign': 'center', 'text-margin-y': 8, 'text-wrap': 'wrap', 'text-max-width': '100px', width: 50, height: 50, 'border-width': 2, 'border-color': '#fff', color: '#333' } },
      { selector: 'node[type="reconnaissance"]', style: { 'background-color': '#1890ff', shape: 'round-rectangle' } },
      { selector: 'node[type="detection"]', style: { 'background-color': '#faad14', shape: 'diamond' } },
      { selector: 'node[type="command"]', style: { 'background-color': '#722ed1', shape: 'rectangle' } },
      { selector: 'node[type="control"]', style: { 'background-color': '#eb2f96', shape: 'round-diamond' } },
      { selector: 'node[type="communication"]', style: { 'background-color': '#13c2c2', shape: 'parallelogram' } },
      { selector: 'node[type="strike"]', style: { 'background-color': '#f5222d', shape: 'triangle' } },
      { selector: 'node[type="assessment"]', style: { 'background-color': '#52c41a', shape: 'hexagon' } },
      { selector: 'node:selected', style: { 'border-color': '#1890ff', 'border-width': 3 } },
      { selector: '.edge-creating', style: { 'line-color': '#ff4d4f', width: 2.5, 'line-style': 'dashed', 'target-arrow-color': '#ff4d4f', 'target-arrow-shape': 'triangle' } },
      { selector: 'edge', style: { width: 2, 'line-color': '#999', 'target-arrow-color': '#999', 'target-arrow-shape': 'triangle', 'curve-style': 'unbundled-bezier', label: 'data(label)', 'font-size': '10px', 'edge-text-rotation': 'autorotate', 'text-background-opacity': 0.85, 'text-background-color': '#fff', 'text-margin-y': -6 } },
      { selector: 'edge[type="communication"]', style: { 'line-color': '#1890ff', 'target-arrow-color': '#1890ff' } },
      { selector: 'edge[type="command"]', style: { 'line-color': '#722ed1', 'target-arrow-color': '#722ed1' } },
      { selector: 'edge[type="support"]', style: { 'line-color': '#52c41a', 'target-arrow-color': '#52c41a', 'line-style': 'dashed' } },
      { selector: 'edge[type="feedback"]', style: { 'line-color': '#faad14', 'target-arrow-color': '#faad14', 'line-style': 'dotted' } },
      { selector: '.validation-error', style: { 'border-color': '#f5222d', 'border-width': 4 } },
    ],
    layout: { name: 'cose', idealEdgeLength: 200, nodeOverlap: 30, nodeRepulsion: 10000, gravity: 0.3, padding: 50 },
  })

  cy.on('tap', 'node', (e) => {
    if (edgeCreationMode.value) {
      // Finish edge creation
      finishEdgeCreation(e.target.id())
      return
    }
    const n = e.target
    selectedEdge.value = null
    const plat = n.data('platform') || ''
    const caps = n.data('capabilities') || ''
    selectedNode.value = { id: n.id(), label: n.data('label'), type: n.data('type'), platform: plat, capabilities: caps }
    selectedPlatforms.value = plat ? plat.split(',').filter(Boolean) : []
    selectedCapabilities.value = caps ? caps.split(',').filter(Boolean) : []
    searchIndicators('')
    loadCapabilityTree()
    showEdgePanelNearNode(e.target)
  })

  cy.on('tap', 'edge', (e) => {
    dismissEdgePanel()
    const ed = e.target
    selectedNode.value = null
    selectedEdge.value = { id: ed.id(), source: ed.data('source'), target: ed.data('target'), label: ed.data('label'), type: ed.data('type') || 'communication' }
  })

  cy.on('tap', (e) => {
    if (e.target === cy) {
      selectedNode.value = null; selectedEdge.value = null
      dismissEdgePanel()
    }
  })

  // Listen for right-click on canvas to cancel edge creation
  cy.on('cxttap', () => { cancelEdgeCreation() })

  cy.on('add remove position dragfree', () => { markDirty() })
}

function showEdgePanelNearNode(node: any) {
  if (!canvasWrapper.value) return
  const pos = node.renderedPosition()
  const rect = canvasWrapper.value.getBoundingClientRect()
  showEdgePanel.value = true
  edgePanelPos.value = {
    x: pos.x + 60,
    y: pos.y - 40,
  }
  // Ensure panel stays within canvas
  if (edgePanelPos.value.x + 120 > rect.width) edgePanelPos.value.x = pos.x - 130
  if (edgePanelPos.value.y < 0) edgePanelPos.value.y = 0
}

let creatingEdgeType = ''

function startEdgeCreation(et: typeof EDGE_TYPES[number]) {
  if (!cy || !selectedNode.value) return
  edgeCreationMode.value = true
  edgeCreationSource.value = selectedNode.value.id
  creatingEdgeType = et.value
  showEdgePanel.value = false
  // Change cursor on canvas
  if (cyContainer.value) cyContainer.value.style.cursor = 'crosshair'
}

function finishEdgeCreation(targetId: string) {
  if (!cy || !edgeCreationSource.value || targetId === edgeCreationSource.value) {
    cancelEdgeCreation()
    return
  }
  cy.add({
    group: 'edges',
    data: {
      id: 'edge-' + Date.now(),
      source: edgeCreationSource.value,
      target: targetId,
      label: EDGE_TYPES.find(e => e.value === creatingEdgeType)?.label || '',
      type: creatingEdgeType,
    },
  })
  edgeCreationMode.value = false
  edgeCreationSource.value = ''
  creatingEdgeType = ''
  if (cyContainer.value) cyContainer.value.style.cursor = ''
  markDirty()
}

function cancelEdgeCreation() {
  edgeCreationMode.value = false
  edgeCreationSource.value = ''
  creatingEdgeType = ''
  if (cyContainer.value) cyContainer.value.style.cursor = ''
  dismissEdgePanel()
}

function loadModel(model: any) {
  if (!cy) return
  const nodes = (model.nodes || []).map((n: any) => ({
    data: { id: n.id, label: n.label || n.id, type: n.type, platform: n.platform || '', capabilities: n.capabilities || '' },
    position: n.position || { x: Math.random() * 400, y: Math.random() * 300 },
  }))
  const edges = (model.edges || []).map((e: any) => ({
    data: { id: e.id, source: e.source, target: e.target, label: e.label || '', type: e.type || 'communication' },
  }))
  cy.add([...nodes, ...edges])
  cy.layout({ name: 'preset' }).run()
}

let dragNodeType: any = null
function onDragStart(e: DragEvent, nt: any) { dragNodeType = nt; e.dataTransfer!.effectAllowed = 'move' }
function onDrop(e: DragEvent) {
  if (!cy || !dragNodeType || !canvasWrapper.value) return
  const rect = canvasWrapper.value.getBoundingClientRect()
  const sx = e.clientX - rect.left
  const sy = e.clientY - rect.top
  // Convert screen coords to model coords (account for pan & zoom)
  const pan = cy.pan(), zoom = cy.zoom()
  const x = (sx - pan.x) / zoom
  const y = (sy - pan.y) / zoom
  cy.add({ data: { id: 'node-' + Date.now(), label: dragNodeType.label, type: dragNodeType.value, platform: '' }, position: { x, y } })
  markDirty()
}

watch(dirty, (val) => { if (!val) return; clearTimeout(saveTimer); saveTimer = setTimeout(() => autoSave(), 800) })

async function autoSave() {
  if (!cy) return
  saveStatus.value = 'saving'
  try {
    const nodes = cy.nodes().map(n => ({ id: n.id(), label: n.data('label'), type: n.data('type'), platform: n.data('platform'), capabilities: n.data('capabilities') || '', position: n.position() }))
    const edges = cy.edges().map(e => ({ id: e.id(), source: e.data('source'), target: e.data('target'), label: e.data('label'), type: e.data('type') }))
    await killChainApi.saveModel(taskId, { modelData: JSON.stringify({ nodes, edges, metadata: { name: taskName.value, version: '1.0', updatedAt: new Date().toISOString() } }), name: taskName.value })
    saveStatus.value = 'saved'; dirty.value = false
  } catch { saveStatus.value = 'error' }
}

function applyNodeProps() {
  if (!cy || !selectedNode.value) return
  const platforms = selectedPlatforms.value.join(',')
  const caps = selectedCapabilities.value.join(',')
  cy.getElementById(selectedNode.value.id).data({
    label: selectedNode.value.label,
    type: selectedNode.value.type,
    platform: platforms,
    capabilities: caps,
  })
  selectedNode.value.platform = platforms
  selectedNode.value.capabilities = caps
  markDirty()
}
function applyEdgeProps() {
  if (!cy || !selectedEdge.value) return
  cy.getElementById(selectedEdge.value.id).data({ label: selectedEdge.value.label, type: selectedEdge.value.type })
  markDirty()
}

function handleValidate() {
  if (!cy) return
  validationResults.value = []
  const nodes = cy.nodes(); const edges = cy.edges()
  const connectedIds = new Set<string>()
  edges.forEach(e => { connectedIds.add(e.data('source')); connectedIds.add(e.data('target')) })
  nodes.forEach(n => { if (!connectedIds.has(n.id())) validationResults.value.push({ level: 'warning', message: `孤立节点: ${n.data('label') || n.id()}` }) })
  const adj = new Map<string, string[]>()
  edges.forEach(e => { const s = e.data('source'), t = e.data('target'); if (!adj.has(s)) adj.set(s, []); adj.get(s)!.push(t) })
  const visited = new Set<string>(); const recStack = new Set<string>()
  function hasCycle(id: string): boolean {
    visited.add(id); recStack.add(id)
    for (const next of (adj.get(id) || [])) { if (!visited.has(next)) { if (hasCycle(next)) return true } else if (recStack.has(next)) return true }
    recStack.delete(id); return false
  }
  for (const n of nodes) { if (!visited.has(n.id()) && hasCycle(n.id())) { validationResults.value.push({ level: 'error', message: '检测到循环依赖' }); break } }
  validated.value = true
}
function handleFit() { cy?.fit() }

function onKeyDown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  if ((e.key === 'Delete' || e.key === 'Backspace') && selectedNode.value && !edgeCreationMode.value) {
    deleteSelectedNode()
  }
}

onMounted(() => { loadTask(); window.addEventListener('keydown', onKeyDown) })
onUnmounted(() => { clearTimeout(saveTimer); window.removeEventListener('keydown', onKeyDown); cy?.destroy() })
</script>

<style scoped>
.modeler-page { display:flex; flex-direction:column; height:calc(100vh - 120px); }
.modeler-toolbar { padding:6px 12px; background:#fafafa; border-bottom:1px solid #e8e8e8; display:flex; align-items:center; }
.side-panel { height:100%; overflow:auto; border-left:1px solid #e8e8e8; background:#fafafa; }
.canvas-wrapper { width:100%; height:100%; position:relative; background:#f5f7fa; }
.palette { padding:8px; background:#f0f2f5; }
.palette-title { font-weight:600; margin-bottom:8px; font-size:13px; color:#333; }
.palette-item { display:flex; align-items:center; gap:6px; padding:6px 8px; margin-bottom:4px; border:1px solid #e8e8e8; border-radius:4px; cursor:grab; font-size:12px; transition:background .2s; }
.palette-item:hover { background:#f0f5ff; }
.palette-dot { width:16px; height:16px; display:inline-block; border:1px solid rgba(0,0,0,.1); flex-shrink:0; }
.prop-card { height:fit-content; }

.edge-creation-panel {
  position: absolute; z-index: 1000; background: #fff;
  border: 1px solid #d9d9d9; border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,.12); padding: 6px 0; min-width: 100px;
}
.ecp-title { padding: 4px 12px; font-size: 12px; color: #999; border-bottom: 1px solid #f0f0f0; margin-bottom: 4px; }
.ecp-item { padding: 6px 12px 6px 8px; margin: 2px 4px; font-size: 13px; cursor: pointer; border-left: 3px solid; border-radius: 2px; transition: background .15s; }
.ecp-item:hover { background: #f0f5ff; }
.ecp-cancel { border-left-color: #ccc !important; color: #999; }
.platform-select :deep(.ant-select-selection-item) { background: #e6f7ff; border-color: #91d5ff; border-radius: 4px; }
.platform-select :deep(.ant-select-selection-item-content) { font-weight: 500; }
</style>

<style>
.cap-tree-dropdown { min-width: 340px !important; }
</style>
