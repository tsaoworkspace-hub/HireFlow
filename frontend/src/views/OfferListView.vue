<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import { useAuthStore } from '@/stores/auth'
import {
  cancelOffer, createOffer, downloadAttachment, getAttachments, getOffer, getOfferOptions,
  getOffers, reviewOffer, sendOffer, submitOffer, updateOffer,
} from '@/services/offerService'

const auth = useAuthStore()
const offers = ref([])
const options = ref([])
const selected = ref(null)
const attachments = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')
const editing = ref(false)
const canMaintain = computed(() => auth.hasAnyRole(['HR']))
const form = reactive({ applicationId: '', annualSalary: '', currency: 'TWD', proposedStartDate: '', expiresAt: '', title: '錄取邀請', message: '', version: null })

const statusLabels = {
  DRAFT: '草稿', PENDING_APPROVAL: '待主管核准', RETURNED: '退回修改', APPROVED: '已核准',
  SENT: '已寄出', ACCEPTED: '求職者接受', DECLINED: '求職者拒絕', EXPIRED: '已過期', CANCELLED: '已取消',
}

function localDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 16)
}

function resetForm() {
  const expiry = new Date(Date.now() + 7 * 86400000)
  form.applicationId = options.value[0]?.applicationId ? String(options.value[0].applicationId) : ''
  form.annualSalary = ''
  form.currency = 'TWD'
  form.proposedStartDate = new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10)
  form.expiresAt = localDateTime(expiry)
  form.title = '錄取邀請'
  form.message = '恭喜你通過招募流程！誠摯邀請你加入我們，請於期限前回覆此 Offer。'
  form.version = null
  editing.value = false
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [page, offerOptions] = await Promise.all([
      getOffers({ size: 100, sort: 'updatedAt,desc' }),
      canMaintain.value ? getOfferOptions() : Promise.resolve([]),
    ])
    offers.value = page.content
    options.value = offerOptions
    const selectedOptionStillExists = options.value.some(
      (item) => String(item.applicationId) === form.applicationId,
    )
    if (!editing.value && !selectedOptionStillExists) resetForm()
    if (selected.value) await select(selected.value.id)
  } catch (exception) { error.value = exception.response?.data?.message || '無法取得 Offer 資料' }
  finally { loading.value = false }
}

async function select(id) {
  error.value = ''
  try {
    selected.value = await getOffer(id)
    attachments.value = await getAttachments(selected.value.applicationId)
    editing.value = false
  } catch (exception) { error.value = exception.response?.data?.message || '無法取得 Offer 詳情' }
}

function editSelected() {
  Object.assign(form, {
    applicationId: String(selected.value.applicationId), annualSalary: selected.value.annualSalary,
    currency: selected.value.currency, proposedStartDate: selected.value.proposedStartDate,
    expiresAt: localDateTime(selected.value.expiresAt), title: selected.value.title,
    message: selected.value.message, version: selected.value.version,
  })
  editing.value = true
}

async function save() {
  saving.value = true; error.value = ''; message.value = ''
  const payload = {
    annualSalary: Number(form.annualSalary), currency: form.currency,
    proposedStartDate: form.proposedStartDate, expiresAt: new Date(form.expiresAt).toISOString(),
    title: form.title, message: form.message,
  }
  try {
    const result = editing.value
      ? await updateOffer(selected.value.id, { ...payload, version: form.version })
      : await createOffer({ ...payload, applicationId: Number(form.applicationId) })
    message.value = editing.value ? 'Offer 已更新' : 'Offer 草稿已建立'
    selected.value = result
    resetForm()
    await load()
    await select(result.id)
  } catch (exception) { error.value = exception.response?.data?.message || 'Offer 儲存失敗' }
  finally { saving.value = false }
}

async function run(action, successMessage) {
  if (!selected.value) return
  saving.value = true; error.value = ''; message.value = ''
  try { selected.value = await action(); message.value = successMessage; await load(); await select(selected.value.id) }
  catch (exception) { error.value = exception.response?.data?.message || '操作失敗' }
  finally { saving.value = false }
}

