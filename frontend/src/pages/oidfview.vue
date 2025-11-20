<script setup>
import {onMounted, ref} from 'vue'
import {useVueFlow, VueFlow} from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import {Background} from '@vue-flow/background'
import {Controls} from '@vue-flow/controls'
import '@vue-flow/controls/dist/style.css'

import dagre from 'dagre'

const nodes = ref([])
const edges = ref([])

const {setNodes, setEdges, fitView} = useVueFlow()

function calcWidth(label){
  return label.length * 7;
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
  const res = await fetch('api/tree')
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

  setNodes(positionedNodes)
  setEdges(labeledEdges)
}

onMounted(async () => {
  await loadTree()
  setTimeout(() => fitView({padding: 0.2}), 150)
})
</script>

<template>
  <div style="width:100%; height:600px;">
    <VueFlow :nodes="nodes" :edges="edges">
      <Background/>
      <Controls/>
    </VueFlow>
  </div>
</template>
