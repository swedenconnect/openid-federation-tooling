<template>
  <v-card class="pa-4 mb-4" variant="outlined">
    <v-row>

      <v-col cols="4">
        <v-text-field
          v-model="localEntityId"
          density="compact"
          label="Entity ID"
        />
      </v-col>

      <v-col cols="4">
        <v-select
          v-model="selectedType"
          density="compact"
          :items="entityTypes"
          label="Entity Type"
        />

        <v-text-field
          v-if="selectedType === OTHER"
          v-model="otherType"
          density="compact"
          label="Other Entity Type"
        />
      </v-col>

      <v-col cols="3">
        <v-text-field
          v-model="localTrustMark"
          density="compact"
          label="Trust Mark"
        />
      </v-col>

      <v-col cols="1">
        <v-btn block class="mt-4" color="primary" @click="$emit('search')">
          Resolve
        </v-btn>

      </v-col>

    </v-row>
  </v-card>
</template>

<script setup>
  import { ref, watch } from 'vue'

  const props = defineProps({
    entityId: String,
    entityType: String,
    trustMark: String,
  })

  const emits = defineEmits([
    'search',
    'update:entity-id',
    'update:entity-type',
    'update:trust-mark',
  ])

  const localEntityId = ref(props.entityId)
  const localEntityType = ref(props.entityType)
  const localTrustMark = ref(props.trustMark)

  watch(localEntityId, v => emits('update:entity-id', v))
  const OTHER = '__other__'

  const entityTypes = [
    { title: 'All entity types', value: '' },
    { title: 'openid_provider', value: 'openid_provider' },
    { title: 'openid_relying_party', value: 'openid_relying_party' },
    { title: 'oauth_authorization_server', value: 'oauth_authorization_server' },
    { title: 'oauth_client', value: 'oauth_client' },
    { title: 'oauth_resource', value: 'oauth_resource' },
    { title: 'federation_entity', value: 'federation_entity' },
    { title: 'saml_identity_provider', value: 'saml_identity_provider' },
    { title: 'Other...', value: OTHER },
  ]

  const selectedType = ref(props.entityType || '')
  const otherType = ref('')

  watch([selectedType, otherType], ([sel, other]) => {
    localEntityType.value = sel === OTHER ? other.trim() : sel
  })
  watch(localEntityType, v => emits('update:entity-type', v))
  watch(localTrustMark, v => emits('update:trust-mark', v))
</script>
