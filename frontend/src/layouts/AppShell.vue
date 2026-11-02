<script setup>
import { RouterLink, RouterView } from 'vue-router'

const navigation = [
  { to: '/', label: 'Dashboard' },
  { to: '/requisitions', label: '人力需求' },
  { to: '/candidates', label: '應徵者' },
  { to: '/interviews', label: '面試' },
]
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">H</span>
        <span>HireFlow</span>
      </RouterLink>

      <nav class="nav flex-column gap-1">
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-note">
        <strong>Week 1 MVP</strong>
        <span>先完成完整流程，再增加整合功能。</span>
      </div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <small>Recruitment workflow</small>
          <strong>招募／面試流程與簽核系統</strong>
        </div>
        <span class="role-badge">DEMO · HR</span>
      </header>

      <div class="page-container">
        <RouterView />
      </div>
    </main>
  </div>
</template>

