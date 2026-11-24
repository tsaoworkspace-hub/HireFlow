<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { getRequisitionOptions, getRequisitions } from '@/services/requisitionService'

const statusLabels = {
  DRAFT: '草稿',
  SUBMITTED: '待審核',
  RETURNED: '已退回',
  APPROVED: '已核准',
  REJECTED: '已拒絕',
}

const filters = reactive({ keyword: '', status: '', departmentId: '' })
const options = ref({ departments: [], statuses: [] })
const pageData = ref({ content: [], page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const loading = ref(true)
const errorMessage = ref('')

async function load(page = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    pageData.value = await getRequisitions({
      page,
      size: 10,
      sort: 'createdAt,desc',
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      departmentId: filters.departmentId || undefined,
    })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法取得人力需求，請稍後再試'
  } finally {
    loading.value = false
  }
}

async function resetFilters() {
  Object.assign(filters, { keyword: '', status: '', departmentId: '' })
  await load(0)
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', { dateStyle: 'medium' }).format(new Date(value))
}

onMounted(async () => {
  try {
    options.value = await getRequisitionOptions()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法取得篩選選項'
  }
  await load()
})
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">REQUISITIONS</p>
        <h1>人力需求</h1>
        <p>建立草稿、送出簽核，並追蹤 HR 審核結果。</p>
      </div>
      <RouterLink class="primary-button page-action" :to="{ name: 'requisition-create' }">
        ＋ 建立需求
      </RouterLink>
    </div>

    <form class="filter-card" @submit.prevent="load(0)">
      <label>
        <span>關鍵字</span>
        <input v-model.trim="filters.keyword" type="search" placeholder="搜尋職稱、原因或技能" />
      </label>
      <label>
        <span>狀態</span>
        <select v-model="filters.status">
          <option value="">全部狀態</option>
          <option v-for="status in options.statuses" :key="status" :value="status">
            {{ statusLabels[status] }}
          </option>
        </select>
      </label>
      <label>
        <span>部門</span>
        <select v-model="filters.departmentId">
          <option value="">全部部門</option>
          <option v-for="department in options.departments" :key="department.id" :value="department.id">
            {{ department.name }}
          </option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="secondary-button" type="button" @click="resetFilters">清除</button>
        <button class="primary-button" type="submit">查詢</button>
      </div>
    </form>

    <p v-if="errorMessage" class="alert-message is-error">{{ errorMessage }}</p>

    <article class="data-card">
      <div v-if="loading" class="table-state">載入人力需求中…</div>
      <div v-else-if="!pageData.content.length" class="table-state">
        <strong>目前沒有符合條件的人力需求</strong>
        <span>你可以調整篩選條件，或建立第一筆需求草稿。</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>職稱</th>
              <th>部門</th>
              <th>需求人數</th>
              <th>建立者</th>
              <th>狀態</th>
              <th>更新日期</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageData.content" :key="item.id">
              <td>
                <strong>{{ item.jobTitle || '未填寫職稱' }}</strong>
                <small>#{{ item.id }}</small>
              </td>
              <td>{{ item.departmentName }}</td>
              <td>{{ item.headcount ?? '—' }}</td>
              <td>{{ item.createdByName }}</td>
              <td><span class="status-badge" :class="`status-${item.status.toLowerCase()}`">{{ statusLabels[item.status] }}</span></td>
              <td>{{ formatDate(item.updatedAt) }}</td>
              <td><RouterLink class="text-link" :to="{ name: 'requisition-detail', params: { id: item.id } }">查看</RouterLink></td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer v-if="pageData.totalPages > 0" class="pagination-bar">
        <span>共 {{ pageData.totalElements }} 筆 · 第 {{ pageData.page + 1 }} / {{ pageData.totalPages }} 頁</span>
        <div>
          <button class="secondary-button" type="button" :disabled="pageData.first" @click="load(pageData.page - 1)">上一頁</button>
          <button class="secondary-button" type="button" :disabled="pageData.last" @click="load(pageData.page + 1)">下一頁</button>
        </div>
      </footer>
    </article>
  </section>
</template>
