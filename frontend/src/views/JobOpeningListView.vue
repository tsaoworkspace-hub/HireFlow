<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { closeJobOpening, getJobOpenings } from '@/services/candidateService'

const auth = useAuthStore()
const filters = reactive({ keyword: '', status: '' })
const page = ref({ content: [], totalElements: 0 })
const loading = ref(false)
const message = ref('')
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await getJobOpenings({ ...filters, keyword: filters.keyword || undefined, status: filters.status || undefined })
  } catch (e) {
    error.value = e.response?.data?.message || '無法取得職缺'
  } finally {
    loading.value = false
  }
}

async function closeOpening(item) {
  if (!window.confirm(`確定關閉「${item.jobTitle}」職缺？`)) return
  try {
    await closeJobOpening(item.id)
    message.value = '職缺已關閉；既有應徵資料仍會保留。'
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || '關閉職缺失敗'
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div><p class="eyebrow">JOB OPENINGS</p><h1>招募職缺</h1><p>核准的人力需求會自動轉成職缺；關閉後不能再新增應徵紀錄。</p></div>
    </div>
    <form class="filter-card compact-filter" @submit.prevent="load">
      <label><span>關鍵字</span><input v-model.trim="filters.keyword" placeholder="搜尋職稱" /></label>
      <label><span>狀態</span><select v-model="filters.status"><option value="">全部</option><option value="OPEN">招募中</option><option value="CLOSED">已關閉</option></select></label>
      <button class="primary-button" type="submit">查詢</button>
    </form>
    <p v-if="message" class="alert-message is-success">{{ message }}</p>
    <p v-if="error" class="alert-message is-error">{{ error }}</p>
    <article class="data-card">
      <div v-if="loading" class="table-state">載入職缺中…</div>
      <div v-else-if="!page.content.length" class="table-state">目前沒有符合條件的職缺</div>
      <div v-else class="table-scroll"><table class="data-table"><thead><tr><th>職稱</th><th>部門</th><th>名額</th><th>應徵數</th><th>負責 HR</th><th>狀態</th><th></th></tr></thead><tbody>
        <tr v-for="item in page.content" :key="item.id"><td><strong>{{ item.jobTitle }}</strong><small>#{{ item.id }}</small></td><td>{{ item.departmentName }}</td><td>{{ item.headcount }}</td><td>{{ item.applicationCount }}</td><td>{{ item.responsibleHrName }}</td><td><span class="status-badge" :class="item.status === 'OPEN' ? 'status-approved' : 'status-draft'">{{ item.status === 'OPEN' ? '招募中' : '已關閉' }}</span></td><td><button v-if="item.status === 'OPEN' && auth.hasAnyRole(['HR'])" class="danger-button" type="button" @click="closeOpening(item)">關閉</button></td></tr>
      </tbody></table></div>
    </article>
  </section>
</template>
