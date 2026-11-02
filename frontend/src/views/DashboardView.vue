<script setup>
import { onMounted, ref } from 'vue'

import { getHealth } from '@/services/healthService'

const apiStatus = ref('checking')

const cards = [
  { label: '待審核需求', value: '0', helper: 'Day 3 串接資料' },
  { label: '本週面試', value: '0', helper: 'Day 5 串接資料' },
  { label: '待填評分', value: '0', helper: '依登入者篩選' },
  { label: '流程中人選', value: '0', helper: 'Day 4 串接資料' },
]

onMounted(async () => {
  try {
    await getHealth()
    apiStatus.value = 'online'
  } catch {
    apiStatus.value = 'offline'
  }
})
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">OVERVIEW</p>
        <h1>招募流程儀表板</h1>
        <p>第一週先用清楚、可驗收的流程證明後端設計能力。</p>
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
        <h2>從需求到錄取決策</h2>
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

