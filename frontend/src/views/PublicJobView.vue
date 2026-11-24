<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPublicJob, submitApplication } from '@/services/publicCareerService'

const route = useRoute()
const router = useRouter()
const job = ref(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const form = reactive({ fullName: '', email: '', phone: '', currentPosition: '', yearsExperience: null,
  skills: '', address: '', autobiography: '', portfolioUrl: '', consent: false, workExperiences: [] })

function addExperience() { form.workExperiences.push({ companyName: '', jobTitle: '', startDate: '', endDate: '', currentJob: false, description: '' }) }
function removeExperience(index) { form.workExperiences.splice(index, 1) }
async function submit() {
  submitting.value = true; error.value = ''
  try {
    const result = await submitApplication(route.params.slug, form)
    sessionStorage.setItem(`hireflow-token-${result.applicationId}`, result.accessToken)
    await router.push(result.trackingPath)
  } catch (exception) { error.value = exception.response?.data?.message || '履歷送出失敗，請稍後再試' }
  finally { submitting.value = false }
}
onMounted(async () => {
  try { job.value = await fetchPublicJob(route.params.slug) }
  catch (exception) { error.value = exception.response?.data?.message || '找不到職缺' }
  finally { loading.value = false }
})
</script>

<template>
  <div class="public-container public-job-page">
    <div v-if="loading" class="public-state">正在載入職缺…</div>
    <template v-else-if="job">
      <section class="job-detail-hero">
        <RouterLink to="/careers">← 返回所有職缺</RouterLink>
        <p class="eyebrow">{{ job.department }}</p><h1>{{ job.title }}</h1>
        <div class="job-meta"><span>{{ job.location || '工作地點面議' }}</span><span>{{ job.employmentType || '全職' }}</span></div>
      </section>
      <div class="public-two-column">
        <article class="job-description">
          <h2>職缺說明</h2><p>{{ job.description || '詳細內容將由招募窗口於面談時說明。' }}</p>
          <h2>需要技能</h2><p>{{ job.requiredSkills || '依職務需求評估。' }}</p>
        </article>
        <form class="application-form" @submit.prevent="submit">
          <h2>應徵這個職缺</h2>
          <div v-if="error" class="alert-message is-error">{{ error }}</div>
          <label>姓名<input v-model.trim="form.fullName" required maxlength="100"></label>
          <label>Email<input v-model.trim="form.email" required type="email"></label>
          <label>電話<input v-model.trim="form.phone" maxlength="40"></label>
          <div class="form-grid"><label>目前職稱<input v-model.trim="form.currentPosition"></label><label>年資<input v-model.number="form.yearsExperience" type="number" min="0" step="0.5"></label></div>
          <label>技能<textarea v-model.trim="form.skills" rows="3" placeholder="Java、Spring Boot、Vue…"></textarea></label>
          <label>地址<input v-model.trim="form.address"></label>
          <label>作品集網址<input v-model.trim="form.portfolioUrl" type="url"></label>
          <label>自傳<textarea v-model.trim="form.autobiography" rows="7"></textarea></label>
          <div class="experience-heading"><h3>工作經歷</h3><button class="secondary-button" type="button" @click="addExperience">新增經歷</button></div>
          <div v-for="(item, index) in form.workExperiences" :key="index" class="experience-card">
            <button type="button" class="remove-link" @click="removeExperience(index)">移除</button>
            <div class="form-grid"><label>公司<input v-model.trim="item.companyName" required></label><label>職稱<input v-model.trim="item.jobTitle" required></label></div>
            <div class="form-grid"><label>開始日期<input v-model="item.startDate" type="date"></label><label>結束日期<input v-model="item.endDate" type="date" :disabled="item.currentJob"></label></div>
            <label class="check-label"><input v-model="item.currentJob" type="checkbox">目前仍在職</label>
            <label>工作內容<textarea v-model.trim="item.description" rows="3"></textarea></label>
          </div>
          <label class="check-label consent"><input v-model="form.consent" type="checkbox" required>我同意公司於招募目的範圍內蒐集與使用上述資料。</label>
          <button class="primary-button" :disabled="submitting">{{ submitting ? '送出中…' : '送出履歷' }}</button>
        </form>
      </div>
    </template>
    <div v-else class="public-state">{{ error }}</div>
  </div>
</template>
