<template>
  <v-container class="pa-4">
    <v-row>
      <v-col cols="12">
        <v-toolbar color="primary" dark flat>
          <v-toolbar-title>OpenID Federation Overview</v-toolbar-title>
        </v-toolbar>
      </v-col>
    </v-row>

    <v-row class="mt-4" dense>
      <!-- Card 1: Introduction -->
      <v-col cols="12" md="4">
        <v-card outlined class="pa-4">
          <v-card-title>Introduction</v-card-title>
          <v-card-text>
            OpenID Federation allows organizations to delegate trust in identity information across multiple entities.
            It extends the OpenID Connect protocol by defining metadata, trust marks, and entity statements for automated discovery and trust evaluation.
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Card 2: Key Concepts -->
      <v-col cols="12" md="4">
        <v-card outlined class="pa-4">
          <v-card-title>Key Concepts</v-card-title>
          <v-card-text>
            <ul>
              <li><strong>Entity ID:</strong> Unique identifier for each participant.</li>
              <li><strong>Trust Marks:</strong> Signed attestations of compliance or capability.</li>
              <li><strong>Entity Statements:</strong> JSON Web Tokens (JWTs) containing metadata.</li>
              <li><strong>Discovery:</strong> Automated process to locate and verify entity metadata.</li>
            </ul>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Card 3: Benefits -->
      <v-col cols="12" md="4">
        <v-card outlined class="pa-4">
          <v-card-title>Benefits</v-card-title>
          <v-card-text>
            <ul>
              <li>Automated trust evaluation across multiple domains.</li>
              <li>Standardized metadata sharing between providers and relying parties.</li>
              <li>Enhanced security through cryptographically signed statements.</li>
              <li>Scalable federation with minimal manual configuration.</li>
            </ul>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Footer / Reference -->
    <v-row class="mt-4">
      <v-col cols="12">
        <v-card outlined class="pa-4">
          <v-card-title>References</v-card-title>
          <v-card-text>
            <ul>
              <li><a href="https://openid.net/specs/openid-federation-1_0.html" target="_blank">OpenID Federation Specification</a></li>
              <li><a href="https://openid.net/connect/" target="_blank">OpenID Connect Core</a></li>
            </ul>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
export default {
  name: "IndexPage"
};
</script>

<style scoped>
ul {
  padding-left: 16px;
}
li {
  margin-bottom: 4px;
}
</style>
