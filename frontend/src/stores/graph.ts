import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LayoutType } from '@/types/graph'

export const useGraphStore = defineStore('graph', () => {
  const currentLayout = ref<LayoutType>('force')
  const selectedNodeId = ref<string | null>(null)
  const selectedEdgeId = ref<string | null>(null)
  const highlightPath = ref<string[]>([])
  const violationPaths = ref<string[][]>([])
  const showRelationshipEditor = ref(false)
  const searchQuery = ref('')

  function setLayout(layout: LayoutType) {
    currentLayout.value = layout
  }

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
    selectedEdgeId.value = null
  }

  function selectEdge(edgeId: string | null) {
    selectedEdgeId.value = edgeId
    selectedNodeId.value = null
  }

  function setHighlightPath(path: string[]) {
    highlightPath.value = path
  }

  function clearSelection() {
    selectedNodeId.value = null
    selectedEdgeId.value = null
    highlightPath.value = []
  }

  return {
    currentLayout, selectedNodeId, selectedEdgeId,
    highlightPath, violationPaths, showRelationshipEditor, searchQuery,
    setLayout, selectNode, selectEdge, setHighlightPath, clearSelection,
  }
})
