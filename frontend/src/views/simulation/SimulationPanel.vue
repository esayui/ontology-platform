<template>
  <div class="simulation-page">
    <a-row :gutter="16" style="height: 100%">
      <a-col :span="12">
        <a-card title="仿真数据接入" size="small" style="height: 100%">
          <template #extra>
            <a-space>
              <a-upload
                accept=".csv"
                :show-upload-list="false"
                :before-upload="handleCsvUpload"
              >
                <a-button size="small">导入CSV</a-button>
              </a-upload>
              <a-button type="primary" size="small" @click="handleIngest" :loading="ingesting">
                提交数据
              </a-button>
            </a-space>
          </template>

          <div class="data-input-area">
            <div v-for="(item, index) in dataList" :key="index" class="data-row">
              <a-input
                v-model:value="item.indicatorId"
                placeholder="指标 ID"
                size="small"
                style="width: 35%"
              />
              <a-input-number
                v-model:value="item.value"
                placeholder="值"
                size="small"
                style="width: 25%"
              />
              <a-input
                v-model:value="item.source"
                placeholder="来源"
                size="small"
                style="width: 25%"
              />
              <a-button size="small" danger type="text" @click="removeRow(index)">删除</a-button>
            </div>
            <a-button size="small" dashed block @click="addRow">+ 添加数据</a-button>
          </div>

          <a-divider>WebSocket 实时推送状态</a-divider>
          <a-tag :color="wsConnected ? 'green' : 'red'">
            {{ wsConnected ? '已连接' : '未连接' }}
          </a-tag>
          <span style="margin-left: 8px; color: #999; font-size: 12px;">
            实时接收违规推送
          </span>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="实时校验结果" size="small" style="height: 100%">
          <div v-if="violations.length > 0">
            <a-timeline>
              <a-timeline-item
                v-for="(v, idx) in violations"
                :key="idx"
                color="red"
              >
                <template #dot>
                  <ExclamationCircleOutlined style="font-size: 16px; color: #ff4d4f" />
                </template>
                <strong>{{ v.ruleName }}</strong>
                <p>{{ v.reason }}</p>
                <small>
                  期望: {{ v.expectedMin }} ~ {{ v.expectedMax }}
                  | 实际: {{ v.actualValue }}
                </small>
              </a-timeline-item>
            </a-timeline>
          </div>
          <a-empty v-else description="暂无违规" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { simulationApi } from '@/api/simulation'
import type { ViolationResult } from '@/types/rule'

const dataList = ref([
  { indicatorId: 'Radar_Detection_Range', value: 80, source: 'sim-001' },
  { indicatorId: 'Warning_Response_Time', value: 35, source: 'sim-001' },
])
const violations = ref<ViolationResult[]>([])
const ingesting = ref(false)
const wsConnected = ref(false)
let stompClient: any = null

function addRow() {
  dataList.value.push({ indicatorId: '', value: 0, source: 'sim-001' })
}

function removeRow(index: number) {
  dataList.value.splice(index, 1)
}

async function handleCsvUpload(file: File) {
  const text = await file.text()
  const lines = text.split('\n').filter(Boolean)
  const newData: any[] = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(',')
    if (cols.length >= 3) {
      newData.push({
        indicatorId: cols[0].trim(),
        value: parseFloat(cols[1]),
        source: cols[2]?.trim() || 'csv',
      })
    }
  }
  dataList.value = newData
  message.info(`已解析 ${newData.length} 条数据`)
  return false
}

async function handleIngest() {
  const validData = dataList.value.filter((d) => d.indicatorId)
  if (validData.length === 0) {
    message.warning('请添加至少一条数据')
    return
  }

  ingesting.value = true
  try {
    const res = await simulationApi.ingestData(validData)
    violations.value = res.data || []
    if (violations.value.length === 0) {
      message.success('所有指标符合要求')
    } else {
      message.warning(`发现 ${violations.value.length} 条违规`)
    }
  } catch (e: any) {
    message.error('提交失败: ' + e.message)
  } finally {
    ingesting.value = false
  }
}

function connectWebSocket() {
  try {
    const { Client } = require('@stomp/stompjs')
    const SockJS = require('sockjs-client')

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws/platform'),
      onConnect: () => {
        wsConnected.value = true
        stompClient?.subscribe('/topic/violations', (msg: any) => {
          const newViolations = JSON.parse(msg.body)
          violations.value = [...newViolations, ...violations.value]
        })
      },
      onDisconnect: () => {
        wsConnected.value = false
      },
    })
    stompClient.activate()
  } catch (e) {
    console.warn('WebSocket connection failed:', e)
  }
}

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  stompClient?.deactivate()
})
</script>

<style scoped>
.simulation-page {
  height: calc(100vh - 200px);
}
.data-input-area {
  max-height: calc(100vh - 450px);
  overflow: auto;
}
.data-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
