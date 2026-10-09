<script setup>
  import { Handle, Position } from '@vue-flow/core'
  import { ref } from 'vue'
  import { extractErrorDetail } from '@/utils/apiError'

  const props = defineProps(['id', 'data'])

  const dialogOpen = ref(false)
  const dialogTitle = ref('')
  const dialogLoading = ref(false)
  const dialogError = ref(null)
  const dialogData = ref(null)

  async function fetchEntityStatement (entityId) {
    const r = await fetch(`api/entity-statement?sub=${encodeURIComponent(entityId)}`)
    const text = await r.text()
    if (!r.ok) {
      throw new Error(extractErrorDetail(text) || `Serverfel (${r.status})`)
    }
    const json = JSON.parse(text)
    return { data: { header: json.header, payload: json.payload } }
  }

  async function fetchResolve (entityId) {
    const r = await fetch(`api/resolve?sub=${encodeURIComponent(entityId)}`)
    const text = await r.text()
    if (!r.ok) {
      throw new Error(extractErrorDetail(text) || `Serverfel (${r.status})`)
    }
    const json = JSON.parse(text)
    return { data: { header: json.header, payload: json.payload } }
  }

  async function runDialogAction (title, fetcher) {
    dialogTitle.value = title
    dialogOpen.value = true
    dialogLoading.value = true
    dialogError.value = null
    dialogData.value = null

    try {
      const result = await fetcher()
      dialogData.value = result.data
    } catch (error) {
      dialogError.value = error.message
    } finally {
      dialogLoading.value = false
    }
  }

  function showEntityStatement () {
    return runDialogAction('Entity statement', () => fetchEntityStatement(props.id))
  }

  function showResolveResponse () {
    return runDialogAction('Resolver response', () => fetchResolve(props.id))
  }
</script>

<template>
  <div class="node-content" :class="{ highlighted: data.highlight }" :title="props.id" @click="showEntityStatement">
    <span class="node-label">{{ data.label }}</span>
    <v-icon
      class="node-resolve"
      icon="mdi-magnify"
      size="small"
      title="Visa resolver-svar"
      @click.stop="showResolveResponse"
    />
  </div>

  <Handle :position="Position.Top" type="target" />
  <Handle :position="Position.Bottom" type="source" />

  <v-dialog v-model="dialogOpen" max-width="900" scrollable>
    <v-card>
      <v-card-title class="d-flex align-center">
        {{ dialogTitle }}
        <v-spacer />
        <v-btn icon size="small" variant="text" @click="dialogOpen = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-subtitle class="text-wrap">{{ props.id }}</v-card-subtitle>

      <v-card-text style="max-height: 70vh">
        <v-alert v-if="dialogError" class="mb-4" type="error">{{ dialogError }}</v-alert>
        <v-progress-circular v-else-if="dialogLoading" color="primary" indeterminate />
        <template v-else>
          <template v-if="dialogData">
            <div class="text-subtitle-2 mt-2">Header</div>
            <jwt-json :data="dialogData.header" />
            <div class="text-subtitle-2 mt-4">Payload</div>
            <jwt-json :data="dialogData.payload" />
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.node-label {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.node-resolve {
  flex: none;
  opacity: 0.6;
}

.node-resolve:hover {
  opacity: 1;
}

.node-content.highlighted {
  border-color: #d32f2f;
  box-shadow: 0 0 0 3px rgb(211 47 47 / 30%);
}

.node-content:hover {
  background: #f5f5f5;
}
</style>
