<template>
  <div class="ontology-manager">
    <!-- Hidden context menu -->
    <div
      v-if="ctxVisible"
      class="ctx-menu"
      :style="{ left: ctxX + 'px', top: ctxY + 'px' }"
      @click.stop
    >
      <div class="ctx-item" v-if="ctxNode?.children" @click="ctxAddCategory">+ 添加装备分类</div>
      <div class="ctx-item" v-if="ctxNode?.children" @click="ctxAddIndicator">+ 添加能力指标</div>
      <div class="ctx-item" v-if="!ctxNode?.children" @click="ctxAddIndicator">+ 添加能力指标</div>
      <div class="ctx-divider" v-if="ctxNode" />
      <div class="ctx-item danger" v-if="ctxNode" @click="ctxDelete">
        删除「{{ ctxNode?.title }}」
      </div>
      <div class="ctx-item" @click="ctxVisible = false">取消</div>
    </div>

    <a-row :gutter="16" style="height: 100%">
      <!-- Left: Tree -->
      <a-col :span="6">
        <a-card title="本体分类树" size="small"
          :body-style="{ padding: '8px', maxHeight: 'calc(100vh - 160px)', overflow: 'auto' }">
          <a-space size="small" style="margin-bottom: 8px" wrap>
            <a-button size="small" type="primary" @click="showAddDomain">+ 添加域</a-button>
            <a-button size="small" @click="expandAll">展开</a-button>
            <a-button size="small" @click="collapseAll">折叠</a-button>
          </a-space>

          <a-spin :spinning="loading">
            <a-tree
              v-if="treeData.length > 0"
              :tree-data="treeData"
              v-model:expandedKeys="expandedKeys"
              :selected-keys="selectedKeys"
              @select="onTreeSelect"
              @right-click="onRightClick"
            >
              <template #title="{ title }">
                <span style="font-size: 13px; user-select: none">{{ title }}</span>
              </template>
            </a-tree>
            <a-empty v-else-if="!loading" description="暂无数据" />
          </a-spin>
        </a-card>
      </a-col>

      <!-- Right: Detail Form + Relationships -->
      <a-col :span="18">
        <a-card :title="selectedIndicator ? selectedIndicator.name : '指标详情'" size="small">
          <template #extra>
            <a-space>
              <a-button size="small" @click="loadTree">刷新</a-button>
              <a-button size="small" @click="handleSync">同步到Neo4j</a-button>
            </a-space>
          </template>

          <div v-if="selectedIndicator" class="detail-area">
            <a-form :model="editForm" layout="vertical" size="small">
              <a-row :gutter="12">
                <a-col :span="8">
                  <a-form-item label="名称">
                    <a-input v-model:value="editForm.name" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="IRI">
                    <a-input v-model:value="editForm.iri" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="单位">
                    <a-input v-model:value="editForm.unit" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="作战域">
                    <a-input v-model:value="editForm.domain" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="装备类别">
                    <a-input v-model:value="editForm.category" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="数据类型">
                    <a-input v-model:value="editForm.dataType" />
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item label="阈值下限">
                    <a-input-number v-model:value="editForm.thresholdMin" style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item label="阈值上限">
                    <a-input-number v-model:value="editForm.thresholdMax" style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="描述">
                    <a-textarea v-model:value="editForm.description" :rows="2" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-space>
                <a-button type="primary" size="small" @click="handleUpdateIndicator" :loading="saving">
                  更新
                </a-button>
                <a-button size="small" @click="resetForm">重置</a-button>
              </a-space>
            </a-form>

            <a-divider style="margin: 12px 0">关联关系</a-divider>
            <a-space style="margin-bottom: 8px">
              <a-button size="small" type="primary" @click="showAddRel">+ 添加关系</a-button>
            </a-space>
            <a-table
              :columns="relColumns"
              :data-source="relatedRels"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <a-select
                  v-if="column.key === 'type'"
                  :value="record.relationshipType"
                  size="small"
                  style="width: 110px"
                  @change="(v: string) => { record.relationshipType = v; handleUpdateRel(record) }"
                >
                  <a-select-option value="directlyAffects">直接影响</a-select-option>
                  <a-select-option value="indirectlyAffects">间接影响</a-select-option>
                  <a-select-option value="constrainedBy">阈值约束</a-select-option>
                </a-select>
                <a-input-number
                  v-else-if="column.key === 'weight'"
                  :value="record.weight" size="small" :min="0" :max="1" :step="0.1"
                  style="width: 70px"
                  @change="(v: number) => { record.weight = v; handleUpdateRel(record) }"
                />
                <a-input-number
                  v-else-if="column.key === 'priority'"
                  :value="record.priority" size="small" :min="0" :max="100"
                  style="width: 60px"
                  @change="(v: number) => { record.priority = v; handleUpdateRel(record) }"
                />
                <a-switch
                  v-else-if="column.key === 'enabled'"
                  :checked="record.enabled" size="small"
                  @change="(v: boolean) => { record.enabled = v; handleUpdateRel(record) }"
                />
                <span v-else-if="column.key === 'action'">
                  <a-popconfirm
                    title="确定删除11?"
                    ok-text="是"
                    cancel-text="否"
                    @confirm="handleDeleteRel(record.id)"
                  >
                    <a-button size="small" danger>删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </a-table>
          </div>

          <a-empty v-else description="请从左侧树中选择一个指标" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Add Domain/Category Dialog -->
    <a-modal v-model:open="dialogVisible" :title="dialogTitle" @ok="handleDialogOk" width="400px">
      <a-form layout="vertical" size="small">
        <a-form-item label="名称" v-if="dialogType === 'domain'">
          <a-input v-model:value="dialogForm.name" placeholder="如: 网络作战域" />
        </a-form-item>
        <a-form-item label="英文Key" v-if="dialogType === 'domain'">
          <a-input v-model:value="dialogForm.domainKey" placeholder="如: CyberWarfare" />
        </a-form-item>
        <a-form-item label="所属域" v-if="dialogType === 'category'">
          <a-input v-model:value="dialogForm.domain" disabled />
        </a-form-item>
        <a-form-item label="分类名称" v-if="dialogType === 'category'">
          <a-input v-model:value="dialogForm.categoryName" placeholder="如: 电子战系统" />
        </a-form-item>
        <a-form-item label="英文Key" v-if="dialogType === 'category'">
          <a-input v-model:value="dialogForm.categoryKey" placeholder="如: ElectronicWarfare" />
        </a-form-item>
        <a-form-item label="所属域" v-if="dialogType === 'indicator'">
          <a-input v-model:value="dialogForm.indDomain" disabled />
        </a-form-item>
        <a-form-item label="所属分类" v-if="dialogType === 'indicator'">
          <a-input v-model:value="dialogForm.indCategory" disabled />
        </a-form-item>
        <a-form-item label="指标名称" v-if="dialogType === 'indicator'">
          <a-input v-model:value="dialogForm.indName" placeholder="如: 雷达探测距离" />
        </a-form-item>
        <a-form-item label="英文Key" v-if="dialogType === 'indicator'">
          <a-input v-model:value="dialogForm.indKey" placeholder="如: Radar-DetectionRange" />
        </a-form-item>
        <a-form-item label="单位" v-if="dialogType === 'indicator'">
          <a-input v-model:value="dialogForm.indUnit" placeholder="如: 千米" />
        </a-form-item>
        <a-row :gutter="8" v-if="dialogType === 'indicator'">
          <a-col :span="12">
            <a-form-item label="阈值下限">
              <a-input-number v-model:value="dialogForm.indMin" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="阈值上限">
              <a-input-number v-model:value="dialogForm.indMax" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- Add Relationship Dialog -->
    <a-modal v-model:open="relDialogVisible" title="添加关系" @ok="handleAddRel" width="500px">
      <a-form layout="vertical" size="small">
        <a-form-item label="源指标">
          <a-select
            v-model:value="relForm.sourceIndicatorId"
            show-search
            placeholder="选择源指标"
            :filter-option="filterOption"
            style="width: 100%"
          >
            <a-select-option v-for="ind in allIndicators" :key="ind.id" :value="ind.id">
              {{ ind.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="目标指标">
          <a-select
            v-model:value="relForm.targetIndicatorId"
            show-search
            placeholder="选择目标指标"
            :filter-option="filterOption"
            style="width: 100%"
          >
            <a-select-option v-for="ind in allIndicators" :key="ind.id" :value="ind.id">
              {{ ind.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关系类型">
          <a-select v-model:value="relForm.relationshipType" style="width: 100%">
            <a-select-option value="directlyAffects">直接影响</a-select-option>
            <a-select-option value="indirectlyAffects">间接影响</a-select-option>
            <a-select-option value="constrainedBy">阈值约束</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联规则">
          <a-select v-model:value="relForm.droolsRuleName" placeholder="选择规则(可选)" allow-clear style="width: 100%">
            <a-select-option v-for="rule in activeRules" :key="rule.ruleName" :value="rule.ruleName"
              :label="rule.ruleName"
            >
              <div>
                <div>{{ rule.ruleName }}</div>
                <div style="font-size:11px;color:#999;white-space:normal" v-if="rule.description">{{ rule.description }}</div>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="8">
          <a-col :span="8">
            <a-form-item label="权重">
              <a-input-number v-model:value="relForm.weight" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="优先级">
              <a-input-number v-model:value="relForm.priority" :min="0" :max="100" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="启用">
              <a-switch v-model:checked="relForm.enabled" style="margin-top: 8px" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import axios from 'axios'

const DOMAIN_NAMES: Record<string, string> = {
  SurfaceWarfare: '水面作战域', UnderwaterWarfare: '水下作战域',
  LandWarfare: '陆上作战域', LowAltitudeWarfare: '低空作战域',
  HighAltitudeWarfare: '高空作战域',
}
const CATEGORY_NAMES: Record<string, string> = {
  Ship: '舰艇', Radar: '雷达系统', WeaponSystem: '武器系统',
  Submarine: '潜艇', UnderwaterDetection: '水下探测',
  Tank: '坦克', Artillery: '火炮系统',
  Helicopter: '直升机', UAV: '无人机',
  Fighter: '歼击机', Bomber: '轰炸机', AWACS: '预警机',
}

// ---- Tree ----
const loading = ref(false)
const treeData = ref<any[]>([])
const expandedKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const allIndicators = ref<any[]>([])

function buildTree(indicators: any[]): any[] {
  allIndicators.value = indicators
  const domainMap = new Map<string, Map<string, any[]>>()
  for (const ind of indicators) {
    const d = ind.domain || '未分类'
    const c = ind.category || '未分类'
    if (!domainMap.has(d)) domainMap.set(d, new Map())
    const cm = domainMap.get(d)!
    if (!cm.has(c)) cm.set(c, [])
    cm.get(c)!.push(ind)
  }
  const keys: string[] = []
  const result: any[] = []
  for (const [domain, catMap] of domainMap) {
    keys.push(domain)
    const catNodes: any[] = []
    for (const [category, inds] of catMap) {
      const ck = `${domain}|${category}`
      keys.push(ck)
      catNodes.push({
        key: ck,
        title: `${CATEGORY_NAMES[category] || category} (${inds.length}个)`,
        domain, category,
        children: inds.map((ind) => ({
          key: ind.id,
          title: ind.name,
          isLeaf: true,
          indicator: ind,
        })),
      })
    }
    result.push({
      key: domain,
      title: `${DOMAIN_NAMES[domain] || domain} (${catNodes.length}类)`,
      domain,
      children: catNodes,
    })
  }
  expandedKeys.value = keys
  return result
}

async function loadTree() {
  loading.value = true
  try {
    const res = await axios.get('/api/ontology/indicators')
    if (res.data.code === 200) {
      treeData.value = buildTree(res.data.data)
    }
  } catch (e: any) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function expandAll() {
  const keys: string[] = []
  for (const d of treeData.value) {
    keys.push(d.key)
    if (d.children) keys.push(...d.children.map((c: any) => c.key))
  }
  expandedKeys.value = keys
}
function collapseAll() { expandedKeys.value = [] }

async function handleSync() {
  try {
    await axios.post('/api/ontology/sync')
    message.success('同步完成')
  } catch { message.error('同步失败') }
}

// ---- Right-click context menu ----
const ctxVisible = ref(false)
const ctxX = ref(0)
const ctxY = ref(0)
const ctxNode = ref<any>(null)

function onRightClick({ event, node }: any) {
  event.preventDefault()
  ctxNode.value = node
  ctxX.value = event.clientX
  ctxY.value = event.clientY
  ctxVisible.value = true
  // Click outside to close
  nextTick(() => {
    document.addEventListener('click', closeCtx, { once: true })
  })
}
function closeCtx() { ctxVisible.value = false }

// ---- Add dialogs ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref<'domain' | 'category' | 'indicator'>('domain')
const dialogForm = ref<Record<string, any>>({
  name: '', domainKey: '', domain: '', categoryName: '', categoryKey: '',
  indDomain: '', indCategory: '', indName: '', indKey: '', indUnit: '', indMin: 0, indMax: 100,
})

function showAddDomain() {
  dialogType.value = 'domain'
  dialogTitle.value = '添加作战域'
  dialogForm.value = { name: '', domainKey: '' }
  dialogVisible.value = true
}

function ctxAddCategory() {
  closeCtx()
  const node = ctxNode.value
  dialogType.value = 'category'
  dialogTitle.value = '添加装备分类'
  dialogForm.value = { domain: node.domain || node.key, categoryName: '', categoryKey: '' }
  dialogVisible.value = true
}

function ctxAddIndicator() {
  closeCtx()
  const node = ctxNode.value
  dialogType.value = 'indicator'
  dialogTitle.value = '添加能力指标'
  // node could be domain, category, or indicator
  let domainKey = ''
  let categoryKey = ''
  if (node.children && node.children.length > 0 && node.children[0]?.children) {
    // Domain level
    domainKey = node.domain || node.key
  } else if (node.children) {
    // Category level
    domainKey = node.domain
    categoryKey = node.category
  } else if (node.indicator) {
    // Indicator level - add sibling
    domainKey = node.indicator.domain
    categoryKey = node.indicator.category
  }
  dialogForm.value = {
    indDomain: DOMAIN_NAMES[domainKey] || domainKey,
    indCategory: CATEGORY_NAMES[categoryKey] || categoryKey,
    domainKey, categoryKey,
    indName: '', indKey: '', indUnit: '', indMin: 0, indMax: 100,
  }
  dialogVisible.value = true
}

async function handleDialogOk() {
  try {
    if (dialogType.value === 'indicator') {
      const f = dialogForm.value
      const body = {
        id: '', iri: `http://www.ontology.org/capability#${f.indKey || f.indName}`,
        name: f.indName, domain: f.domainKey, category: f.categoryKey,
        unit: f.indUnit, thresholdMin: f.indMin, thresholdMax: f.indMax,
        description: `${f.indName} (${f.indMin}~${f.indMax} ${f.indUnit})`,
      }
      await axios.post('/api/ontology/indicator', body)
      message.success('指标创建成功')
      await loadTree()
    } else if (dialogType.value === 'category') {
      const f = dialogForm.value
      // Create a placeholder indicator to establish the category
      const body = {
        id: '', iri: `http://www.ontology.org/capability#${f.categoryKey || f.categoryName}`,
        name: `${f.categoryName}示例指标`, domain: f.domain,
        category: f.categoryKey || f.categoryName,
        unit: '', thresholdMin: 0, thresholdMax: 100,
        description: `自动创建的${f.categoryName}分类示例指标`,
      }
      await axios.post('/api/ontology/indicator', body)
      message.success('分类创建成功')
      await loadTree()
    } else if (dialogType.value === 'domain') {
      const f = dialogForm.value
      const body = {
        id: '', iri: `http://www.ontology.org/capability#${f.domainKey || f.name}`,
        name: `${f.name}示例指标`, domain: f.domainKey || f.name,
        category: 'General',
        unit: '', thresholdMin: 0, thresholdMax: 100,
        description: `自动创建的${f.name}域示例指标`,
      }
      await axios.post('/api/ontology/indicator', body)
      message.success('作战域创建成功')
      await loadTree()
    }
    dialogVisible.value = false
  } catch (e: any) {
    message.error('创建失败: ' + (e.response?.data?.message || e.message))
  }
}

async function ctxDelete() {
  closeCtx()
  const node = ctxNode.value
  if (!node) return

  const title = node.title || node.key
  if (!confirm(`确认删除「${title}」及其下所有内容?`)) return

  try {
    if (node.indicator) {
      // Delete single indicator
      await axios.delete(`/api/ontology/indicator/${node.indicator.id}`)
      message.success('指标已删除')
    } else if (node.domain && node.category) {
      // Delete category: delete all indicators under this category
      const domain = node.domain
      const category = node.category
      const ids = (allIndicators.value || [])
        .filter((i: any) => i.domain === domain && i.category === category)
        .map((i: any) => i.id)
      for (const id of ids) {
        await axios.delete(`/api/ontology/indicator/${id}`).catch(() => {})
      }
      message.success(`已删除分类及 ${ids.length} 个指标`)
    } else {
      // Delete domain
      const domain = node.domain || node.key
      const ids = (allIndicators.value || [])
        .filter((i: any) => i.domain === domain)
        .map((i: any) => i.id)
      for (const id of ids) {
        await axios.delete(`/api/ontology/indicator/${id}`).catch(() => {})
      }
      message.success(`已删除域及 ${ids.length} 个指标`)
    }
    selectedIndicator.value = null
    relatedRels.value = []
    await loadTree()
  } catch (e: any) {
    message.error('删除失败')
  }
}

// ---- Indicator detail & editing ----
const selectedIndicator = ref<any>(null)
const saving = ref(false)

interface EditForm {
  name: string; iri: string; unit: string; domain: string; category: string
  dataType: string; thresholdMin: number; thresholdMax: number; description: string
}
const editForm = ref<EditForm>({
  name: '', iri: '', unit: '', domain: '', category: '',
  dataType: '', thresholdMin: 0, thresholdMax: 0, description: '',
})

function onTreeSelect(keys: (string | number)[], info: any) {
  selectedKeys.value = keys as string[]
  const ind = info.node?.indicator
  if (ind) {
    selectedIndicator.value = ind
    editForm.value = {
      name: ind.name, iri: ind.iri, unit: ind.unit || '',
      domain: ind.domain, category: ind.category,
      dataType: ind.dataType || '', thresholdMin: ind.thresholdMin ?? 0,
      thresholdMax: ind.thresholdMax ?? 0, description: ind.description || '',
    }
    loadRelationships(ind.id)
  }
}

function resetForm() {
  if (selectedIndicator.value) {
    const ind = selectedIndicator.value
    editForm.value = {
      name: ind.name, iri: ind.iri, unit: ind.unit || '',
      domain: ind.domain, category: ind.category,
      dataType: ind.dataType || '', thresholdMin: ind.thresholdMin ?? 0,
      thresholdMax: ind.thresholdMax ?? 0, description: ind.description || '',
    }
  }
}

async function handleUpdateIndicator() {
  saving.value = true
  try {
    const body = {
      id: selectedIndicator.value.id,
      ...editForm.value,
    }
    await axios.put('/api/ontology/indicator', body)
    message.success('指标更新成功')
    // Refresh tree and selected
    await loadTree()
    // Update selectedIndicator ref
    const found = allIndicators.value.find((i: any) => i.id === selectedIndicator.value.id)
    if (found) selectedIndicator.value = found
  } catch (e: any) {
    message.error('更新失败: ' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

// ---- Relationships ----
const relatedRels = ref<any[]>([])
const relColumns = [
  { title: '关系类型', key: 'type', width: 120 },
  { title: '目标指标ID', dataIndex: 'targetIndicatorId', ellipsis: true },
  { title: '权重', key: 'weight', width: 80 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '规则名', dataIndex: 'droolsRuleName', ellipsis: true },
  { title: '启用', key: 'enabled', width: 60 },
  { title: '操作', key: 'action', width: 80 },
]

async function loadRelationships(indicatorId: string) {
  try {
    const res = await axios.get(`/api/ontology/relationships/indicator/${indicatorId}`)
    if (res.data.code === 200) {
      relatedRels.value = res.data.data || []
    }
  } catch { relatedRels.value = [] }
}

async function handleUpdateRel(record: any) {
  try {
    await axios.put('/api/ontology/relationship', record)
  } catch { /* ignore */ }
}

async function handleDeleteRel(id: string) {
  try {
    console.log(id)
    await axios.delete(`/api/ontology/relationship/${id}`)
    message.success('关系已删除')
    if (selectedIndicator.value) loadRelationships(selectedIndicator.value.id)
  } catch (e: any) { message.error('删除失败') }
}

// ---- Add Relationship Dialog ----
const relDialogVisible = ref(false)
const activeRules = ref<any[]>([])
const relForm = ref({
  sourceIndicatorId: '',
  targetIndicatorId: '',
  relationshipType: 'directlyAffects',
  weight: 0.8,
  priority: 5,
  enabled: true,
  droolsRuleName: '',
  influenceDirection: 'forward',
})

async function showAddRel() {
  relForm.value.sourceIndicatorId = selectedIndicator.value?.id || ''
  relForm.value.targetIndicatorId = ''
  relForm.value.droolsRuleName = ''
  // Fetch active rules for dropdown
  try {
    const res = await axios.get('/api/rules/active')
    if (res.data.code === 200) {
      activeRules.value = res.data.data || []
    }
  } catch { activeRules.value = [] }
  relDialogVisible.value = true
}

async function handleAddRel() {
  if (!relForm.value.sourceIndicatorId || !relForm.value.targetIndicatorId) {
    message.warning('请选择源和目标指标')
    return
  }
  try {
    const body = {
      id: (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : 'rel-' + Date.now() + '-' + Math.random().toString(36).slice(2)),
      ...relForm.value,
    }
    await axios.post('/api/ontology/relationship', body)
    message.success('关系创建成功')
    relDialogVisible.value = false
    if (selectedIndicator.value) loadRelationships(selectedIndicator.value.id)
  } catch (e: any) {
    message.error('创建失败: ' + (e.response?.data?.message || e.message))
  }
}

function filterOption(input: string, option: any) {
  return option.children?.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// ---- Init ----
onMounted(() => {
  loadTree()
  document.addEventListener('click', () => { ctxVisible.value = false })
})
</script>

<style scoped>
.ontology-manager { height: calc(100vh - 160px); }
.detail-area { max-height: calc(100vh - 300px); overflow: auto; }

.ctx-menu {
  position: fixed;
  z-index: 1050;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  min-width: 160px;
  padding: 4px 0;
}
.ctx-item {
  padding: 6px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}
.ctx-item:hover { background: #f0f5ff; }
.ctx-item.danger { color: #ff4d4f; }
.ctx-item.danger:hover { background: #fff1f0; }
.ctx-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 4px 0;
}
</style>
