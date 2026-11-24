<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="public-shell">
    <header class="public-header">
      <RouterLink class="public-brand" to="/careers"><span>H</span> HireFlow Careers</RouterLink>
      <RouterLink class="public-header-link" to="/login">公司登入</RouterLink>
    </header>
    <main><RouterView /></main>
    <footer class="public-footer">HireFlow 招募入口 · 我們只將履歷用於本次招募流程</footer>
  </div>
</template>
