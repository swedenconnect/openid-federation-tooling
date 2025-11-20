<template>
  <v-card v-if="errorMsg" class="pa-4"><pre>{{ errorMsg }}</pre></v-card>
  <v-container class="pa-1" v-if="pl">
      <!-- Översta sektionen -->
      <v-card class="pa-1 mb-1">
        <v-card-title>Resolver response</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" md="6">
              <strong>Subject:</strong> {{ pl.sub }}
            </v-col>
            <v-col cols="12" md="6">
              <strong>Issuer:</strong> {{ pl.iss }}
            </v-col>
            <v-col cols="12" md="6">
              <strong>IssuedAt:</strong> <div :title="pl.iat">{{ iss(pl.iat) }}</div>
            </v-col>
            <v-col cols="12" md="6">
              <strong>Expire:</strong> <div :title="pl.exp">{{ iss(pl.iat) }} {{ exp(pl.exp) }} </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

    <v-card class="pa-1 mb-1">
      <v-card-title>Metadata</v-card-title>
      <v-card-text>
        <v-treeview
          :items="metadataTree"
          activatable
          open-all=true
          density="compact"
        ></v-treeview>
      </v-card-text>
    </v-card>

    <v-card class="pa-1 mb-1" v-if="pl.trust_marks">
      <v-card-title>Trustmarks</v-card-title>

      <v-card-text>
        <v-list density="condensed">
          <v-list-item
            v-for="(tm, i) in pl.trust_marks"
            :key="i"
          >
            <v-list-item-title>{{ tm.trust_mark_id }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>


    <v-expansion-panels multiple>

      <v-expansion-panel>
        <v-expansion-panel-title>Plain Header</v-expansion-panel-title>
        <v-expansion-panel-text>
          <pre>{{ formattedHeader }}</pre>
        </v-expansion-panel-text>
      </v-expansion-panel>

      <v-expansion-panel>
        <v-expansion-panel-title>Plain Payload</v-expansion-panel-title>
        <v-expansion-panel-text>
          <pre>{{ formattedPayload }}</pre>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>


    </v-container>



</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  header: Object,
  payload: Object,
  errorMsg: String,
})

const formattedHeader = computed(() =>
  props.header ? JSON.stringify(props.header, null, 2) : ""
)

const formattedPayload = computed(() =>
  props.payload ? JSON.stringify(props.payload, null, 2) : ""
)

const pl = computed(() =>  props.payload?props.payload:undefined )


function iss(t) {
  if (!t) return ''
  return new Date(t * 1000).toLocaleString()
}
function exp(t) {
  if (!t) return ''

  const now = Date.now()
  const target = t * 1000
  const diff = target - now

  const abs = Math.abs(diff)
  const seconds = Math.floor(abs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  let text

  if (days > 0) {
    text = `${days} day(s)`
  } else if (hours > 0) {
    text = `${hours} hour(s)`
  } else if (minutes > 0) {
    text = `${minutes} minute(s)`
  } else {
    text = `${seconds} second(s)`
  }

  if (diff >= 0) {
    return text + ' remaining'
  } else {
    return text + ' ago'
  }
}

function toTree(obj, parent = '') {
  return Object.entries(obj).map(([key, value]) => {
    const id = parent + key
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return {
        id,
        title: key,
        children: toTree(value, id + '.')
      }
    }
    return {
      id,
      title: `${key}: ${JSON.stringify(value)}`
    }
  })
}

const metadataTree = computed(() => {
  if (!pl.value.metadata) return []
  return toTree(pl.value.metadata)
})



</script>
