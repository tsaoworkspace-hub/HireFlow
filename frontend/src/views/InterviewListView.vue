<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import {
  cancelInterview,
  completeInterview,
  getInterview,
  getInterviewOptions,
  getInterviews,
  saveScorecard,
  scheduleInterview,
  unlockScorecard,
} from '@/services/interviewService'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isHr = computed(() => auth.hasAnyRole(['HR']))
const statusLabels = { SCHEDULED: '已排程', COMPLETED: '已完成', CANCELLED: '已取消' }
const formatLabels = { ONSITE: '現場', ONLINE: '線上', PHONE: '電話' }
const recommendationLabels = { STRONG_HIRE: '強烈建議錄取', HIRE: '建議錄取', NO_HIRE: '不建議錄取', STRONG_NO_HIRE: '強烈不建議錄取' }
const filters = reactive({ status: '' })
const page = ref({ content: [], totalElements: 0 })
const options = ref({ applications: [], interviewers: [] })
const selected = ref(null)
const showSchedule = ref(false)
const loading = ref(false)
const error = ref('')
const message = ref('')
const scheduleForm = reactive({ applicationId: '', roundNumber: 1, startsAt: '', endsAt: '', format: 'ONLINE', location: '', interviewerIds: [] })
const scoreForm = reactive({ technicalScore: 3, problemAnalysisScore: 3, communicationScore: 3, learningAbilityScore: 3, overallComment: '', recommendation: 'HIRE' })

const ownScorecard = computed(() => selected.value?.scorecards?.find(item => item.interviewerId === auth.user?.id))

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await getInterviews({ status: filters.status || undefined, size: 50, sort: 'startsAt,asc' })
  } catch (e) { error.value = e.response?.data?.message || '無法取得面試資料' }
  finally { loading.value = false }
}

async function loadOptions() {
  if (!isHr.value) return
  try { options.value = await getInterviewOptions() }
  catch (e) { error.value = e.response?.data?.message || '無法取得面試安排選項' }
}

async function schedule() {
  error.value = ''
  try {
    await scheduleInterview({
      ...scheduleForm,
      applicationId: Number(scheduleForm.applicationId),
      roundNumber: Number(scheduleForm.roundNumber),
      startsAt: new Date(scheduleForm.startsAt).toISOString(),
      endsAt: new Date(scheduleForm.endsAt).toISOString(),
      interviewerIds: scheduleForm.interviewerIds.map(Number),
    })
    message.value = '面試已安排，Application 已進入「已排面試」。'
    showSchedule.value = false
    Object.assign(scheduleForm, { applicationId: '', roundNumber: 1, startsAt: '', endsAt: '', format: 'ONLINE', location: '', interviewerIds: [] })
    await Promise.all([load(), loadOptions()])
  } catch (e) { error.value = e.response?.data?.message || '安排面試失敗' }
}

async function openDetail(item) {
  try {
    selected.value = await getInterview(item.id)
    const mine = selected.value.scorecards?.find(scorecard => scorecard.interviewerId === auth.user?.id)
    if (mine?.status === 'DRAFT') Object.assign(scoreForm, mine)
  } catch (e) { error.value = e.response?.data?.message || '無法取得面試明細' }
}

async function cancel(item) {
  const reason = window.prompt('請輸入取消原因')
  if (!reason) return
  try { await cancelInterview(item.id, reason); message.value = '面試已取消'; selected.value = null; await load() }
  catch (e) { error.value = e.response?.data?.message || '取消失敗' }
}

async function complete(item) {
  if (!window.confirm('確認評分已完成，並將此面試標記為完成？')) return
  try { await completeInterview(item.id); message.value = '面試已完成'; selected.value = null; await load() }
  catch (e) { error.value = e.response?.data?.message || '完成面試失敗' }
}

async function saveScore(submit) {
  if (submit && !window.confirm('評分送出後會鎖定，確定送出？')) return
  try {
    await saveScorecard(selected.value.id, { ...scoreForm, submit })
    message.value = submit ? '評分已送出並鎖定' : '評分草稿已儲存'
    await openDetail(selected.value)
    await load()
  } catch (e) { error.value = e.response?.data?.message || '儲存評分失敗' }
}

async function unlock(scorecard) {
  const reason = window.prompt('請輸入解鎖原因')
  if (!reason) return
  try { await unlockScorecard(selected.value.id, scorecard.id, reason); message.value = '評分已解鎖'; await openDetail(selected.value) }
  catch (e) { error.value = e.response?.data?.message || '解鎖失敗' }
}

