<script setup>
import { onMounted, ref } from 'vue'
import { fetchPublicJobs } from '@/services/publicCareerService'

const jobs = ref([])
const loading = ref(true)
const error = ref('')
onMounted(async () => {
  try { jobs.value = await fetchPublicJobs() }
  catch (exception) { error.value = exception.response?.data?.message || '目前無法載入職缺' }
  finally { loading.value = false }
})
</script>

<template>
  <section class="career-hero">
    <p class="eyebrow">JOIN OUR TEAM</p>
    <h1>一起打造更好的工作體驗</h1>
    <p>查看目前開放職缺，線上送出履歷並追蹤應徵進度。</p>
  </section>
  <section class="public-container">
    <div v-if="loading" class="public-state">正在載入職缺…</div>
    <div v-else-if="error" class="alert-message is-error">{{ error }}</div>
    <div v-else-if="!jobs.length" class="public-state">目前沒有開放中的職缺。</div>
    <div v-else class="job-grid">
      <RouterLink v-for="job in jobs" :key="job.slug" :to="`/careers/${job.slug}`" class="job-card">
        <div><span>{{ job.department }}</span><span>{{ job.employmentType || '全職' }}</span></div>
        <h2>{{ job.title }}</h2>
        <p>{{ job.location || '工作地點面議' }}</p>
        <strong>查看職缺 →</strong>
      </RouterLink>
    </div>
  </section>
</template>
