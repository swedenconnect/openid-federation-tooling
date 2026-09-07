<script setup>
import {onMounted, onBeforeUnmount, ref} from 'vue'
import {useVueFlow, VueFlow} from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import {Background} from '@vue-flow/background'
import {Controls} from '@vue-flow/controls'
import '@vue-flow/controls/dist/style.css'
import ToolbarNode from '../components/ToolbarNode.vue'

import dagre from 'dagre'

const nodes = ref([])
const edges = ref([])

const {setNodes, setEdges, fitView} = useVueFlow()
const containerHeight = ref(0)
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
             :edges="edges">
      <template #node-menu="props">
        <ToolbarNode :id="props.id" :data="props.data" />
      </template>
      <Background/>
      <Controls/>

    </VueFlow>
  </div>
</template>

