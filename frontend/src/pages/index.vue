<script setup>
import {onMounted, onBeforeUnmount, ref} from 'vue'
import {useVueFlow, VueFlow} from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import {Background} from '@vue-flow/background'
import {Controls} from '@vue-flow/controls'
import '@vue-flow/controls/dist/style.css'
import ToolbarNode from '../components/ToolbarNode.vue'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'
import { extractErrorDetail } from '@/utils/apiError'

import dagre from 'dagre'

const nodes = ref([])
const edges = ref([])

const {setNodes, setEdges, fitView} = useVueFlow()
const containerHeight = ref(0)

const edgeDialogOpen = ref(false)
const edgeDialogLoading = ref(false)
const edgeDialogError = ref(null)
const edgeDialogData = ref(null)
const edgeDialogParent = ref('')
const edgeDialogChild = ref('')

async function onEdgeClick({ edge }) {
  edgeDialogParent.value = edge.source
  edgeDialogChild.value = edge.target
  edgeDialogOpen.value = true
  edgeDialogLoading.value = true
  edgeDialogError.value = null
  edgeDialogData.value = null

  try {
    const url = `api/subordinate-statement?parent=${encodeURIComponent(edge.source)}&sub=${encodeURIComponent(edge.target)}`
    const r = await fetch(url)
    const text = await r.text()
    if (!r.ok) {
      throw new Error(extractErrorDetail(text) || `Serverfel (${r.status})`)
    }
    edgeDialogData.value = JSON.parse(text)
  } catch (e) {
    edgeDialogError.value = e.message
  } finally {
    edgeDialogLoading.value = false
  }
}
function calcWidth(label){
  return label.length * 8 + 24;
}

// Träd-layout med dagre
function applyDagreLayout(nodes, edges) {
  const g = new dagre.graphlib.Graph()
  g.setGraph({rankdir: 'TB', nodesep: 50, ranksep: 100})
  g.setDefaultEdgeLabel(() => ({}))

  // Lägg till noder i grafen med bredd/höjd
  nodes.forEach(n => {
    g.setNode(n.id, {width: calcWidth(n.label) +50, height: 50})
  })

  // Lägg till kanter
  edges.forEach(e => {
    g.setEdge(e.source, e.target)
  })

  // Räkna ut layout
  dagre.layout(g)

  // Uppdatera positioner
  return nodes.map(n => {
    const nodeWithPos = g.node(n.id)
    return {
      ...n,
      position: {
        x: nodeWithPos.x - 200 / 2, // centrera nod
        y: nodeWithPos.y - 50 / 2
      },
        width: calcWidth(n.label)
    }
  })
}

async function loadTree() {
  console.debug("Loading tree...");
  const res = await fetch('api/tree').catch(console.error)
  if (!res.ok) {
    console.error("Server error:", res.status)
    return
  }

  const data = await res.json()

  // Lägg till edge-labels
  const labeledEdges = data.edges.map(e => ({
    ...e,
    label: e.label || '',
  }));

  const positionedNodes = applyDagreLayout(data.nodes, labeledEdges)
    .map(n => ({
      ...n,
      type: 'menu',
      data: { 
        label: n.label,
        toolbarVisible: false,
        selectedOptions: []
      }
    }))
console.debug("Loaded tree:", positionedNodes);
  setNodes(positionedNodes)
  setEdges(labeledEdges)
}

onMounted(async () => {
  await loadTree()
  setTimeout(() => fitView({padding: 0.2}), 150)
  updateContainerHeight() // sätt initial höjd
  window.addEventListener('resize', updateContainerHeight)

})



function updateContainerHeight() {
  // Tar fönstrets höjd minus 200px för headern
  containerHeight.value = window.innerHeight - 100
}

onMounted(() => {
  updateContainerHeight() // sätt initial höjd
  window.addEventListener('resize', updateContainerHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateContainerHeight)
})
</script>

<template>
  <div :style="{ width: '100%', height: containerHeight + 'px' }">
    <VueFlow :nodes="nodes"
             :edges="edges"
             @edge-click="onEdgeClick">
      <template #node-menu="props">
        <ToolbarNode :id="props.id" :data="props.data" />
      </template>
      <Background/>
      <Controls/>

    </VueFlow>
  </div>

  <v-dialog v-model="edgeDialogOpen" max-width="900" scrollable>
    <v-card>
      <v-card-title class="d-flex align-center">
        Subordinate statement
        <v-spacer />
        <v-btn icon variant="text" size="small" @click="edgeDialogOpen = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-subtitle class="text-wrap">{{ edgeDialogParent }} &rarr; {{ edgeDialogChild }}</v-card-subtitle>

      <v-card-text style="max-height: 70vh">
        <v-alert v-if="edgeDialogError" type="error" class="mb-4">{{ edgeDialogError }}</v-alert>
        <v-progress-circular v-else-if="edgeDialogLoading" indeterminate color="primary" />
        <template v-else-if="edgeDialogData">
          <div class="text-subtitle-2 mt-2">Header</div>
          <vue-json-pretty :data="edgeDialogData.header" :deep="3" />
          <div class="text-subtitle-2 mt-4">Payload</div>
          <vue-json-pretty :data="edgeDialogData.payload" :deep="3" />
        </template>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

