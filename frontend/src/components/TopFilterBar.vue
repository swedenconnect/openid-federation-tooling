<template>
  <v-card class="pa-4 mb-4" variant="outlined">
    <v-row>

      <v-col cols="4">
        <v-text-field
          label="Entity ID"
          density="compact"
          v-model="localEntityId"
        />
      </v-col>

      <v-col cols="4">
        <v-select
          label="Entity Type"
          density="compact"
          :items="['',
                  'openid_relying_party',
                  'openid_provider',
                  'oauth_authorization_server',
                  'oauth_client',
                  'aauth_resource',
                  'federation_entity',
                  'saml_identity_provider',
                  'Other'
          ]"
          v-model="localEntityType"
        />


        <v-text-field
          v-if="localEntityType === 'Other'"
          label="Other Entity Type"
          density="compact"
          v-model="localEntityType"
        />
      </v-col>

      <v-col cols="3">
        <v-text-field
          label="Trust Mark"
          density="compact"
          v-model="localTrustMark"
        />
      </v-col>

      <v-col cols="1" >
        <v-btn block class="mt4" color="primary" @click="$emit('search')">
          Resolve
        </v-btn>

      </v-col>

    </v-row>
  </v-card>
</template>

<script setup>
import {ref, watch} from 'vue'

const props = defineProps({
  entityId: String,
  entityType: String,
  trustMark: String
})

const emits = defineEmits([
  'search',
  'update:entity-id',
  'update:entity-type',
  'update:trust-mark'
])

const localEntityId = ref(props.entityId)
const localEntityType = ref(props.entityType)
const localTrustMark = ref(props.trustMark)

watch(localEntityId, v => emits('update:entity-id', v))
watch(localEntityType, v => emits('update:entity-type', v))
watch(localTrustMark, v => emits('update:trust-mark', v))
</script>
