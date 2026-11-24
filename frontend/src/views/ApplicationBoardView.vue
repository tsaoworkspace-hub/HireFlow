<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { createApplication, getApplications, getCandidates, getJobOpenings, transitionApplication } from '@/services/candidateService'

const auth = useAuthStore()
const stages = ['NEW', 'SCREENING', 'INTERVIEW_SCHEDULED', 'INTERVIEWED', 'DECISION_PENDING', 'HIRED', 'ON_HOLD', 'REJECTED', 'WITHDRAWN']
const labels = { NEW: '新進件', SCREENING: '履歷篩選', INTERVIEW_SCHEDULED: '已排面試', INTERVIEWED: '面試完成', DECISION_PENDING: '待決策', HIRED: '已錄取', ON_HOLD: '保留觀察', REJECTED: '未錄取', WITHDRAWN: '已退出' }
const next = { NEW: ['SCREENING', 'WITHDRAWN'], SCREENING: ['INTERVIEW_SCHEDULED', 'REJECTED', 'WITHDRAWN'], INTERVIEW_SCHEDULED: ['INTERVIEWED', 'WITHDRAWN'], INTERVIEWED: ['DECISION_PENDING', 'WITHDRAWN'], DECISION_PENDING: ['WITHDRAWN'] }
const applications = ref([])
const candidates = ref([])
const openings = ref([])
const filters = reactive({ keyword: '', jobOpeningId: '' })
const createForm = reactive({ candidateId: '', jobOpeningId: '' })
const error = ref('')
const message = ref('')
const grouped = computed(() => Object.fromEntries(stages.map(stage => [stage, applications.value.filter(item => item.stage === stage)])))

async function load() {
  error.value = ''
  try {
    const [applicationPage, candidatePage, openingPage] = await Promise.all([
      getApplications({ keyword: filters.keyword || undefined, jobOpeningId: filters.jobOpeningId || undefined, size: 200 }),
      getCandidates({ size: 200 }), getJobOpenings({ status: 'OPEN', size: 100 }),
    ])
    applications.value = applicationPage.content
    candidates.value = candidatePage.content
    openings.value = openingPage.content
  } catch (e) { error.value = e.response?.data?.message || '無法取得應徵流程' }
}

async function addApplication() {
  try {
    await createApplication({ candidateId: Number(createForm.candidateId), jobOpeningId: Number(createForm.jobOpeningId) })
    message.value = '應徵紀錄已建立'
    Object.assign(createForm, { candidateId: '', jobOpeningId: '' })
    await load()
  } catch (e) { error.value = e.response?.data?.message || '建立應徵紀錄失敗' }
}

async function move(item, nextStage) {
  let reason = null
  if (['REJECTED', 'WITHDRAWN'].includes(nextStage)) {
    reason = window.prompt(`請輸入「${labels[nextStage]}」原因`)
    if (!reason) return
  }
  try { await transitionApplication(item.id, { nextStage, reason }); await load() }
  catch (e) { error.value = e.response?.data?.message || '更新階段失敗' }
}

onMounted(load)
</script>

<template>
  <section class="board-page">
    <div class="page-heading"><div><p class="eyebrow">APPLICATION PIPELINE</p><h1>應徵流程看板</h1><p>每張卡片是一筆 Candidate 與 Job Opening 的應徵關係。</p></div></div>
    <form v-if="auth.hasAnyRole(['HR'])" class="application-create" @submit.prevent="addApplication"><select v-model="createForm.candidateId" required><option value="">選擇應徵者</option><option v-for="item in candidates" :key="item.id" :value="item.id">{{ item.fullName }} · {{ item.email }}</option></select><select v-model="createForm.jobOpeningId" required><option value="">選擇招募中職缺</option><option v-for="item in openings" :key="item.id" :value="item.id">{{ item.departmentName }} · {{ item.jobTitle }}</option></select><button class="primary-button" type="submit">建立應徵</button></form>
    <form class="filter-card board-filter" @submit.prevent="load"><label><span>關鍵字</span><input v-model.trim="filters.keyword" placeholder="姓名或 Email" /></label><label><span>職缺</span><select v-model="filters.jobOpeningId"><option value="">全部職缺</option><option v-for="item in openings" :key="item.id" :value="item.id">{{ item.jobTitle }}</option></select></label><button class="primary-button" type="submit">篩選</button></form>
    <p v-if="message" class="alert-message is-success">{{ message }}</p><p v-if="error" class="alert-message is-error">{{ error }}</p>
    <div class="kanban-board"><section v-for="stage in stages" :key="stage" class="kanban-column"><header><strong>{{ labels[stage] }}</strong><span>{{ grouped[stage].length }}</span></header><article v-for="item in grouped[stage]" :key="item.id" class="candidate-card"><strong>{{ item.candidateName }}</strong><small>{{ item.jobTitle }} · {{ item.departmentName }}</small><div v-if="auth.hasAnyRole(['HR']) && next[stage]?.length" class="card-actions"><button v-for="target in next[stage]" :key="target" type="button" @click="move(item, target)">→ {{ labels[target] }}</button></div></article><p v-if="!grouped[stage].length" class="empty-column">沒有資料</p></section></div>
  </section>
</template>
