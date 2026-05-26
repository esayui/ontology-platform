<template>
  <div class="modeler-page">
    <!-- Toolbar -->
    <div class="modeler-toolbar">
      <a-space>
        <a-button size="small" @click="router.push('/killchain')">← 返回列表</a-button>
        <a-divider type="vertical" />
        <span style="font-weight:bold">{{ taskName }}</span>
        <a-divider type="vertical" />
        <a-button size="small" @click="handleValidate">校验模型</a-button>
        <a-button size="small" @click="handleFit">自适应</a-button>
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
              <a-form-item label="平台/装备"><a-input v-model:value="selectedNode.platform" @change="markDirty" placeholder="关联的装备名称" /></a-form-item>
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

        <!-- Validation -->
        <a-card title="校验结果" size="small" class="prop-card" style="margin-top:8px">
          <div v-if="validationResults.length === 0 && validated">
            <a-tag color="green">模型校验通过</a-tag>
          </div>
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
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

let saveTimer: any = null

function markDirty() { dirty.value = true }

async function loadTask() {
  try {
    const res = await killChainApi.getTask(taskId)
    taskName.value = res.data.name
    initCytoscape()
    if (res.data.modelData) {
      try {
        const model = JSON.parse(res.data.modelData)
        loadModel(model)
      } catch { /* ignore */ }
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
      { selector: 'edge', style: { width: 2, 'line-color': '#999', 'target-arrow-color': '#999', 'target-arrow-shape': 'triangle', 'curve-style': 'bezier', label: 'data(label)', 'font-size': '10px', 'edge-text-rotation': 'autorotate', 'text-background-opacity': 0.85, 'text-background-color': '#fff', 'text-margin-y': -6 } },
      { selector: 'edge[type="communication"]', style: { 'line-color': '#1890ff', 'target-arrow-color': '#1890ff' } },
      { selector: 'edge[type="command"]', style: { 'line-color': '#722ed1', 'target-arrow-color': '#722ed1' } },
      { selector: 'edge[type="support"]', style: { 'line-color': '#52c41a', 'target-arrow-color': '#52c41a', 'line-style': 'dashed' } },
      { selector: 'edge[type="feedback"]', style: { 'line-color': '#faad14', 'target-arrow-color': '#faad14', 'line-style': 'dotted' } },
      { selector: '.validation-error', style: { 'border-color': '#f5222d', 'border-width': 4 } },
    ],
    layout: { name: 'cose', idealEdgeLength: 200, nodeOverlap: 30, nodeRepulsion: 10000, gravity: 0.3, padding: 50 },
  })

  cy.on('tap', 'node', (e) => {
    const n = e.target
    selectedEdge.value = null
    selectedNode.value = { id: n.id(), label: n.data('label'), type: n.data('type'), platform: n.data('platform') || '' }
  })
  cy.on('tap', 'edge', (e) => {
    const ed = e.target
    selectedNode.value = null
    selectedEdge.value = { id: ed.id(), source: ed.data('source'), target: ed.data('target'), label: ed.data('label'), type: ed.data('type') || 'communication' }
  })
  cy.on('tap', (e) => {
    if (e.target === cy) { selectedNode.value = null; selectedEdge.value = null }
  })
  // Auto-save on node/edge changes
  cy.on('add remove position dragfree', () => { markDirty() })
}

function loadModel(model: any) {
  if (!cy) return
  const nodes = (model.nodes || []).map((n: any) => ({
    data: { id: n.id, label: n.label || n.id, type: n.type, platform: n.platform || '' },
    position: n.position || { x: Math.random() * 400, y: Math.random() * 300 },
  }))
  const edges = (model.edges || []).map((e: any) => ({
    data: { id: e.id, source: e.source, target: e.target, label: e.label || '', type: e.type || 'communication' },
  }))
  cy.add([...nodes, ...edges])
  cy.layout({ name: 'preset' }).run()
}

// ---- Drag from palette ----
let dragNodeType: any = null
function onDragStart(e: DragEvent, nt: any) {
  dragNodeType = nt
  e.dataTransfer!.effectAllowed = 'move'
}
function onDrop(e: DragEvent) {
  if (!cy || !dragNodeType || !canvasWrapper.value) return
  const rect = canvasWrapper.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top + canvasWrapper.value.scrollTop
  const id = 'node-' + Date.now()
  cy.add({
    data: { id, label: dragNodeType.label, type: dragNodeType.value, platform: '' },
    position: { x, y },
  })
  cy.layout({ name: 'cose', fit: false, animate: true, animationDuration: 300 }).run()
  markDirty()
}

