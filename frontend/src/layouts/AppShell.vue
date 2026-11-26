<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const navigation = [
  { to: '/', label: 'Dashboard' },
  { to: '/requisitions', label: '人力需求', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/job-openings', label: '招募職缺', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/candidates', label: '應徵者', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/applications', label: '應徵流程', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/interviews', label: '面試', roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] },
  { to: '/calendar', label: '公司行事曆', roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] },
  { to: '/integrations', label: '通知與同步紀錄', roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] },
  { to: '/decisions', label: '招募決策', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/offers', label: 'Offer 管理', roles: ['HR', 'HIRING_MANAGER'] },
  { to: '/admin/audit-logs', label: '稽核紀錄', roles: ['ADMIN'] },
]

const visibleNavigation = computed(() => navigation.filter((item) => authStore.hasAnyRole(item.roles)))
const primaryRole = computed(() => authStore.user?.roles?.[0] || 'USER')

async function handleLogout() {
  await authStore.signOut()
  await router.push({ name: 'login' })
}
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
          v-for="item in visibleNavigation"
          :key="item.to"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-note">
        <strong>HireFlow</strong>
        <span>招募流程與面試協作平台</span>
      </div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <small>Recruitment workflow</small>
          <strong>招募／面試流程與簽核系統</strong>
        </div>
        <div class="user-menu">
          <span class="user-name">{{ authStore.user?.displayName }}</span>
          <span class="role-badge">DEMO · {{ primaryRole }}</span>
          <button class="logout-button" type="button" @click="handleLogout">登出</button>
        </div>
      </header>

      <div class="page-container">
        <RouterView />
      </div>
    </main>
  </div>
</template>

