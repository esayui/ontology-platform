<template>
  <div class="cytoscape-container">
    <div ref="cyContainer" class="cy-canvas" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import cytoscape, { type Core } from 'cytoscape'
import type { LayoutType } from '@/types/graph'

const props = defineProps<{
  elements: cytoscape.ElementDefinition[]
  layout: LayoutType
  highlightPath: string[]
}>()

const emit = defineEmits<{
  (e: 'node-click', nodeId: string): void
  (e: 'edge-click', edgeId: string): void
}>()

const cyContainer = ref<HTMLDivElement>()
let cy: Core | null = null

const layoutConfig: Record<LayoutType, any> = {
  force: {
    name: 'cose',
    idealEdgeLength: 200,
    nodeOverlap: 40,
    nodeRepulsion: 20000,
    gravity: 0.3,
    padding: 50,
    animate: false,
  },
  dag: {
    name: 'breadthfirst',
    directed: true,
    spacingFactor: 1.6,
    avoidOverlap: true,
    padding: 30,
  },
  tree: {
    name: 'breadthfirst',
    directed: true,
    spacingFactor: 1.4,
    avoidOverlap: true,
    padding: 30,
  },
  circle: {
    name: 'circle',
    spacingFactor: 1.3,
    avoidOverlap: true,
    padding: 30,
  },
  grid: {
    name: 'grid',
    avoidOverlap: true,
    padding: 30,
  },
}

onMounted(() => {
  if (!cyContainer.value) return

  cy = cytoscape({
    container: cyContainer.value,
    elements: props.elements,
    style: [
      {
        selector: 'node',
        style: {
          'background-color': '#4a90d9',
          label: 'data(label)',
          'font-size': '11px',
          'text-valign': 'bottom',
          'text-halign': 'center',
          'text-margin-y': 8,
          'text-wrap': 'wrap',
          'text-max-width': '90px',
          'text-overflow-wrap': 'anywhere',
          color: '#333',
          width: 44,
          height: 44,
          'border-width': 1.5,
          'border-color': '#fff',
        },
      },
      {
        selector: 'node.domain-SurfaceWarfare',
        style: { 'background-color': '#e74c3c' },
      },
      {
        selector: 'node.domain-UnderwaterWarfare',
        style: { 'background-color': '#2980b9' },
      },
      {
        selector: 'node.domain-LandWarfare',
        style: { 'background-color': '#27ae60' },
      },
      {
        selector: 'node.domain-LowAltitudeWarfare',
        style: { 'background-color': '#f39c12' },
      },
      {
        selector: 'node.domain-HighAltitudeWarfare',
        style: { 'background-color': '#8e44ad' },
      },
      {
        selector: 'node.category-Radar',
        style: { shape: 'diamond' },
      },
      {
        selector: 'node.category-WeaponSystem',
        style: { shape: 'triangle' },
      },
      {
        selector: 'node.category-Submarine',
        style: { shape: 'round-rectangle' },
      },
      {
        selector: 'node.category-Fighter',
        style: { shape: 'vee' },
      },
      {
        selector: '.dimmed',
        style: { opacity: 0.12, 'text-opacity': 0 },
      },
      {
        selector: 'node.highlighted',
        style: {
          'border-color': '#ff0000',
          'border-width': 4,
          'border-opacity': 1,
        },
      },
      {
        selector: 'node:selected',
        style: {
          'border-color': '#1890ff',
          'border-width': 3,
        },
      },
      {
        selector: 'edge',
        style: {
          width: 2,
          'line-color': '#999',
          'target-arrow-color': '#999',
          'target-arrow-shape': 'triangle',
          'target-arrow-fill': 'filled',
          'curve-style': 'bezier',
          label: 'data(label)',
          'font-size': '10px',
          color: '#666',
          'edge-text-rotation': 'autorotate',
          'text-background-color': '#ffffff',
          'text-background-opacity': 0.85,
          'text-background-padding': '2px',
          'text-background-shape': 'round-rectangle',
          'text-margin-y': -8,
        },
      },
      {
        selector: 'edge.directlyAffects',
        style: {
          'line-color': '#e74c3c',
          'target-arrow-color': '#e74c3c',
          width: 2.5,
        },
      },
      {
        selector: 'edge.indirectlyAffects',
        style: {
          'line-color': '#f39c12',
          'target-arrow-color': '#f39c12',
          'line-style': 'dashed',
          'line-dash-pattern': [8, 4],
        },
      },
      {
        selector: 'edge.constrainedBy',
        style: {
          'line-color': '#3498db',
          'target-arrow-color': '#3498db',
          'line-style': 'dotted',
          'line-dash-pattern': [4, 4],
        },
      },
      {
        selector: 'edge.highlighted',
        style: {
          'line-color': '#ff0000',
          'target-arrow-color': '#ff0000',
          width: 4,
        },
      },
    ],
    layout: layoutConfig[props.layout] || layoutConfig.force,
  })

  cy.on('tap', 'node', (evt) => { emit('node-click', evt.target.id()) })
  cy.on('tap', 'edge', (evt) => { emit('edge-click', evt.target.id()) })
  cy.on('tap', (evt) => {
    if (evt.target === cy) { emit('node-click', ''); emit('edge-click', '') }
  })
})

onUnmounted(() => { cy?.destroy() })

watch(() => props.layout, (newLayout) => {
  if (cy) cy.layout(layoutConfig[newLayout] || layoutConfig.force).run()
})

watch(() => props.elements, (newElements) => {
  if (cy) {
    cy.elements().remove()
    cy.add(newElements)
    cy.layout(layoutConfig[props.layout] || layoutConfig.force).run()
  }
}, { deep: true })

watch(() => props.highlightPath, (path) => {
  if (!cy) return
  cy.elements().removeClass('highlighted')
  path.forEach((id) => {
    const el = cy?.getElementById(id)
    if (el) el.addClass('highlighted')
  })
})

defineExpose({ getCy: () => cy })
</script>

<style scoped>
.cytoscape-container { width: 100%; height: 100%; min-height: 400px; }
.cy-canvas { width: 100%; height: 100%; }
</style>
