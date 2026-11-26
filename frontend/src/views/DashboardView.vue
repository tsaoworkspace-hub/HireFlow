<script setup>
import { onMounted, ref } from 'vue'

import { getHealth } from '@/services/healthService'
import { getApplications } from '@/services/candidateService'
import { getInterviews } from '@/services/interviewService'
import { getRequisitions } from '@/services/requisitionService'
import { useAuthStore } from '@/stores/auth'

const apiStatus = ref('checking')
const authStore = useAuthStore()

const cards = ref([
  { label: '待審核需求', value: '0', helper: '等待 HR 審核的人力需求' },
  { label: '未來七天面試', value: '0', helper: '依登入者資料範圍顯示' },
  { label: '待填評分', value: '0', helper: '只計算指派給我的面試' },
  { label: '流程中人選', value: '0', helper: '目前進行中的應徵紀錄' },
])

onMounted(async () => {
  try {
    await getHealth()
    apiStatus.value = 'online'
  } catch {
    apiStatus.value = 'offline'
  }

  if (authStore.hasAnyRole(['HR', 'HIRING_MANAGER'])) {
    try {
      const result = await getRequisitions({ status: 'SUBMITTED', page: 0, size: 1 })
      cards.value[0].value = String(result.totalElements)
      cards.value[0].helper = '目前等待 HR 審核'
    } catch {
      cards.value[0].helper = '暫時無法取得資料'
    }

    try {
      const activeStages = ['NEW', 'SCREENING', 'INTERVIEW_SCHEDULED', 'INTERVIEWED', 'DECISION_PENDING']
      const results = await Promise.all(activeStages.map((stage) => getApplications({ stage, page: 0, size: 1 })))
      cards.value[3].value = String(results.reduce((total, result) => total + result.totalElements, 0))
    } catch {
      cards.value[3].helper = '暫時無法取得資料'
    }
  }

  if (authStore.hasAnyRole(['HR', 'HIRING_MANAGER', 'INTERVIEWER'])) {
    try {
      const from = new Date()
      const to = new Date(from)
      to.setDate(to.getDate() + 7)
      const result = await getInterviews({
        status: 'SCHEDULED',
        from: from.toISOString(),
        to: to.toISOString(),
        page: 0,
        size: 100,
      })
      cards.value[1].value = String(result.totalElements)
      cards.value[2].value = String(result.content.filter((item) => item.canScore).length)
    } catch {
      cards.value[1].helper = '暫時無法取得資料'
      cards.value[2].helper = '暫時無法取得資料'
    }
  }
})
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">OVERVIEW</p>
        <h1>{{ authStore.user?.displayName }}，歡迎回來</h1>
        <p>{{ authStore.user?.department }} · 依你的角色顯示可操作模組。</p>
      </div>
      <div class="api-status" :class="`is-${apiStatus}`">
        <span class="status-dot"></span>
        API {{ apiStatus }}
      </div>
    </div>

    <div class="row g-3 mb-4">
      <div v-for="card in cards" :key="card.label" class="col-12 col-md-6 col-xl-3">
        <article class="metric-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.helper }}</small>
        </article>
      </div>
    </div>

    <article class="workflow-card">
      <div>
        <p class="eyebrow">MVP WORKFLOW</p>
        <h2>從需求到招募決策</h2>
      </div>
      <ol class="workflow-steps">
        <li>建立需求</li>
        <li>HR 核准</li>
        <li>建立職缺</li>
        <li>加入人選</li>
        <li>安排面試</li>
        <li>完成決策</li>
      </ol>
    </article>
  </section>
</template>

