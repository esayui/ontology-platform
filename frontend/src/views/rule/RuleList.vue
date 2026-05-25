<template>
  <div class="rule-list-page">
    <a-card title="规则管理">
      <template #extra>
        <a-space>
          <a-button type="primary" @click="router.push('/rules/editor')">新建规则</a-button>
          <a-button @click="router.push('/rules/test')">规则测试台</a-button>
        </a-space>
      </template>

      <a-row :gutter="12" style="margin-bottom: 16px">
        <a-col :span="8">
          <a-input-search v-model:value="keyword" placeholder="规则名/标签/描述" @search="doSearch" allow-clear />
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="statusFilter" placeholder="状态筛选" allow-clear :dropdown-match-select-width="false" @change="doSearch">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="ACTIVE">已发布</a-select-option>
            <a-select-option value="DRAFT">草稿</a-select-option>
            <a-select-option value="DISABLED">已禁用</a-select-option>
            <a-select-option value="ARCHIVED">已归档</a-select-option>
          </a-select>
        </a-col>
      </a-row>

      <a-table
        :columns="columns"
        :data-source="rules"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ruleName'">
            <span>{{ record.ruleName }}</span>
            <a-tooltip v-if="incompleteRules.has(record.ruleName)" :title="'缺少参数: ' + incompleteRules.get(record.ruleName)?.join(', ')">
              <a-tag color="warning" style="margin-left:4px;cursor:help">⚠ 待补全</a-tag>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'tags'">
            <a-tag v-for="t in (record.tags || '').split(',').filter(Boolean)" :key="t" :color="tagColor(t.trim())" size="small" style="margin:1px">{{ t.trim() }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="router.push(`/rules/editor/${record.id}`)">编辑</a-button>
              <a-switch
                :checked="record.status === 'ACTIVE'"
                :loading="switchingId === record.id"
                checked-children="开"
                un-checked-children="关"
                @change="(val: boolean) => handleToggle(record, val)"
              />
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import axios from 'axios'
import { ruleApi } from '@/api/rule'

const router = useRouter()
const loading = ref(false)
const rules = ref<any[]>([])
const keyword = ref('')
const statusFilter = ref('')
const incompleteRules = ref<Map<string, string[]>>(new Map())

async function checkCompleteness() {
  try {
    const res = await axios.get('/api/rules/completeness')
    if (res.data.code === 200) {
      const m = new Map<string, string[]>()
      for (const r of (res.data.data?.incompleteRules || [])) {
        m.set(r.ruleName, r.missingParams)
      }
      incompleteRules.value = m
    }
  } catch { /* ignore */ }
}

const columns = [
  { title: '规则名', dataIndex: 'ruleName', key: 'ruleName', width: 180 },
  { title: '标签', key: 'tags', width: 150 },
  { title: '状态', key: 'status', width: 80 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 160, sorter: true },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 160, sorter: true },
  { title: '操作', key: 'action', width: 250 },
]

const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条`,
})

const TAG_COLORS = ['blue','green','orange','purple','cyan','magenta','gold','lime','geekblue','volcano']

function tagColor(tag: string): string {
  let hash = 0
  for (let i = 0; i < tag.length; i++) hash = ((hash << 3) - hash) + tag.charCodeAt(i)
  return TAG_COLORS[Math.abs(hash) % TAG_COLORS.length]
}

function statusColor(s: string) {
  return { ACTIVE: 'green', DRAFT: 'default', DISABLED: 'red', ARCHIVED: 'gray' }[s] || 'default'
}
function statusLabel(s: string) {
  return { ACTIVE: '已发布', DRAFT: '草稿', DISABLED: '已禁用', ARCHIVED: '已归档' }[s] || s
}

async function fetchRules() {
  loading.value = true
  try {
    const res = await ruleApi.listRules(
      pagination.current, pagination.pageSize,
      keyword.value || undefined,
      statusFilter.value || undefined,
      sortField.value, sortOrder.value,
    )
    rules.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch {
    // ignore
  } finally { loading.value = false }
}

const sortField = ref('updateTime')
const sortOrder = ref('desc')

function doSearch() {
  pagination.current = 1
  fetchRules()
}

function handleTableChange(pag: any, _filters: any, sorter: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  if (sorter.field) {
    sortField.value = sorter.field
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  }
  fetchRules()
}

const switchingId = ref<string | null>(null)

async function handleToggle(record: any, checked: boolean) {
  switchingId.value = record.id
  try {
    if (checked) {
      await ruleApi.publishRule(record.id)
      message.success('已启用')
    } else {
      await ruleApi.disableRule(record.id)
      message.success('已禁用')
    }
    fetchRules()
  } catch (e: any) { message.error(e.message) }
  finally { switchingId.value = null }
}

async function handleDelete(id: string) {
  try { await ruleApi.deleteRule(id); message.success('已删除'); fetchRules() } catch (e: any) { message.error(e.message) }
}

onMounted(() => { fetchRules(); checkCompleteness() })
</script>
