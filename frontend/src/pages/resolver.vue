<template>
  <v-app>
    <v-container fluid>

      <!-- TOP BAR -->
      <top-filter-bar
        :entity-id="entityId"
        :entity-type="entityType"
        :trust-mark="trustMark"
        @update:entity-id="entityId = $event"
        @update:entity-type="entityType = $event"
        @update:trust-mark="trustMark = $event"
        @search="fetchUrls"
      />

      <v-row>
        <v-col cols="3" class="pa-2" style="border-right: 1px solid #ddd">
          <entity-id-list
            :urls="urls"
            @select="resolveUrl"
          />
        </v-col>

        <v-col cols="9" class="pa-4">
          <resolver-response-view
            :header="jwtHeader"
            :payload="jwtPayload"
            :error-msg="errorMsg"
          />
        </v-col>
      </v-row>

    </v-container>
  </v-app>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import TopFilterBar from '@/components/TopFilterBar.vue'
import EntityIdList from "@/components/EntityIdList.vue";
import ResolverResponseView from "@/components/ResolverResponseView.vue";
import { extractErrorDetail } from '@/utils/apiError'

const route = useRoute()

const entityId = ref("")
const entityType = ref("")
const trustMark = ref("")

const urls = ref([]);
const jwtHeader = ref(null);
const jwtPayload = ref(null);
const errorMsg = ref(null);

onMounted(() => {
  const queryEntityId = route.query.entityId
  if (queryEntityId) {
    entityId.value = queryEntityId
    resolveUrl(queryEntityId)
  }
})

function displayError(msg) {
   jwtHeader.value = null;
   jwtPayload.value = null;
   errorMsg.value = msg;
}


function fetchUrls() {
  const url = `api/discovery?entityid=${encodeURIComponent(entityId.value)}&entityType=${encodeURIComponent(entityType.value)}&trustMark=${encodeURIComponent(trustMark.value)}`

  fetch(url)
      .then(async r => {
        errorMsg.value=undefined;
        if (!r.ok) {
          const text = await r.text();
          throw new Error(text);
        }
        return r.json()
      })
    .then(data => {
      urls.value = data.urls || data
    })
      .catch(err => {
        displayError(extractErrorDetail(err.message));
      })
}

function resolveUrl(u) {
  const apiUrl = `api/resolve?sub=${encodeURIComponent(u)}`

  fetch(apiUrl)
    .then(async r => {
      errorMsg.value=undefined;
      if (!r.ok) {
        const text = await r.text();
        throw new Error(text);
      }
      return r.json()
    })
    .then(data => {
      jwtHeader.value = data.header
      jwtPayload.value = data.payload
    })
    .catch(err => {
      displayError(JSON.parse(err.message).detail);
    })
}
</script>
