<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import { useAuthStore } from '@/stores/auth'
import { getApplications, transitionApplication } from '@/services/candidateService'
import { createDecision, getDecisionView } from '@/services/decisionService'

const auth = useAuthStore()
const applications = ref([])
const selected = ref(null)
const selectedId = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const message = ref('')
const form = reactive({ result: 'HIRED', reason: '' })
const search = reactive({ keyword: '', stage: '' })
const pageNumber = ref(0)
const hasMore = ref(false)
const visibleApplications = computed(() => applications.value)

const stageLabels = {
  INTERVIEWED: '面試完成', DECISION_PENDING: '待決策',
  HIRED: '已錄取', ON_HOLD: '保留觀察', REJECTED: '不錄取',
}
const resultLabels = { HIRED: '錄取', ON_HOLD: '保留', REJECTED: '不錄取' }
const recommendationLabels = {
  STRONG_HIRE: '強烈錄取', HIRE: '建議錄取',
  NO_HIRE: '不建議錄取', STRONG_NO_HIRE: '強烈不錄取',
}
const canPrepare = computed(() =>
  auth.hasAnyRole(['HR'])
    && selected.value?.stage === 'INTERVIEWED'
    && selected.value?.readyForDecision)

async function loadApplications() {
  error.value = ''
  try {
    const page = await getApplications({ keyword: search.keyword || undefined, stage: search.stage || undefined, decisionOnly: true, page: pageNumber.value, size: 50, sort: 'updatedAt,desc' })
    hasMore.value = !page.last
    const visibleStages = ['INTERVIEWED', 'DECISION_PENDING', 'HIRED', 'ON_HOLD', 'REJECTED']
    applications.value = page.content.filter((item) => visibleStages.includes(item.stage))
    if (!applications.value.some(item => String(item.id) === selectedId.value)) {
      selectedId.value = applications.value.length ? String(applications.value[0].id) : ''
      await loadDecisionView()
    }
  } catch (e) {
    error.value = e.response?.data?.message || '無法取得待決策資料'
  }
}

function searchApplications() { pageNumber.value = 0; selectedId.value = ''; loadApplications() }
function changePage(delta) { pageNumber.value += delta; selectedId.value = ''; loadApplications() }

async function loadDecisionView() {
  if (!selectedId.value) {
    selected.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    selected.value = await getDecisionView(Number(selectedId.value))
  } catch (e) {
    error.value = e.response?.data?.message || '無法取得評分彙整'
  } finally {
    loading.value = false
  }
}

async function prepareDecision() {
  if (!window.confirm('確認面試皆已完成，將此應徵送入用人主管決策？')) return
  saving.value = true
  error.value = ''
  try {
    await transitionApplication(selected.value.applicationId, { nextStage: 'DECISION_PENDING', reason: null })
    message.value = '已送入待決策階段'
    await loadDecisionView()
    await loadApplications()
  } catch (e) {
    error.value = e.response?.data?.message || '送入待決策失敗'
  } finally {
    saving.value = false
  }
}

async function submitDecision() {
  if (!window.confirm(`確認送出「${resultLabels[form.result]}」決策？送出後不可修改。`)) return
  saving.value = true
  error.value = ''
  try {
    selected.value = await createDecision(selected.value.applicationId, {
      result: form.result,
      reason: form.reason,
      applicationVersion: selected.value.applicationVersion,
    })
    message.value = '最終招募決策已建立'
    form.reason = ''
    await loadApplications()
  } catch (e) {
    error.value = e.response?.data?.message || '建立決策失敗'
  } finally {
    saving.value = false
  }
}

