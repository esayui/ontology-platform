<template>
  <a-modal
    :open="visible"
    title="编辑关系属性"
    @ok="handleSave"
    @cancel="$emit('close')"
    width="500px"
  >
    <a-form :model="form" layout="vertical">
      <a-form-item label="关系类型">
        <a-select v-model:value="form.relationshipType">
          <a-select-option value="directlyAffects">直接影响</a-select-option>
          <a-select-option value="indirectlyAffects">间接影响</a-select-option>
          <a-select-option value="constrainedBy">阈值约束</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="Drools 规则名">
        <a-input v-model:value="form.droolsRuleName" placeholder="关联的规则名称" />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="权重">
            <a-input-number v-model:value="form.weight" :min="0" :max="10" :step="0.1" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="优先级">
            <a-input-number v-model:value="form.priority" :min="0" :max="100" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="影响方向">
        <a-radio-group v-model:value="form.influenceDirection">
          <a-radio value="forward">正向</a-radio>
          <a-radio value="reverse">反向</a-radio>
          <a-radio value="bidirectional">双向</a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="是否启用">
        <a-switch v-model:checked="form.enabled" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CapabilityRelationship } from '@/types/ontology'

const props = defineProps<{
  visible: boolean
  relationship: CapabilityRelationship | null
}>()

const emit = defineEmits<{
  (e: 'save', data: CapabilityRelationship): void
  (e: 'close'): void
}>()

const defaultForm = {
  id: '',
  sourceIndicatorId: '',
  targetIndicatorId: '',
  relationshipType: 'directlyAffects',
  droolsRuleName: '',
  weight: 1.0,
  priority: 0,
  influenceDirection: 'forward',
  enabled: true,
}

const form = reactive({ ...defaultForm })

watch(() => props.relationship, (rel) => {
  if (rel) {
    Object.assign(form, rel)
  } else {
    Object.assign(form, defaultForm)
  }
})

function handleSave() {
  emit('save', { ...form })
}
</script>