function formatTime(value) {
  return new Intl.DateTimeFormat('zh-TW', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

onMounted(async () => { await Promise.all([load(), loadOptions()]) })
</script>

<template>
  <section>
    <div class="page-heading">
      <div><p class="eyebrow">INTERVIEWS</p><h1>{{ isHr ? '面試安排與評分' : '我的面試' }}</h1><p>時段與權限由後端檢查；評分送出後必須由 HR 留下原因才能解鎖。</p></div>
      <button v-if="isHr" class="primary-button page-action" type="button" @click="showSchedule = !showSchedule">＋ 安排面試</button>
    </div>

    <form v-if="showSchedule" class="form-card interview-schedule-form" @submit.prevent="schedule">
      <div class="form-grid">
        <label class="span-full">應徵紀錄<select v-model="scheduleForm.applicationId" required><option value="">選擇應徵者與職缺</option><option v-for="item in options.applications" :key="item.id" :value="item.id">{{ item.candidateName }} · {{ item.departmentName }} / {{ item.jobTitle }}</option></select></label>
        <label>輪次<input v-model="scheduleForm.roundNumber" type="number" min="1" required /></label>
        <label>方式<select v-model="scheduleForm.format"><option value="ONLINE">線上</option><option value="ONSITE">現場</option><option value="PHONE">電話</option></select></label>
        <label>開始時間<input v-model="scheduleForm.startsAt" type="datetime-local" required /></label>
        <label>結束時間<input v-model="scheduleForm.endsAt" type="datetime-local" required /></label>
        <label class="span-full">地點或會議資訊<input v-model.trim="scheduleForm.location" placeholder="會議室、Meet 連結或電話" /></label>
        <fieldset class="span-full interviewer-picker"><legend>面試官（至少一人）</legend><label v-for="item in options.interviewers" :key="item.id"><input v-model="scheduleForm.interviewerIds" type="checkbox" :value="item.id" /> <span>{{ item.displayName }} · {{ item.departmentName }}</span></label></fieldset>
      </div>
      <div class="form-footer"><button class="secondary-button" type="button" @click="showSchedule = false">取消</button><button class="primary-button page-action" type="submit">確認安排</button></div>
    </form>

    <form class="filter-card interview-filter" @submit.prevent="load"><label><span>面試狀態</span><select v-model="filters.status"><option value="">全部狀態</option><option value="SCHEDULED">已排程</option><option value="COMPLETED">已完成</option><option value="CANCELLED">已取消</option></select></label><button class="primary-button" type="submit">篩選</button></form>
    <p v-if="message" class="alert-message is-success">{{ message }}</p><p v-if="error" class="alert-message is-error">{{ error }}</p>

    <div class="interview-layout">
      <article class="data-card interview-list">
        <div v-if="loading" class="table-state">載入面試中…</div><div v-else-if="!page.content.length" class="table-state">目前沒有符合條件的面試</div>
        <button v-for="item in page.content" v-else :key="item.id" class="interview-row" :class="{ active: selected?.id === item.id }" type="button" @click="openDetail(item)">
          <span class="interview-date"><strong>{{ formatTime(item.startsAt) }}</strong><small>第 {{ item.roundNumber }} 輪 · {{ formatLabels[item.format] }}</small></span>
          <span><strong>{{ item.candidateName }}</strong><small>{{ item.jobTitle }} · {{ item.departmentName }}</small></span>
          <span class="status-badge" :class="`interview-${item.status.toLowerCase()}`">{{ statusLabels[item.status] }}</span>
        </button>
      </article>

      <aside v-if="selected" class="detail-card interview-detail">
        <div class="card-heading"><div><p class="eyebrow">INTERVIEW #{{ selected.id }}</p><h2>{{ selected.candidateName }}</h2></div><button class="secondary-button" @click="selected = null">關閉</button></div>
        <dl class="interview-metadata"><div><dt>職缺</dt><dd>{{ selected.jobTitle }} · {{ selected.departmentName }}</dd></div><div><dt>時間</dt><dd>{{ formatTime(selected.startsAt) }}－{{ formatTime(selected.endsAt) }}</dd></div><div><dt>方式</dt><dd>{{ formatLabels[selected.format] }} · {{ selected.location || '未填寫' }}</dd></div><div><dt>面試官</dt><dd>{{ selected.interviewers.map(item => item.displayName).join('、') }}</dd></div></dl>
        <div v-if="selected.canManage && selected.status === 'SCHEDULED'" class="interview-admin-actions"><button class="danger-button" @click="cancel(selected)">取消面試</button><button class="primary-button" @click="complete(selected)">完成面試</button></div>

        <section v-if="selected.canScore" class="scorecard-form"><h3>{{ ownScorecard ? '修改評分草稿' : '填寫我的評分' }}</h3><div class="score-grid"><label v-for="field in [{key:'technicalScore',label:'技術能力'},{key:'problemAnalysisScore',label:'問題分析'},{key:'communicationScore',label:'溝通能力'},{key:'learningAbilityScore',label:'學習能力'}]" :key="field.key">{{ field.label }}<select v-model.number="scoreForm[field.key]"><option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option></select></label></div><label>錄取建議<select v-model="scoreForm.recommendation"><option v-for="(label, value) in recommendationLabels" :key="value" :value="value">{{ label }}</option></select></label><label>整體評語<textarea v-model.trim="scoreForm.overallComment" rows="4" required></textarea></label><div class="score-actions"><button class="secondary-button" @click="saveScore(false)">儲存草稿</button><button class="primary-button" @click="saveScore(true)">送出並鎖定</button></div></section>

        <section class="scorecard-list"><h3>評分紀錄</h3><p v-if="!selected.scorecards.length" class="empty-column">目前尚無評分</p><article v-for="scorecard in selected.scorecards" :key="scorecard.id"><header><strong>{{ scorecard.interviewerName }}</strong><span class="status-badge" :class="scorecard.status === 'SUBMITTED' ? 'status-approved' : 'status-draft'">{{ scorecard.status === 'SUBMITTED' ? '已送出' : '草稿' }}</span></header><p>技術 {{ scorecard.technicalScore }}／分析 {{ scorecard.problemAnalysisScore }}／溝通 {{ scorecard.communicationScore }}／學習 {{ scorecard.learningAbilityScore }}</p><blockquote>{{ scorecard.overallComment }}</blockquote><small>{{ recommendationLabels[scorecard.recommendation] }}</small><button v-if="selected.canManage && scorecard.status === 'SUBMITTED'" class="secondary-button" @click="unlock(scorecard)">解鎖評分</button></article></section>
      </aside>
    </div>
  </section>
</template>
