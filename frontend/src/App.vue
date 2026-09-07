<template>
  <v-app>
    <div class="sc-page-container">
      <nav class="sc-navbar">
        <router-link to="/" class="sc-navbar-brand">
          <img src="@/assets/sweden-connect-logo.svg" alt="Sweden Connect" class="sc-logo" />
          <span class="sc-app-title">OpenID Federation Tooling</span>
        </router-link>

        <ul class="sc-nav-links">
          <li><router-link to="/">View</router-link></li>
          <li><router-link to="/validate">Validator</router-link></li>
          <li><router-link to="/resolver">Resolver</router-link></li>
        </ul>
      </nav>

      <v-main class="sc-main">
        <router-view />
      </v-main>

      <footer class="sc-footer">
        <p>SwedenConnect OpenId Federations Tooling</p>
      </footer>
    </div>
  </v-app>
</template>

<script setup>
  //
</script>

<style scoped>
.sc-page-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  background-color: var(--sc-bg);
}

.sc-navbar {
  position: sticky;
  top: 0;
  z-index: 1030;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 1rem 2rem;
  background-color: var(--sc-bg);
  border-bottom: var(--sc-border-width) solid var(--sc-border);
}

.sc-navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  color: var(--sc-text);
}

.sc-logo {
  width: 100%;
  max-width: 180px;
  height: auto;
}

.sc-app-title {
  font-size: 1rem;
  font-weight: 500;
  color: var(--sc-text-muted);
  white-space: nowrap;
}

.sc-nav-links {
  display: flex;
  align-items: center;
  gap: 2rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.sc-nav-links a {
  display: inline-block;
  padding-bottom: 0.5rem;
  color: var(--sc-text);
  font-size: 1rem;
  text-decoration: none;
  border-bottom: var(--sc-border-width) solid transparent;
}

.sc-nav-links a:hover {
  color: var(--sc-link-hover);
}

.sc-nav-links a.router-link-exact-active {
  font-weight: 700;
  border-bottom-color: var(--sc-terracotta);
}

.sc-main {
  flex: 1 1 auto;
}

.sc-footer {
  margin-top: 2rem;
  padding: 1.5rem 2rem;
  border-top: var(--sc-border-width) solid var(--sc-border);
}

.sc-footer p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--sc-text-muted);
}
</style>
