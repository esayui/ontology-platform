<template>
  <div class="rule-editor-page">
    <a-card :title="isNew ? '新建规则' : '编辑规则'">
      <template #extra>
        <a-space>
          <a-button @click="router.back()">返回</a-button>
          <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
        </a-space>
      </template>

      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="规则名称" required>
              <a-input v-model:value="form.ruleName" placeholder="例如: RadarDetectionCheck" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检索标签">
              <a-select
                v-model:value="tagList"
                mode="tags"
                placeholder="输入后回车添加标签"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="规则描述">
          <a-textarea v-model:value="form.description" placeholder="描述规则用途和约束条件" :rows="2" />
        </a-form-item>

        <a-form-item label="DRL 规则内容" required>
          <DrlMonacoEditor
            v-model="form.drlContent"
            style="height: calc(100vh - 340px); min-height: 400px;"
          />
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import DrlMonacoEditor from '@/components/editor/DrlMonacoEditor.vue'
import { ruleApi } from '@/api/rule'

const router = useRouter()
const route = useRoute()
const saving = ref(false)

const ruleId = computed(() => route.params.id as string | undefined)
const isNew = computed(() => !ruleId.value)

const form = reactive({
  ruleName: '',
  tags: '',
  description: '',
  drlContent: `package com.ontology.platform.rules;

import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;
import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;

global java.util.List violations;

rule "示例规则"
    when
        $fact : IndicatorFact(indicatorName == "Radar_Detection_Range", value != null)
    then
        double val = ((Number) $fact.value()).doubleValue();
        if (val < 100) {
            ViolationResult vr = new ViolationResult(
                "示例规则",
                $fact.indicatorName(),
                "指标值低于阈值 [" + val + "]",
                100, 500, val
            );
            violations.add(vr);
        }
end
`,
})

const tagList = ref<string[]>([])

// Sync tagList <-> form.tags
watch(tagList, (val) => { form.tags = val.join(',') })
watch(() => form.tags, (val) => {
  tagList.value = val ? val.split(',').map(t => t.trim()).filter(Boolean) : []
})

function buildBody() {
  return {
    ruleName: form.ruleName,
    tags: form.tags,
    description: form.description,
    drlContent: form.drlContent,
  }
}

async function handleSave() {
  saving.value = true
  try {
    const body = buildBody()
    if (isNew.value) {
      await ruleApi.createRule(body)
      message.success('规则创建成功')
    } else {
      await ruleApi.updateRule({ id: ruleId.value!, ...body })
      message.success('规则保存成功')
    }
    router.push('/rules')
  } catch (e: any) {
    message.error('保存失败: ' + e.message)
  } finally { saving.value = false }
}

onMounted(async () => {
  if (ruleId.value) {
    try {
      const res = await ruleApi.getRule(ruleId.value)
      Object.assign(form, res.data)
      tagList.value = res.data.tags ? res.data.tags.split(',').map((t: string) => t.trim()).filter(Boolean) : []
    } catch {
      message.error('加载规则失败')
    }
  }
})
</script>

<style scoped>
.rule-editor-page { height: calc(100vh - 160px); }
</style>
