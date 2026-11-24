<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import {
  createRequisition,
  getRequisition,
  getRequisitionOptions,
  updateRequisition,
} from '@/services/requisitionService'

const route = useRoute()
const router = useRouter()
const isEditing = computed(() => Boolean(route.params.id))
const options = ref({ departments: [] })
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})
const form = reactive({
  version: null,
  departmentId: '',
  jobTitle: '',
  headcount: '',
  reason: '',
  requiredSkills: '',
  salaryMin: '',
  salaryMax: '',
  targetStartDate: '',
})

function assignForm(item) {
  Object.assign(form, {
    version: item.version,
    departmentId: item.departmentId,
    jobTitle: item.jobTitle || '',
    headcount: item.headcount ?? '',
    reason: item.reason || '',
    requiredSkills: item.requiredSkills || '',
    salaryMin: item.salaryMin ?? '',
    salaryMax: item.salaryMax ?? '',
    targetStartDate: item.targetStartDate || '',
  })
}

function numberOrNull(value) {
  return value === '' || value === null ? null : Number(value)
}

function payload() {
  return {
    ...(isEditing.value ? { version: form.version } : {}),
    departmentId: numberOrNull(form.departmentId),
    jobTitle: form.jobTitle || null,
    headcount: numberOrNull(form.headcount),
    reason: form.reason || null,
    requiredSkills: form.requiredSkills || null,
    salaryMin: numberOrNull(form.salaryMin),
    salaryMax: numberOrNull(form.salaryMax),
    targetStartDate: form.targetStartDate || null,
  }
}

async function save() {
  saving.value = true
  errorMessage.value = ''
  fieldErrors.value = {}
  try {
    const item = isEditing.value
      ? await updateRequisition(route.params.id, payload())
      : await createRequisition(payload())
    await router.push({ name: 'requisition-detail', params: { id: item.id } })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '儲存失敗，請檢查資料後再試'
    fieldErrors.value = error.response?.data?.data || {}
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    options.value = await getRequisitionOptions()
    if (options.value.departments.length === 1) {
      form.departmentId = options.value.departments[0].id
    }
    if (isEditing.value) {
      const item = await getRequisition(route.params.id)
      if (!item.canEdit) {
        await router.replace({ name: 'requisition-detail', params: { id: item.id } })
        return
      }
      assignForm(item)
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法載入人力需求資料'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">REQUISITION DRAFT</p>
        <h1>{{ isEditing ? '編輯人力需求' : '建立人力需求' }}</h1>
        <p>草稿可以先儲存；送出審核前需完整填寫所有業務欄位。</p>
      </div>
      <RouterLink class="secondary-button page-action" :to="isEditing ? { name: 'requisition-detail', params: { id: route.params.id } } : { name: 'requisitions' }">
        返回
      </RouterLink>
    </div>

    <p v-if="errorMessage" class="alert-message is-error">{{ errorMessage }}</p>
    <div v-if="loading" class="form-card table-state">載入表單中…</div>

    <form v-else class="form-card requisition-form" @submit.prevent="save">
      <div class="form-grid">
        <label>
          <span>部門 *</span>
          <select v-model="form.departmentId" required>
            <option disabled value="">請選擇部門</option>
            <option v-for="department in options.departments" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
          <small v-if="fieldErrors.departmentId" class="field-error">{{ fieldErrors.departmentId }}</small>
        </label>
        <label>
          <span>職稱</span>
          <input v-model.trim="form.jobTitle" maxlength="120" placeholder="例如：Backend Engineer" />
          <small v-if="fieldErrors.jobTitle" class="field-error">{{ fieldErrors.jobTitle }}</small>
        </label>
        <label>
          <span>需求人數</span>
          <input v-model="form.headcount" type="number" min="1" placeholder="1" />
          <small v-if="fieldErrors.headcount" class="field-error">{{ fieldErrors.headcount }}</small>
        </label>
        <label>
          <span>希望到職日</span>
          <input v-model="form.targetStartDate" type="date" />
        </label>
        <label>
          <span>最低月薪</span>
          <input v-model="form.salaryMin" type="number" min="0" step="1000" placeholder="60000" />
          <small v-if="fieldErrors.salaryMin" class="field-error">{{ fieldErrors.salaryMin }}</small>
        </label>
        <label>
          <span>最高月薪</span>
          <input v-model="form.salaryMax" type="number" min="0" step="1000" placeholder="90000" />
          <small v-if="fieldErrors.salaryMax" class="field-error">{{ fieldErrors.salaryMax }}</small>
        </label>
        <label class="span-full">
          <span>需求原因</span>
          <textarea v-model.trim="form.reason" maxlength="1000" rows="4" placeholder="說明擴編、補人或專案需求"></textarea>
          <small v-if="fieldErrors.reason" class="field-error">{{ fieldErrors.reason }}</small>
        </label>
        <label class="span-full">
          <span>技能需求</span>
          <textarea v-model.trim="form.requiredSkills" maxlength="2000" rows="5" placeholder="列出必要技能、加分條件與經驗"></textarea>
          <small v-if="fieldErrors.requiredSkills" class="field-error">{{ fieldErrors.requiredSkills }}</small>
        </label>
      </div>

      <footer class="form-footer">
        <span>儲存後仍是草稿，不會立即送給 HR。</span>
        <button class="primary-button page-action" type="submit" :disabled="saving">
          {{ saving ? '儲存中…' : '儲存草稿' }}
        </button>
      </footer>
    </form>
  </section>
</template>
