<script setup>
import { Handle, Position, useVueFlow } from '@vue-flow/core'
import { NodeToolbar } from '@vue-flow/node-toolbar'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'

const props = defineProps(['id', 'data'])

const { updateNodeData } = useVueFlow()

const nodeContentRef = ref(null)
const toolbarCardRef = ref(null)

function handleClickOutside(event) {
  if (!props.data.toolbarVisible) {
    return
  }
  const target = event.target
  const clickedNode = nodeContentRef.value && nodeContentRef.value.contains(target)
  const clickedToolbar = toolbarCardRef.value && toolbarCardRef.value.$el.contains(target)
  if (!clickedNode && !clickedToolbar) {
    closeToolbar()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

function closeToolbar() {
  updateNodeData(props.id, { toolbarVisible: false })
}

const dialogOpen = ref(false)
const dialogTitle = ref('')
const dialogShowSubResults = ref(false)
const dialogLoading = ref(false)
const dialogError = ref(null)
const dialogData = ref(null)
const dialogSubResults = ref([])

const subResultHeaders = [
  { title: 'Level', key: 'level' },
  { title: 'Type', key: 'type' },
  { title: 'Message', key: 'message' }
]

async function fetchValidator(entityId) {
  const r = await fetch('api/validator', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: entityId
  })
  const text = await r.text()
  if (!r.ok) {
    throw new Error(`Serverfel (${r.status}): ${text}`)
  }
  const data = JSON.parse(text)
  return { data: data.validatedData, subResults: data.subResults || [], success: data.success }
}

async function fetchResolve(entityId) {
  const r = await fetch(`api/resolve?sub=${encodeURIComponent(entityId)}`)
  const text = await r.text()
  if (!r.ok) {
    throw new Error(`Serverfel (${r.status}): ${text}`)
  }
  const json = JSON.parse(text)
  return { data: { header: json.header, payload: json.payload }, subResults: [], success: true }
}

async function runDialogAction(title, fetcher, showSubResults) {
  closeToolbar()
  dialogTitle.value = title
  dialogShowSubResults.value = showSubResults
  dialogOpen.value = true
  dialogLoading.value = true
  dialogError.value = null
  dialogData.value = null
  dialogSubResults.value = []

  try {
    const result = await fetcher()
    if (!result.success) {
      const firstError = result.subResults.find((r) => r.level === 'ERROR')
      dialogError.value = firstError ? firstError.message : 'Kunde inte hämta data.'
    }
    dialogData.value = result.data
    dialogSubResults.value = result.subResults
  } catch (e) {
    dialogError.value = e.message
  } finally {
    dialogLoading.value = false
  }
}

const nodeActions = [
  {
    value: 'view',
    label: 'Visa entity statement',
    icon: 'mdi-eye-outline',
    action: () => runDialogAction('Entity statement', () => fetchValidator(props.id), false)
  },
  {
    value: 'resolve',
    label: 'Resolvera entitet',
    icon: 'mdi-magnify',
    action: () => runDialogAction('Resolve entity', () => fetchResolve(props.id), false)
  },
  {
    value: 'validate',
    label: 'Validera entity statement',
    icon: 'mdi-check-decagram-outline',
    action: () => runDialogAction('Validera entity statement', () => fetchValidator(props.id), true)
  }
]
</script>

<template>
  <NodeToolbar :is-visible="data.toolbarVisible" :position="Position.Bottom">
    <v-card ref="toolbarCardRef" class="pa-1" min-width="240">
      <v-list density="compact" nav>
        <v-list-item
          v-for="option in nodeActions"
          :key="option.value"
          :prepend-icon="option.icon"
          :title="option.label"
          @click="option.action"
        />
      </v-list>
    </v-card>
  </NodeToolbar>

  <div
    ref="nodeContentRef"
    class="node-content"
    :title="props.id"
    @click="() => updateNodeData(props.id, { toolbarVisible: !data.toolbarVisible })"
  >
    {{ data.label }}
  </div>

  <Handle type="target" :position="Position.Top" />
  <Handle type="source" :position="Position.Bottom" />

  <v-dialog v-model="dialogOpen" max-width="900" scrollable>
    <v-card>
      <v-card-title class="d-flex align-center">
        {{ dialogTitle }}
        <v-spacer />
        <v-btn icon variant="text" size="small" @click="dialogOpen = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-subtitle class="text-wrap">{{ props.id }}</v-card-subtitle>

      <v-card-text style="max-height: 70vh">
        <v-alert v-if="dialogError" type="error" class="mb-4">{{ dialogError }}</v-alert>
        <v-progress-circular v-else-if="dialogLoading" indeterminate color="primary" />
        <template v-else>
          <v-data-table
            v-if="dialogShowSubResults && dialogSubResults.length"
            :headers="subResultHeaders"
            :items="dialogSubResults"
            dense
            hide-default-footer
            class="mb-4"
          />
          <template v-if="dialogData">
            <div class="text-subtitle-2 mt-2">Header</div>
            <vue-json-pretty :data="dialogData.header" :deep="3" />
            <div class="text-subtitle-2 mt-4">Payload</div>
            <vue-json-pretty :data="dialogData.payload" :deep="3" />
          </template>
        </template>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.node-content {
  box-sizing: border-box;
  padding: 8px;
  cursor: pointer;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  min-width: 120px;
  max-width: 100%;
  text-align: center;
  color: #333;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.node-content:hover {
  background: #f5f5f5;
}
</style>
