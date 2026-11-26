<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="public-shell">
    <header class="public-header">
      <RouterLink class="public-brand" to="/careers"><span>H</span> HireFlow</RouterLink>
      <nav class="public-nav" aria-label="公開導覽"><RouterLink to="/careers">瀏覽職缺・我要應徵</RouterLink><RouterLink class="public-header-link" to="/login">企業登入</RouterLink></nav>
    </header>
    <main><RouterView /></main>
    <footer class="public-footer">HireFlow 招募入口 · 我們只將履歷用於本次招募流程</footer>
  </div>
</template>
