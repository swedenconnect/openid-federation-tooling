<script setup>
  import { computed } from 'vue'
  import VueJsonPretty from 'vue-json-pretty'
  import 'vue-json-pretty/lib/styles.css'

  const props = defineProps({
    data: { type: Object, default: null },
    deep: { type: Number, default: 3 },
  })

  const JWT_PATTERN = /^[\w-]+\.[\w-]+\.[\w-]*$/

  function decodePart (part) {
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(b64.padEnd(Math.ceil(b64.length / 4) * 4, '=')), c => c.codePointAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
  }

  // Replaces signed JWT strings under the `trust_mark` claim with their decoded header and payload
  function expandTrustMarks (value, key) {
    if (Array.isArray(value)) {
      return value.map(v => expandTrustMarks(v, key))
    }
    if (value && typeof value === 'object') {
      return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, expandTrustMarks(v, k)]))
    }
    if (key === 'trust_mark' && typeof value === 'string' && JWT_PATTERN.test(value)) {
      try {
        const [header, payload] = value.split('.')
        return { header: decodePart(header), payload: decodePart(payload) }
      } catch {
        return value
      }
    }
    return value
  }

  const displayData = computed(() => expandTrustMarks(props.data, null))

  // NumericDate claims (RFC 7519) - seconds since epoch
  const DATE_CLAIMS = new Set(['exp', 'iat', 'nbf', 'auth_time'])

  function remaining (date) {
    const diff = Math.floor((date.getTime() - Date.now()) / 1000)
    const total = Math.abs(diff)
    const days = Math.floor(total / 86_400)
    const hours = Math.floor(total % 86_400 / 3600)
    const minutes = Math.floor(total % 3600 / 60)
    const seconds = total % 60
    const text = `${days} days ${hours} hours ${minutes} minutes ${seconds} seconds`
    return diff < 0 ? `expired ${text} ago` : `in ${text}`
  }

  function dateTitle (node) {
    if (!DATE_CLAIMS.has(node.key) || typeof node.content !== 'number') {
      return null
    }
    const date = new Date(node.content * 1000)
    if (Number.isNaN(date.getTime())) {
      return null
    }
    const local = date.toLocaleString('sv-SE')
    return node.key === 'exp' ? `Expires: ${local} (${remaining(date)})` : local
  }
</script>

<template>
  <vue-json-pretty :data="displayData" :deep="deep">
    <template #renderNodeValue="{ node, defaultValue }">
      <span v-if="dateTitle(node)" class="jwt-date" :title="dateTitle(node)">{{ defaultValue }}</span>
      <template v-else>{{ defaultValue }}</template>
    </template>
  </vue-json-pretty>
</template>

<style scoped>
.jwt-date {
  cursor: help;
  text-decoration: underline dotted;
}
</style>
