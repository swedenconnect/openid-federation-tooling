<script setup>
  import { Background } from '@vue-flow/background'
  import { Controls } from '@vue-flow/controls'
  import { Panel, useVueFlow, VueFlow } from '@vue-flow/core'
  import dagre from 'dagre'
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
  import { extractErrorDetail } from '@/utils/apiError'
  import ToolbarNode from '../components/ToolbarNode.vue'
  import '@vue-flow/core/dist/style.css'
  import '@vue-flow/core/dist/theme-default.css'

  import '@vue-flow/controls/dist/style.css'

  const nodes = ref([])
  const edges = ref([])

  const { setNodes, setEdges, fitView, updateNodeData } = useVueFlow()

  const searchText = ref('')
  const searchIndex = ref(0)

  const searchMatches = computed(() => {
    const needle = searchText.value.trim().toLowerCase()
    if (!needle) {
      return []
    }
    return nodes.value.filter(n => n.id.toLowerCase().includes(needle)).map(n => n.id)
  })

  function focusMatch () {
    const matches = searchMatches.value
    if (matches.length === 0) {
      return
    }
    searchIndex.value = ((searchIndex.value % matches.length) + matches.length) % matches.length
    fitView({ nodes: [matches[searchIndex.value]], padding: 2, maxZoom: 1, duration: 400 })
  }

  function nextMatch () {
    searchIndex.value++
    focusMatch()
  }

  watch(searchMatches, matches => {
    const matchSet = new Set(matches)
    for (const n of nodes.value) {
      updateNodeData(n.id, { highlight: matchSet.has(n.id) })
    }
    searchIndex.value = 0
    focusMatch()
  })
  const containerHeight = ref(0)

  const edgeDialogOpen = ref(false)
  const edgeDialogLoading = ref(false)
  const edgeDialogError = ref(null)
  const edgeDialogData = ref(null)
  const edgeDialogParent = ref('')
  const edgeDialogChild = ref('')

  async function onEdgeClick ({ edge }) {
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
    } catch (error) {
      edgeDialogError.value = error.message
    } finally {
      edgeDialogLoading.value = false
    }
  }
  function calcWidth (label) {
    return label.length * 8 + 24
  }

  // Träd-layout med dagre
  function applyDagreLayout (nodes, edges) {
    const g = new dagre.graphlib.Graph()
    g.setGraph({ rankdir: 'TB', nodesep: 50, ranksep: 100 })
    g.setDefaultEdgeLabel(() => ({}))

    // Lägg till noder i grafen med bredd/höjd
    for (const n of nodes) {
      g.setNode(n.id, { width: calcWidth(n.label) + 50, height: 50 })
    }

    // Lägg till kanter
    for (const e of edges) {
      g.setEdge(e.source, e.target)
    }

    // Räkna ut layout
    dagre.layout(g)

    // Uppdatera positioner
    return nodes.map(n => {
      const nodeWithPos = g.node(n.id)
      return {
        ...n,
        position: {
          x: nodeWithPos.x - 200 / 2, // centrera nod
          y: nodeWithPos.y - 50 / 2,
        },
        width: calcWidth(n.label),
      }
    })
  }

  async function loadTree () {
    console.debug('Loading tree...')
    const res = await fetch('api/tree').catch(console.error)
    if (!res.ok) {
      console.error('Server error:', res.status)
      return
    }

    const data = await res.json()

    // Lägg till edge-labels
    const labeledEdges = data.edges.map(e => ({
      ...e,
      label: e.label || '',
    }))

    const positionedNodes = applyDagreLayout(data.nodes, labeledEdges)
      .map(n => ({
        ...n,
        type: 'menu',
        data: {
          label: n.label,
          selectedOptions: [],
        },
      }))
    console.debug('Loaded tree:', positionedNodes)
    nodes.value = positionedNodes
    setNodes(positionedNodes)
    setEdges(labeledEdges)
  }

  onMounted(async () => {
    await loadTree()
    setTimeout(() => fitView({ padding: 0.2 }), 150)
    updateContainerHeight() // sätt initial höjd
    window.addEventListener('resize', updateContainerHeight)
  })

  function updateContainerHeight () {
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
    <VueFlow
      :edges="edges"
      :nodes="nodes"
      @edge-click="onEdgeClick"
    >
      <template #node-menu="props">
        <ToolbarNode :id="props.id" :data="props.data" />
      </template>
      <Background />
      <Controls />

      <Panel position="top-left">
        <v-text-field
          v-model="searchText"
          class="node-search"
          clearable
          density="compact"
          flat
          hide-details
          label="Search entity"
          prepend-inner-icon="mdi-magnify"
          style="width: 320px"
          variant="solo-filled"
          @keydown.enter="nextMatch"
        >
          <template v-if="searchText" #append-inner>
            <span class="text-caption">{{ searchMatches.length > 0 ? `${searchIndex + 1}/${searchMatches.length}` : '0' }}</span>
          </template>
        </v-text-field>
      </Panel>

    </VueFlow>
  </div>

  <v-dialog v-model="edgeDialogOpen" max-width="900" scrollable>
    <v-card>
      <v-card-title class="d-flex align-center">
        Subordinate statement
        <v-spacer />
        <v-btn icon size="small" variant="text" @click="edgeDialogOpen = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-subtitle class="text-wrap">{{ edgeDialogParent }} &rarr; {{ edgeDialogChild }}</v-card-subtitle>

      <v-card-text style="max-height: 70vh">
        <v-alert v-if="edgeDialogError" class="mb-4" type="error">{{ edgeDialogError }}</v-alert>
        <v-progress-circular v-else-if="edgeDialogLoading" color="primary" indeterminate />
        <template v-else-if="edgeDialogData">
          <div class="text-subtitle-2 mt-2">Header</div>
          <jwt-json :data="edgeDialogData.header" />
          <div class="text-subtitle-2 mt-4">Payload</div>
          <jwt-json :data="edgeDialogData.payload" />
        </template>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* Drop the global dotted focus outline on the search field; Vuetify's own field highlight remains */
.node-search :deep(input:focus-visible) {
  outline: none !important;
}
</style>
