<template>
  <v-app>
    <v-toolbar color="primary" dark>
      <v-toolbar-title>OpenId Federation Tooling</v-toolbar-title>
      <v-spacer />
      <v-btn :to="{ name: '/' }">View</v-btn>
      <v-btn :to="{ name: '/validate' }">Validator</v-btn>
      <v-btn :to="{ name: '/resolver' }">Resolver</v-btn>
    </v-toolbar>

    <v-main class="pa-4">
      <router-view />
    </v-main>
  </v-app>
  <v-footer>SwedenConnect OpenId Federations Tooling</v-footer>
</template>

<script setup>
  //
</script>
