<template>
  <v-container class="pa-4" fluid>

    <!-- INPUT + POST -->
    <v-card class="pa-4">
      <v-card-title>EntityStatement Validator</v-card-title>

      <v-textarea
        v-model="inputData"
        label="Enter EntityId JWT or JSON"
        rows="3"
        auto-grow
        variant="outlined"
      ></v-textarea>

      <v-btn
        class="mt-4"
        color="primary"
        @click="postData"
        :loading="loading"
      >
        Validate
      </v-btn>

      <v-alert
        v-if="errorMsg"
        type="error"
        class="mt-4"
      >
        {{ errorMsg }}
      </v-alert>
    </v-card>
    <!-- SUBRESULTS TABELL -->
    <v-card class="mt-8 pa-4" v-if="subResults && subResults.length">
        <v-data-table
          :headers="subHeaders"
          :items="subResults"
          dense
          hide-default-footer
        />
    </v-card>

    <v-spacer />
    <!-- RESULTAT -->
    <v-row class="mt-6" v-if="validatedData">

      <!-- HEADER -->
        <v-card v-if="validatedData.header">
          <v-card-title>
            Header
            <v-btn size="small" icon @click="copyJson(validatedData.header)">
              <v-icon>mdi-content-copy</v-icon>
            </v-btn>
          </v-card-title>
          <vue-json-pretty :data="validatedData.header" :deep="3"></vue-json-pretty>
        </v-card>
      <v-spacer />
      <!-- PAYLOAD -->
        <v-card >
          <v-card-title>
            Payload
            <v-btn size="small" icon @click="copyJson(validatedData.payload)">
              <v-icon>mdi-content-copy</v-icon>
            </v-btn>
          </v-card-title>
          <vue-json-pretty :data="validatedData.payload" :deep="3"></vue-json-pretty>
        </v-card>
      <!-- SIGNATURE -->
        <v-card >
          <v-card-title>
            Signature
            <v-btn size="small" icon @click="copyText(validatedData.signature)">
              <v-icon>mdi-content-copy</v-icon>
            </v-btn>
          </v-card-title>

          <pre>{{ validatedData.signature }}</pre>
        </v-card>
    </v-row>


  </v-container>
</template>


<script setup>
import { ref } from 'vue'
import VueJsonPretty from 'vue-json-pretty';
import 'vue-json-pretty/lib/styles.css';
const inputData = ref("")
const validatedData = ref(null)
const subResults = ref([])
const errorMsg = ref(null)
const loading = ref(false)

const subHeaders = [
  { title: "Level", key: "level" },
  { title: "Type", key: "type" },
  { title: "Message", key: "message" }
]

function pretty(obj) {
  return JSON.stringify(obj, null, 2)
}

async function copyJson(obj) {
  await navigator.clipboard.writeText(JSON.stringify(obj, null, 2))
}

async function copyText(txt) {
  await navigator.clipboard.writeText(txt)
}

async function postData() {
  errorMsg.value = null
  loading.value = true
  validatedData.value = null
  subResults.value = []

  try {
    const r = await fetch("api/validator", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: inputData.value
    })

    const text = await r.text()

    if (!r.ok) {
      throw new Error(`Serverfel (${r.status}): ${text}`)
    }

    const data = JSON.parse(text)

    validatedData.value = data.validatedData
    subResults.value = data.subResults || []

  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>


<style scoped>
pre {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
