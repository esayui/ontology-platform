<template>
  <div class="task-list-page">
    <a-card title="杀伤链/杀伤网建模任务">
      <template #extra>
        <a-button type="primary" @click="showCreateDialog">新建任务</a-button>
      </template>

      <a-row :gutter="12" style="margin-bottom:16px">
        <a-col :span="6">
          <a-input-search v-model:value="keyword" placeholder="搜索任务..." @search="fetchTasks" allow-clear />
        </a-col>
      </a-row>

      <a-table :columns="columns" :data-source="tasks" :loading="loading"
        :pagination="pagination" row-key="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">
              {{ record.status === 'ACTIVE' ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="primary" @click="enterModeler(record.id)">进入建模</a-button>
              <a-button size="small" @click="editTask(record)">编辑</a-button>
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="editingId ? '编辑任务' : '新建任务'" @ok="handleSave" width="500px">
      <a-form layout="vertical" size="small">
        <a-form-item label="任务名称" required>
          <a-input v-model:value="form.name" placeholder="如: 区域防空杀伤链" />
        </a-form-item>
        <a-form-item label="任务描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="描述作战场景..." />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { killChainApi } from '@/api/killchain'

const router = useRouter()
const loading = ref(false)
const tasks = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ name: '', description: '' })

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '状态', key: 'status', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 260 },
]

const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条`,
})

async function fetchTasks() {
  loading.value = true
  try {
    const res = await killChainApi.listTasks(pagination.current, pagination.pageSize, keyword.value || undefined)
    tasks.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch { message.error('加载失败') } finally { loading.value = false }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchTasks()
}

function showCreateDialog() {
  editingId.value = null; form.name = ''; form.description = ''; dialogVisible.value = true
}
function editTask(record: any) {
  editingId.value = record.id; form.name = record.name; form.description = record.description; dialogVisible.value = true
}
function enterModeler(id: string) { router.push(`/killchain/modeler/${id}`) }

async function handleSave() {
  if (!form.name) { message.warning('请输入名称'); return }
  try {
    if (editingId.value) {
      await killChainApi.updateTask({ id: editingId.value, ...form })
    } else {
      await killChainApi.createTask({ ...form })
    }
    message.success('保存成功'); dialogVisible.value = false; fetchTasks()
  } catch { message.error('保存失败') }
}
async function handleDelete(id: string) {
  try { await killChainApi.deleteTask(id); message.success('已删除'); fetchTasks() } catch { message.error('删除失败') }
}

onMounted(() => fetchTasks())
</script>
