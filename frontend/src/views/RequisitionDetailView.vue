<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import {
  getRequisition,
  reviewRequisition,
  submitRequisition,
} from '@/services/requisitionService'

const route = useRoute()
const item = ref(null)
const loading = ref(true)
const working = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const reviewReason = ref('')

const statusLabels = {
  DRAFT: '草稿',
  SUBMITTED: '待審核',
  RETURNED: '已退回',
  APPROVED: '已核准',
  REJECTED: '已拒絕',
}

const actionLabels = {
  CREATED: '建立草稿',
  UPDATED: '更新草稿',
  SUBMITTED: '送出審核',
  APPROVED: '核准需求',
  RETURNED: '退回修改',
  REJECTED: '拒絕需求',
}

function formatDate(value, withTime = false) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', withTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(new Date(value))
}

function formatMoney(value) {
  if (value === null || value === undefined) return '—'
  return new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD', maximumFractionDigits: 0 }).format(value)
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    item.value = await getRequisition(route.params.id)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法取得人力需求明細'
  } finally {
    loading.value = false
  }
}

async function submitForReview() {
  if (!window.confirm('送出後將無法編輯，確定要交給 HR 審核嗎？')) return
  await runAction(async () => submitRequisition(item.value.id), '人力需求已送出審核')
}

async function review(decision) {
  if ((decision === 'RETURN' || decision === 'REJECT') && !reviewReason.value.trim()) {
    errorMessage.value = decision === 'RETURN' ? '請填寫退回理由' : '請填寫拒絕理由'
    return
  }
  const labels = { APPROVE: '核准', RETURN: '退回', REJECT: '拒絕' }
  if (!window.confirm(`確定要${labels[decision]}這筆人力需求嗎？`)) return
  await runAction(
    async () => reviewRequisition(item.value.id, { decision, reason: reviewReason.value || null }),
    `人力需求已${labels[decision]}`,
  )
  reviewReason.value = ''
}

async function runAction(action, message) {
  working.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    item.value = await action()
    successMessage.value = message
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '操作失敗，請重新整理後再試'
  } finally {
    working.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">REQUISITION DETAIL</p>
        <h1>{{ item?.jobTitle || '人力需求明細' }}</h1>
        <p v-if="item">#{{ item.id }} · {{ item.departmentName }} · 由 {{ item.createdByName }} 建立</p>
      </div>
      <div class="heading-actions">
        <RouterLink class="secondary-button page-action" :to="{ name: 'requisitions' }">返回列表</RouterLink>
        <RouterLink v-if="item?.canEdit" class="primary-button page-action" :to="{ name: 'requisition-edit', params: { id: item.id } }">
          編輯草稿
        </RouterLink>
      </div>
    </div>

    <p v-if="errorMessage" class="alert-message is-error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="alert-message is-success">{{ successMessage }}</p>
    <article v-if="loading" class="data-card table-state">載入需求明細中…</article>

    <template v-else-if="item">
      <div class="detail-layout">
        <div class="detail-main">
          <article class="detail-card">
            <header class="card-heading">
              <div>
                <p class="eyebrow">OVERVIEW</p>
                <h2>需求內容</h2>
              </div>
              <span class="status-badge status-large" :class="`status-${item.status.toLowerCase()}`">
                {{ statusLabels[item.status] }}
              </span>
            </header>

            <dl class="detail-grid">
              <div><dt>部門</dt><dd>{{ item.departmentName }}</dd></div>
              <div><dt>需求人數</dt><dd>{{ item.headcount ?? '尚未填寫' }}</dd></div>
              <div><dt>薪資範圍</dt><dd>{{ formatMoney(item.salaryMin) }} ～ {{ formatMoney(item.salaryMax) }}</dd></div>
              <div><dt>希望到職日</dt><dd>{{ item.targetStartDate ? formatDate(`${item.targetStartDate}T00:00:00`) : '尚未填寫' }}</dd></div>
              <div class="span-full"><dt>需求原因</dt><dd class="pre-line">{{ item.reason || '尚未填寫' }}</dd></div>
              <div class="span-full"><dt>技能需求</dt><dd class="pre-line">{{ item.requiredSkills || '尚未填寫' }}</dd></div>
            </dl>
          </article>

          <article class="detail-card">
            <header class="card-heading">
              <div>
                <p class="eyebrow">HISTORY</p>
                <h2>簽核歷程</h2>
              </div>
            </header>
            <ol class="history-list">
              <li v-for="action in item.actions" :key="action.id">
                <span class="history-dot"></span>
                <div>
                  <strong>{{ actionLabels[action.actionType] }}</strong>
                  <p>{{ action.actorName }} · {{ formatDate(action.createdAt, true) }}</p>
                  <blockquote v-if="action.reason">{{ action.reason }}</blockquote>
                </div>
                <span>{{ statusLabels[action.toStatus] }}</span>
              </li>
            </ol>
          </article>
        </div>

        <aside class="detail-sidebar">
          <article v-if="item.canSubmit" class="action-card">
            <p class="eyebrow">SUBMIT</p>
            <h2>送出 HR 審核</h2>
            <p>送出前請確認必填資料完整；送出後將暫時不能編輯。</p>
            <button class="primary-button wide-button" type="button" :disabled="working" @click="submitForReview">
              送出審核
            </button>
          </article>

          <article v-if="item.canReview" class="action-card">
            <p class="eyebrow">HR REVIEW</p>
            <h2>審核決定</h2>
            <p>退回或拒絕時必須填寫理由；核准會在同一交易建立 OPEN 職缺。</p>
            <label class="review-reason">
              <span>審核理由</span>
              <textarea v-model.trim="reviewReason" rows="5" maxlength="1000" placeholder="說明退回、拒絕或核准原因"></textarea>
            </label>
            <div class="review-actions">
              <button class="primary-button" type="button" :disabled="working" @click="review('APPROVE')">核准</button>
              <button class="secondary-button" type="button" :disabled="working" @click="review('RETURN')">退回</button>
              <button class="danger-button" type="button" :disabled="working" @click="review('REJECT')">拒絕</button>
            </div>
          </article>

          <article v-if="item.opening" class="action-card opening-card">
            <p class="eyebrow">JOB OPENING</p>
            <h2>職缺已建立</h2>
            <p>職缺 #{{ item.opening.id }} · {{ item.opening.status }}</p>
            <small>建立時間 {{ formatDate(item.opening.createdAt, true) }}</small>
          </article>

          <article class="metadata-card">
            <dl>
              <div><dt>建立時間</dt><dd>{{ formatDate(item.createdAt, true) }}</dd></div>
              <div><dt>最後更新</dt><dd>{{ formatDate(item.updatedAt, true) }}</dd></div>
              <div><dt>送出時間</dt><dd>{{ formatDate(item.submittedAt, true) }}</dd></div>
              <div><dt>審核時間</dt><dd>{{ formatDate(item.reviewedAt, true) }}</dd></div>
              <div><dt>資料版本</dt><dd>v{{ item.version }}</dd></div>
            </dl>
          </article>
        </aside>
      </div>
    </template>
  </section>
</template>
