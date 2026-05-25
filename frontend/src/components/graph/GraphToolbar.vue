<template>
  <div class="graph-toolbar">
    <a-space>
      <a-radio-group v-model:value="currentLayout" size="small" @change="handleLayoutChange">
        <a-radio-button value="force">力导向</a-radio-button>
        <a-radio-button value="dag">DAG</a-radio-button>
        <a-radio-button value="tree">树</a-radio-button>
        <a-radio-button value="circle">圆形</a-radio-button>
        <a-radio-button value="grid">网格</a-radio-button>
      </a-radio-group>

      <a-divider type="vertical" />

      <a-input-search
        v-model:value="searchText"
        placeholder="搜索节点..."
        size="small"
        style="width: 180px"
        @search="handleSearch"
      />

      <a-divider type="vertical" />

      <a-select
        v-model:value="filterType"
        size="small"
        style="width: 120px"
        placeholder="关系筛选"
        @change="handleFilterChange"
      >
        <a-select-option value="">全部关系</a-select-option>
        <a-select-option value="directlyAffects">直接影响</a-select-option>
        <a-select-option value="indirectlyAffects">间接影响</a-select-option>
        <a-select-option value="constrainedBy">阈值约束</a-select-option>
      </a-select>

      <a-divider type="vertical" />

      <a-button size="small" @click="$emit('fit')">自适应</a-button>
      <a-button size="small" @click="$emit('refresh')">刷新</a-button>

      <a-divider type="vertical" />

      <a-tag v-if="selectedNode" color="blue">选中: {{ selectedNode }}</a-tag>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { LayoutType } from '@/types/graph'

const props = defineProps<{
  layout: LayoutType
  selectedNode: string | null
}>()

const emit = defineEmits<{
  (e: 'layout-change', layout: LayoutType): void
  (e: 'search', query: string): void
  (e: 'filter-change', type: string): void
  (e: 'fit'): void
  (e: 'refresh'): void
}>()

const currentLayout = ref(props.layout)
const searchText = ref('')
const filterType = ref('')

function handleLayoutChange() {
  emit('layout-change', currentLayout.value)
}

function handleSearch() {
  emit('search', searchText.value)
}

function handleFilterChange() {
  emit('filter-change', filterType.value)
}
</script>

<style scoped>
.graph-toolbar {
  padding: 8px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
}
</style>