// ---- Save ----
watch(dirty, (val) => {
  if (!val) return
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => autoSave(), 800)
})

async function autoSave() {
  if (!cy) return
  saveStatus.value = 'saving'
  try {
    const nodes = cy.nodes().map(n => ({ id: n.id(), label: n.data('label'), type: n.data('type'), platform: n.data('platform'), position: n.position() }))
    const edges = cy.edges().map(e => ({ id: e.id(), source: e.data('source'), target: e.data('target'), label: e.data('label'), type: e.data('type') }))
    const model = { nodes, edges, metadata: { name: taskName.value, version: '1.0', createdAt: '', updatedAt: new Date().toISOString() } }
    await killChainApi.saveModel(taskId, { modelData: JSON.stringify(model), name: taskName.value })
    saveStatus.value = 'saved'; dirty.value = false
  } catch { saveStatus.value = 'error' }
}

// ---- Property apply ----
function applyNodeProps() {
  if (!cy || !selectedNode.value) return
  const n = cy.getElementById(selectedNode.value.id)
  n.data('label', selectedNode.value.label)
  n.data('type', selectedNode.value.type)
  n.data('platform', selectedNode.value.platform)
  markDirty()
}
function applyEdgeProps() {
  if (!cy || !selectedEdge.value) return
  const e = cy.getElementById(selectedEdge.value.id)
  e.data('label', selectedEdge.value.label)
  e.data('type', selectedEdge.value.type)
  markDirty()
}

// ---- Validation ----
function handleValidate() {
  if (!cy) return
  validationResults.value = []
  const nodes = cy.nodes()
  const edges = cy.edges()

  // Isolated nodes
  const connectedIds = new Set<string>()
  edges.forEach(e => { connectedIds.add(e.data('source')); connectedIds.add(e.data('target')) })
  nodes.forEach(n => { if (!connectedIds.has(n.id())) validationResults.value.push({ level: 'warning', message: `孤立节点: ${n.data('label') || n.id()}` }) })

  // DAG cycle check (simple DFS)
  const adj = new Map<string, string[]>()
  edges.forEach(e => {
    const s = e.data('source'), t = e.data('target')
    if (!adj.has(s)) adj.set(s, [])
    adj.get(s)!.push(t)
  })
  const visited = new Set<string>()
  const recStack = new Set<string>()
  function hasCycle(id: string): boolean {
    visited.add(id); recStack.add(id)
    for (const next of (adj.get(id) || [])) {
      if (!visited.has(next)) { if (hasCycle(next)) return true }
      else if (recStack.has(next)) return true
    }
    recStack.delete(id); return false
  }
  for (const n of nodes) { if (!visited.has(n.id()) && hasCycle(n.id())) { validationResults.value.push({ level: 'error', message: '检测到循环依赖' }); break } }

  validated.value = true
}

function handleFit() { cy?.fit() }

onMounted(() => loadTask())
onUnmounted(() => { clearTimeout(saveTimer); cy?.destroy() })
</script>

<style scoped>
.modeler-page { display:flex; flex-direction:column; height:calc(100vh - 120px); }
.modeler-toolbar { padding:6px 12px; background:#fafafa; border-bottom:1px solid #e8e8e8; display:flex; align-items:center; }
.side-panel { height:100%; overflow:auto; border-left:1px solid #e8e8e8; }
.canvas-wrapper { width:100%; height:100%; position:relative; }
.palette { padding:8px; }
.palette-title { font-weight:600; margin-bottom:8px; font-size:13px; color:#333; }
.palette-item { display:flex; align-items:center; gap:6px; padding:6px 8px; margin-bottom:4px; border:1px solid #e8e8e8; border-radius:4px; cursor:grab; font-size:12px; transition:background .2s; }
.palette-item:hover { background:#f0f5ff; }
.palette-dot { width:16px; height:16px; display:inline-block; border:1px solid rgba(0,0,0,.1); flex-shrink:0; }
.prop-card { height:fit-content; }
</style>
