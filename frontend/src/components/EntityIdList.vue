<template>
  <v-list dense>
    <v-list-subheader>Result</v-list-subheader>
    <v-list-item
      v-for="entityid in urls"
      :key="entityid"
      @click="$emit('select', entityid)"
      class="cursor-pointer"
    >
      <v-list-item-title class="wrap-text">{{ entityid.replace(/^https?:\/\//, "") }}</v-list-item-title>
    </v-list-item>
  </v-list>
</template>

<script setup>
defineProps({
  urls: Array
})

defineEmits(["select"])
</script>

<style>
.cursor-pointer {
  cursor: pointer;
}
.wrap-text {
  white-space: normal;
}
</style>