onMounted(loadApplications)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">HIRING DECISIONS</p>
        <h1>招募決策</h1>
        <p>彙整面試評分，由用人主管決定錄取、保留或未錄取。</p>
      </div>
    </div>

    <p v-if="message" class="alert-message is-success">{{ message }}</p>
    <p v-if="error" class="alert-message is-error">{{ error }}</p>

    <form class="filter-card decision-search" @submit.prevent="searchApplications"><label><span>搜尋案件</span><input v-model.trim="search.keyword" placeholder="姓名、Email 或職缺" /></label><label><span>狀態</span><select v-model="search.stage"><option value="">全部狀態</option><option v-for="(label, stage) in stageLabels" :key="stage" :value="stage">{{ label }}</option></select></label><button class="primary-button page-action">查詢</button></form>

    <div class="decision-layout">
      <aside class="data-card decision-list">
        <header><strong>決策案件</strong><span>{{ visibleApplications.length }}</span></header>
        <button
          v-for="item in visibleApplications"
          :key="item.id"
          type="button"
          :class="['decision-list-item', { active: Number(selectedId) === item.id }]"
          @click="selectedId = String(item.id); loadDecisionView()"
        >
          <strong>{{ item.candidateName }}</strong>
          <small>{{ item.jobTitle }} · {{ item.departmentName }}</small>
          <span class="status-badge">{{ stageLabels[item.stage] || item.stage }}</span>
        </button>
        <div v-if="!visibleApplications.length" class="table-state">沒有符合條件的案件</div>
        <div class="list-pagination"><button type="button" :disabled="pageNumber === 0" @click="changePage(-1)">上一頁</button><span>第 {{ pageNumber + 1 }} 頁</span><button type="button" :disabled="!hasMore" @click="changePage(1)">下一頁</button></div>
      </aside>

      <main class="decision-main">
        <div v-if="loading" class="detail-card table-state">載入評分彙整中…</div>
        <template v-else-if="selected">
          <article class="detail-card">
            <div class="card-heading">
              <div><p class="eyebrow">APPLICATION #{{ selected.applicationId }}</p><h2>{{ selected.candidateName }}</h2></div>
              <span class="status-badge status-large">{{ stageLabels[selected.stage] || selected.stage }}</span>
            </div>
            <dl class="detail-grid">
              <div><dt>應徵職缺</dt><dd>{{ selected.jobTitle }}</dd></div>
              <div><dt>部門</dt><dd>{{ selected.departmentName }}</dd></div>
              <div><dt>Email</dt><dd>{{ selected.candidateEmail }}</dd></div>
              <div><dt>決策前置條件</dt><dd>{{ selected.readyForDecision ? '已完成' : '尚未完成' }}</dd></div>
            </dl>
            <button v-if="canPrepare" class="primary-button wide-button" :disabled="saving" @click="prepareDecision">送入待決策</button>
          </article>

          <section class="detail-card">
            <div class="card-heading"><div><p class="eyebrow">SCORECARD SUMMARY</p><h2>面試評分彙整</h2></div></div>
            <div v-if="selected.scorecards.length" class="decision-scorecards">
              <article v-for="score in selected.scorecards" :key="score.scorecardId">
                <header><strong>第 {{ score.roundNumber }} 輪 · {{ score.interviewerName }}</strong><span>{{ score.averageScore }} / 5</span></header>
                <p>技術 {{ score.technicalScore }}｜分析 {{ score.problemAnalysisScore }}｜溝通 {{ score.communicationScore }}｜學習 {{ score.learningAbilityScore }}</p>
                <blockquote>{{ score.overallComment }}</blockquote>
                <small>{{ recommendationLabels[score.recommendation] || score.recommendation }}</small>
              </article>
            </div>
            <div v-else class="table-state">尚無已送出的評分</div>
          </section>

          <article v-if="selected.decision" class="detail-card decision-result-card">
            <p class="eyebrow">FINAL DECISION</p>
            <div class="decision-result-layout"><span class="decision-result-icon" aria-hidden="true">{{ selected.decision.result === 'HIRED' ? '✓' : selected.decision.result === 'REJECTED' ? '×' : '—' }}</span><div><h2>{{ resultLabels[selected.decision.result] }}</h2><p>決策人：{{ selected.decision.decidedByName }} · {{ new Date(selected.decision.decidedAt).toLocaleString('zh-TW') }}</p></div></div>
            <div class="decision-reason"><strong>決策說明</strong><p>{{ selected.decision.reason }}</p></div>
          </article>

          <form v-else-if="selected.canDecide" class="form-card decision-form" @submit.prevent="submitDecision">
            <p class="eyebrow">FINAL DECISION</p><h2>建立招募決策</h2>
            <label><span>決策結果</span><select v-model="form.result"><option value="HIRED">錄取</option><option value="ON_HOLD">保留</option><option value="REJECTED">不錄取</option></select></label>
            <label><span>決策說明</span><textarea v-model.trim="form.reason" rows="5" maxlength="2000" required placeholder="請說明決策依據；送出後不可修改。" /></label>
            <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '送出中…' : '送出最終決策' }}</button>
          </form>

          <p v-else-if="selected.stage === 'DECISION_PENDING'" class="alert-message is-error">只有該部門用人主管可以建立最終決策。</p>
        </template>
        <div v-else class="empty-state"><h2>選擇一筆案件</h2><p>查看面試評分與決策狀態。</p></div>
      </main>
    </div>
  </section>
</template>