function submit() { if (confirm('確定送交用人主管核准？')) run(() => submitOffer(selected.value.id), 'Offer 已送審') }
function approve() { run(() => reviewOffer(selected.value.id, { decision: 'APPROVE', reason: null }), 'Offer 已核准') }
function returnForRevision() {
  const reason = prompt('請輸入退回修改原因')
  if (reason?.trim()) run(() => reviewOffer(selected.value.id, { decision: 'RETURN', reason }), 'Offer 已退回 HR')
}
function send() { if (confirm('確定寄出 Offer？系統將建立求職者專屬連結並加入 Email 佇列。')) run(() => sendOffer(selected.value.id), 'Offer 已加入寄信佇列') }
function cancel() {
  const reason = prompt('請輸入取消原因（可留空）')
  if (reason !== null) run(() => cancelOffer(selected.value.id, reason), 'Offer 已取消')
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading"><div><p class="eyebrow">JOB OFFERS</p><h1>Offer 管理</h1><p>由 HR 建立草稿、用人主管核准，再寄給求職者線上回覆。</p></div></div>
    <p v-if="message" class="alert-message is-success">{{ message }}</p>
    <p v-if="error" class="alert-message is-error">{{ error }}</p>

    <form v-if="canMaintain && (options.length || editing)" class="form-card offer-form" @submit.prevent="save">
      <div class="card-heading"><div><p class="eyebrow">{{ editing ? 'EDIT OFFER' : 'NEW OFFER' }}</p><h2>{{ editing ? `修改 ${selected.candidateName} 的 Offer` : '建立 Offer 草稿' }}</h2></div><button v-if="editing" type="button" class="secondary-button" @click="resetForm">取消修改</button></div>
      <div class="form-grid">
        <label v-if="!editing"><span>錄取人選</span><select v-model="form.applicationId" required><option v-for="item in options" :key="item.applicationId" :value="String(item.applicationId)">{{ item.candidateName }}｜{{ item.jobTitle }}（{{ item.departmentName }}）</option></select></label>
        <label><span>年薪</span><input v-model="form.annualSalary" type="number" min="1" required></label>
        <label><span>幣別</span><input v-model.trim="form.currency" maxlength="10" required></label>
        <label><span>預計到職日</span><input v-model="form.proposedStartDate" type="date" required></label>
        <label><span>回覆期限</span><input v-model="form.expiresAt" type="datetime-local" required></label>
        <label class="span-full"><span>標題</span><input v-model.trim="form.title" maxlength="200" required></label>
        <label class="span-full"><span>Offer 訊息</span><textarea v-model.trim="form.message" rows="5" maxlength="5000" required /></label>
      </div>
      <button class="primary-button" :disabled="saving">{{ saving ? '儲存中…' : (editing ? '儲存修改' : '建立草稿') }}</button>
    </form>

    <div v-if="loading" class="data-card table-state">載入 Offer 中…</div>
    <div v-else class="offer-layout">
      <aside class="data-card offer-list">
        <button v-for="offer in offers" :key="offer.id" :class="{ active: selected?.id === offer.id }" @click="select(offer.id)">
          <strong>{{ offer.candidateName }}</strong><small>{{ offer.jobTitle }} · {{ offer.departmentName }}</small><span class="status-badge">{{ statusLabels[offer.status] }}</span>
        </button>
        <div v-if="!offers.length" class="table-state">目前沒有 Offer</div>
      </aside>

      <main v-if="selected" class="offer-detail">
        <article class="detail-card">
          <div class="card-heading"><div><p class="eyebrow">OFFER #{{ selected.id }}</p><h2>{{ selected.title }}</h2></div><span class="status-badge status-large">{{ statusLabels[selected.status] }}</span></div>
          <dl class="detail-grid">
            <div><dt>求職者</dt><dd>{{ selected.candidateName }}（{{ selected.candidateEmail }}）</dd></div><div><dt>職缺</dt><dd>{{ selected.jobTitle }} · {{ selected.departmentName }}</dd></div>
            <div><dt>年薪</dt><dd>{{ Number(selected.annualSalary).toLocaleString() }} {{ selected.currency }}</dd></div><div><dt>預計到職</dt><dd>{{ selected.proposedStartDate }}</dd></div>
            <div><dt>回覆期限</dt><dd>{{ new Date(selected.expiresAt).toLocaleString('zh-TW') }}</dd></div><div><dt>建立／核准</dt><dd>{{ selected.createdByName }} / {{ selected.approvedByName || '尚未核准' }}</dd></div>
            <div class="span-full"><dt>訊息</dt><dd class="pre-line">{{ selected.message }}</dd></div>
          </dl>
          <div class="offer-actions">
            <button v-if="selected.canEdit" class="secondary-button" @click="editSelected">修改</button>
            <button v-if="selected.canSubmit" class="primary-button" :disabled="saving" @click="submit">送主管核准</button>
            <button v-if="selected.canReview" class="primary-button" :disabled="saving" @click="approve">核准</button>
            <button v-if="selected.canReview" class="secondary-button" :disabled="saving" @click="returnForRevision">退回修改</button>
            <button v-if="selected.canSend" class="primary-button" :disabled="saving" @click="send">寄出 Offer</button>
            <button v-if="selected.canCancel" class="danger-button" :disabled="saving" @click="cancel">取消 Offer</button>
          </div>
        </article>

        <article class="detail-card"><div class="card-heading"><div><p class="eyebrow">RESUME PDF</p><h2>求職者附件</h2></div></div>
          <button v-for="file in attachments" :key="file.id" class="attachment-row" @click="downloadAttachment(file)"><span><strong>{{ file.originalFilename }}</strong><small>{{ Math.ceil(file.fileSize / 1024) }} KB · {{ new Date(file.createdAt).toLocaleString('zh-TW') }}</small></span><b>下載</b></button>
          <p v-if="!attachments.length" class="table-state">求職者尚未上傳 PDF 履歷</p>
        </article>

        <article class="detail-card"><div class="card-heading"><div><p class="eyebrow">AUDIT TRAIL</p><h2>Offer 歷程</h2></div></div>
          <ol class="history-list"><li v-for="history in selected.histories" :key="history.id"><span class="history-dot"></span><div><strong>{{ statusLabels[history.toStatus] }}</strong><p>{{ history.actorLabel }} · {{ history.actorType }}</p><blockquote v-if="history.reason">{{ history.reason }}</blockquote></div><span>{{ new Date(history.createdAt).toLocaleString('zh-TW') }}</span></li></ol>
        </article>
      </main>
      <div v-else class="empty-state"><h2>選擇一筆 Offer</h2><p>查看內容、附件與簽核歷程。</p></div>
    </div>
  </section>
</template>
