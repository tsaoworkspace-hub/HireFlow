<script setup>
import { onMounted, reactive, ref } from 'vue'

import { getAuditLogs } from '@/services/auditService'

const page = ref({ content: [], page: 0, totalElements: 0, totalPages: 0, first: true, last: true })
const filters = reactive({ action: '', targetType: '', page: 0 })
const loading = ref(false)
const error = ref('')

const actionLabels = {
  REQUISITION_CREATED: '建立人力需求', REQUISITION_UPDATED: '修改人力需求',
  REQUISITION_SUBMITTED: '送出人力需求', REQUISITION_APPROVED: '核准人力需求',
  REQUISITION_RETURNED: '退回人力需求', REQUISITION_REJECTED: '拒絕人力需求',
  APPLICATION_CREATED: '建立應徵', APPLICATION_STAGE_CHANGED: '變更應徵階段',
  INTERVIEW_SCHEDULED: '安排面試', INTERVIEW_CANCELLED: '取消面試',
  INTERVIEW_COMPLETED: '完成面試', SCORECARD_SUBMITTED: '送出評分',
  SCORECARD_UNLOCKED: '解鎖評分', HIRING_DECISION_CREATED: '建立招募決策',
}

async function load(targetPage = filters.page) {
  loading.value = true
  error.value = ''
  filters.page = targetPage
  try {
    page.value = await getAuditLogs({
      action: filters.action || undefined,
      targetType: filters.targetType || undefined,
      page: filters.page,
      size: 30,
      sort: 'createdAt,desc',
    })
  } catch (e) {
    error.value = e.response?.data?.message || '無法取得稽核紀錄'
  } finally {
    loading.value = false
  }
}

onMounted(() => load(0))
</script>

<template>
  <section>
    <div class="page-heading"><div><p class="eyebrow">AUDIT TRAIL</p><h1>稽核紀錄</h1><p>重要流程操作與操作者皆由後端在同一交易中保存。</p></div></div>
    <form class="filter-card audit-filter" @submit.prevent="load(0)">
      <label><span>操作</span><select v-model="filters.action"><option value="">全部操作</option><option v-for="(label, value) in actionLabels" :key="value" :value="value">{{ label }}</option></select></label>
      <label><span>目標類型</span><select v-model="filters.targetType"><option value="">全部類型</option><option value="REQUISITION">人力需求</option><option value="APPLICATION">應徵</option><option value="INTERVIEW">面試</option><option value="SCORECARD">評分</option><option value="HIRING_DECISION">招募決策</option></select></label>
      <button class="primary-button" type="submit">篩選</button>
    </form>
    <p v-if="error" class="alert-message is-error">{{ error }}</p>
    <div class="data-card">
      <div v-if="loading" class="table-state">載入稽核紀錄中…</div>
      <div v-else-if="!page.content.length" class="table-state">目前沒有符合條件的紀錄</div>
      <div v-else class="table-scroll"><table class="data-table"><thead><tr><th>時間</th><th>操作</th><th>操作者</th><th>目標</th><th>結果</th><th>摘要</th></tr></thead><tbody><tr v-for="item in page.content" :key="item.id"><td>{{ new Date(item.createdAt).toLocaleString('zh-TW') }}</td><td><strong>{{ actionLabels[item.action] || item.action }}</strong></td><td>{{ item.actorName }}<small>{{ item.actorEmail }}</small></td><td>{{ item.targetType }} #{{ item.targetId }}</td><td><span class="status-badge status-approved">成功</span></td><td>{{ item.details || '—' }}</td></tr></tbody></table></div>
      <footer class="pagination-bar"><span>共 {{ page.totalElements }} 筆</span><div><button class="secondary-button" :disabled="page.first" @click="load(page.page - 1)">上一頁</button><button class="secondary-button" :disabled="page.last" @click="load(page.page + 1)">下一頁</button></div></footer>
    </div>
  </section>
</template>
