<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { createCandidate, getCandidateResume, getCandidates, updateCandidate } from '@/services/candidateService'

const auth = useAuthStore()
const page = ref({ content: [], totalElements: 0 })
const keyword = ref('')
const editingId = ref(null)
const showForm = ref(false)
const error = ref('')
const message = ref('')
const resume = ref(null)
const resumeLoading = ref(false)
const form = reactive({ fullName: '', email: '', phone: '', currentPosition: '', yearsExperience: '', skills: '', notes: '', version: null })

async function load() {
  try { page.value = await getCandidates({ keyword: keyword.value || undefined, size: 50 }) }
  catch (e) { error.value = e.response?.data?.message || '無法取得應徵者' }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { fullName: '', email: '', phone: '', currentPosition: '', yearsExperience: '', skills: '', notes: '', version: null })
}

function edit(item) {
  editingId.value = item.id
  Object.assign(form, item, { yearsExperience: item.yearsExperience ?? '' })
  showForm.value = true
}

async function save() {
  error.value = ''
  try {
    const payload = { ...form, yearsExperience: form.yearsExperience === '' ? null : Number(form.yearsExperience) }
    if (editingId.value) await updateCandidate(editingId.value, payload)
    else await createCandidate(payload)
    message.value = editingId.value ? '應徵者已更新' : '應徵者已建立'
    showForm.value = false
    resetForm()
    await load()
  } catch (e) { error.value = e.response?.data?.message || '儲存失敗' }
}

async function viewResume(item) {
  resumeLoading.value = true; error.value = ''
  try { resume.value = await getCandidateResume(item.id) }
  catch (e) { error.value = e.response?.data?.message || '無法取得履歷' }
  finally { resumeLoading.value = false }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading"><div><p class="eyebrow">CANDIDATES</p><h1>應徵者資料庫</h1><p>Candidate 是人；同一個人可應徵不同職缺，Email 不重複。</p></div><button v-if="auth.hasAnyRole(['HR'])" class="primary-button page-action" @click="resetForm(); showForm = !showForm">＋ 新增應徵者</button></div>
    <form v-if="showForm" class="form-card candidate-form" @submit.prevent="save"><div class="form-grid">
      <label>姓名<input v-model.trim="form.fullName" required maxlength="100" /></label><label>Email<input v-model.trim="form.email" required type="email" /></label>
      <label>電話<input v-model.trim="form.phone" /></label><label>目前職位<input v-model.trim="form.currentPosition" /></label>
      <label>年資<input v-model="form.yearsExperience" type="number" min="0" step="0.5" /></label><label>技能<input v-model.trim="form.skills" /></label>
      <label class="span-full">備註<textarea v-model.trim="form.notes" rows="3"></textarea></label>
    </div><div class="form-footer"><button class="secondary-button" type="button" @click="showForm = false">取消</button><button class="primary-button page-action" type="submit">{{ editingId ? '儲存修改' : '建立應徵者' }}</button></div></form>
    <form class="filter-card candidate-filter" @submit.prevent="load"><label><span>搜尋</span><input v-model.trim="keyword" placeholder="姓名、Email 或技能" /></label><button class="primary-button" type="submit">查詢</button></form>
    <p v-if="message" class="alert-message is-success">{{ message }}</p><p v-if="error" class="alert-message is-error">{{ error }}</p>
    <article class="data-card"><div v-if="!page.content.length" class="table-state">目前沒有符合條件的應徵者</div><div v-else class="table-scroll"><table class="data-table"><thead><tr><th>姓名</th><th>Email</th><th>目前職位</th><th>年資</th><th>技能</th><th></th></tr></thead><tbody>
      <tr v-for="item in page.content" :key="item.id"><td><strong>{{ item.fullName }}</strong><small>#{{ item.id }}</small></td><td>{{ item.email }}</td><td>{{ item.currentPosition || '—' }}</td><td>{{ item.yearsExperience ?? '—' }}</td><td>{{ item.skills || '—' }}</td><td><div class="heading-actions"><button class="secondary-button" @click="viewResume(item)">查看履歷</button><button v-if="auth.hasAnyRole(['HR'])" class="secondary-button" @click="edit(item)">編輯</button></div></td></tr>
    </tbody></table></div></article>
    <div v-if="resumeLoading" class="resume-drawer">履歷載入中…</div>
    <aside v-else-if="resume" class="resume-drawer">
      <div class="resume-title"><div><p class="eyebrow">RESUME</p><h2>{{ resume.fullName }}</h2></div><button class="secondary-button" @click="resume = null">關閉</button></div>
      <dl class="resume-facts"><div><dt>Email</dt><dd>{{ resume.email }}</dd></div><div><dt>電話</dt><dd>{{ resume.phone || '—' }}</dd></div><div><dt>目前職位</dt><dd>{{ resume.currentPosition || '—' }}</dd></div><div><dt>年資</dt><dd>{{ resume.yearsExperience ?? '—' }}</dd></div><div><dt>技能</dt><dd>{{ resume.skills || '—' }}</dd></div><div><dt>地址</dt><dd>{{ resume.address || '—' }}</dd></div></dl>
      <h3>自傳</h3><p class="resume-copy">{{ resume.autobiography || '未填寫' }}</p>
      <p v-if="resume.portfolioUrl"><a class="text-link" :href="resume.portfolioUrl" target="_blank" rel="noopener">開啟作品集 ↗</a></p>
      <h3>工作經歷</h3><div v-if="!resume.workExperiences.length" class="resume-copy">未填寫</div>
      <article v-for="(work, index) in resume.workExperiences" :key="index" class="resume-work"><strong>{{ work.jobTitle }} · {{ work.companyName }}</strong><small>{{ work.startDate || '—' }} ～ {{ work.currentJob ? '至今' : (work.endDate || '—') }}</small><p>{{ work.description || '未填寫工作內容' }}</p></article>
    </aside>
  </section>
</template>
