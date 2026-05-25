<template>
  <div class="test-bench-page">
    <a-row :gutter="16" style="height: 100%">
      <a-col :span="10">
        <a-card title="测试数据" size="small" style="height: 100%">
          <template #extra>
            <a-button type="primary" size="small" @click="handleExecute" :loading="executing">
              执行规则
            </a-button>
          </template>

          <div class="fact-input-area">
            <div v-for="(item, index) in facts" :key="index" class="fact-row">
              <a-input v-model:value="item.name" placeholder="指标名" size="small" style="width: 45%" />
              <a-input-number v-model:value="item.value" placeholder="值" size="small" style="width: 35%" />
              <a-button size="small" danger type="text" @click="removeFact(index)">删</a-button>
            </div>
            <a-button size="small" dashed block @click="addFact">+ 添加指标</a-button>

            <a-divider>预设场景</a-divider>
            <a-space wrap>
              <a-button size="small" @click="loadPreset('radar')">雷达场景</a-button>
              <a-button size="small" @click="loadPreset('submarine')">潜艇场景</a-button>
              <a-button size="small" @click="loadPreset('tank')">坦克场景</a-button>
              <a-button size="small" @click="loadPreset('fighter')">歼击机场景</a-button>
              <a-button size="small" @click="loadPreset('awacs')">预警机场景</a-button>
            </a-space>
          </div>
        </a-card>
      </a-col>

      <a-col :span="14">
        <a-card title="执行结果" size="small" style="height: 100%">
          <div v-if="results.length > 0">
            <a-alert
              v-for="(r, idx) in results" :key="idx"
              :message="r.ruleName" type="error" show-icon
              style="margin-bottom: 8px"
            >
              <template #description>
                <p>{{ r.reason }}</p>
                <p style="color: #999; font-size: 12px">
                  期望: {{ r.expectedMin }} ~ {{ r.expectedMax }} | 实际: {{ r.actualValue }}
                </p>
              </template>
            </a-alert>
          </div>
          <a-result v-else-if="executed" status="success" title="全部通过" sub-title="所有指标符合规则约束" />
          <a-empty v-else description="输入指标值并点击「执行规则」" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import axios from 'axios'

const executing = ref(false)
const executed = ref(false)
const results = ref<any[]>([])

const facts = ref([
  { name: 'Radar-DetectionRange', value: 80 },
  { name: 'Weapon-ReactionTime', value: 45 },
  { name: 'Weapon-Accuracy', value: 0.5 },
])

function addFact() {
  facts.value.push({ name: '', value: 0 })
}

function removeFact(index: number) {
  facts.value.splice(index, 1)
}

function loadPreset(type: string) {
  const presets: Record<string, { name: string; value: number }[]> = {
    radar: [
      { name: 'Radar-DetectionRange', value: 80 },
      { name: 'Weapon-ReactionTime', value: 45 },
    ],
    submarine: [
      { name: 'Torpedo-Speed', value: 25 },
      { name: 'Submarine-UnderwaterSpeed', value: 28 },
      { name: 'Submarine-SilentLevel', value: 150 },
    ],
    tank: [
      { name: 'Tank-ArmorThickness', value: 1000 },
      { name: 'Tank-MaxSpeed', value: 75 },
    ],
    fighter: [
      { name: 'Fighter-MaxSpeed', value: 1.5 },
      { name: 'Fighter-CombatRadius', value: 400 },
    ],
    awacs: [
      { name: 'AWACS-Endurance', value: 6 },
      { name: 'AWACS-DetectionRange', value: 350 },
    ],
  }
  facts.value = [...(presets[type] || [])]
  executed.value = false
  results.value = []
}

async function handleExecute() {
  const factMap: Record<string, any> = {}
  for (const f of facts.value) {
    if (f.name) factMap[f.name] = f.value
  }
  if (Object.keys(factMap).length === 0) {
    message.warning('请添加至少一个指标')
    return
  }

  executing.value = true
  executed.value = false
  try {
    const res = await axios.post('/api/rules/execute', factMap)
    if (res.data.code === 200) {
      results.value = res.data.data || []
      executed.value = true
      if (results.value.length === 0) {
        message.success('所有指标符合规则约束')
      } else {
        message.warning(`发现 ${results.value.length} 条违规`)
      }
    } else {
      message.error(res.data.message || '执行失败')
    }
  } catch (e: any) {
    message.error('执行失败: ' + (e.response?.data?.message || e.message))
  } finally {
    executing.value = false
  }
}
</script>

<style scoped>
.test-bench-page { height: calc(100vh - 200px); }
.fact-input-area { max-height: calc(100vh - 380px); overflow: auto; }
.fact-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
</style>
