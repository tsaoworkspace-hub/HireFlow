<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchApplication } from '@/services/publicCareerService'
import { getPublicInterviewSlots, selectPublicInterviewSlot } from '@/services/calendarService'
import { acceptPublicOffer, declinePublicOffer, getPublicOffer, getPublicResume, uploadPublicResume } from '@/services/offerService'

const route = useRoute()
const application = ref(null)
const loading = ref(true)
const error = ref('')
const slots = ref([])
const selecting = ref(null)
const offer = ref(null)
const resumes = ref([])
const resumeFile = ref(null)
const uploading = ref(false)
const responding = ref(false)
const actionError = ref('')
const actionMessage = ref('')
const declineReason = ref('')
const token = computed(() => route.query.token || sessionStorage.getItem(`hireflow-token-${route.params.publicId}`))
onMounted(async () => {
  if (!token.value) { error.value = '這個查詢網址缺少存取憑證。'; loading.value = false; return }
  sessionStorage.setItem(`hireflow-token-${route.params.publicId}`, token.value)
  try {
    application.value = await fetchApplication(route.params.publicId, token.value)
    slots.value = await getPublicInterviewSlots(route.params.publicId, token.value)
    resumes.value = await getPublicResume(route.params.publicId, token.value)
    try { offer.value = await getPublicOffer(route.params.publicId, token.value) }
    catch (exception) { if (exception.response?.status !== 404) throw exception }
  }
  catch (exception) { error.value = exception.response?.data?.message || '目前無法取得應徵進度' }
  finally { loading.value = false }
})
async function choose(slot) {
  selecting.value = slot.id; error.value = ''
  try {
    await selectPublicInterviewSlot(route.params.publicId, slot.id, token.value)
    application.value = await fetchApplication(route.params.publicId, token.value)
    slots.value = []
  } catch (exception) { error.value = exception.response?.data?.message || '無法確認面試時間' }
  finally { selecting.value = null }
}
async function uploadResume() {
  if (!resumeFile.value) return
  uploading.value = true; actionError.value = ''; actionMessage.value = ''
  try {
    await uploadPublicResume(route.params.publicId, token.value, resumeFile.value)
    resumes.value = await getPublicResume(route.params.publicId, token.value)
    resumeFile.value = null
    document.querySelector('#resume-file').value = ''
    actionMessage.value = 'PDF 履歷已更新，公司端現在可查看最新版本。'
  } catch (exception) { actionError.value = exception.response?.data?.message || '履歷上傳失敗' }
  finally { uploading.value = false }
}
async function respondToOffer(accept) {
  if (!accept && !declineReason.value.trim()) { actionError.value = '拒絕 Offer 時請填寫原因。'; return }
  if (!confirm(accept ? '確定接受這份 Offer？' : '確定拒絕這份 Offer？')) return
  responding.value = true; actionError.value = ''; actionMessage.value = ''
  try {
    offer.value = accept
      ? await acceptPublicOffer(route.params.publicId, token.value)
      : await declinePublicOffer(route.params.publicId, token.value, declineReason.value)
    actionMessage.value = accept ? '已接受 Offer，公司已收到通知。' : '已回覆拒絕 Offer。'
  } catch (exception) { actionError.value = exception.response?.data?.message || 'Offer 回覆失敗' }
  finally { responding.value = false }
}
</script>

<template>
  <div class="public-container portal-page">
    <div v-if="loading" class="public-state">正在確認應徵進度…</div>
    <div v-else-if="error" class="public-state"><h2>無法開啟進度</h2><p>{{ error }}</p></div>
    <template v-else>
      <section class="portal-summary"><p class="eyebrow">APPLICATION STATUS</p><h1>{{ application.candidateName }}，您好</h1>
        <p>您應徵的「{{ application.jobTitle }}」目前狀態</p><strong>{{ application.statusLabel }}</strong></section>
      <section class="timeline-card"><h2>應徵歷程</h2><ol>
        <li v-for="item in application.timeline" :key="`${item.status}-${item.occurredAt}`"><span></span><div><strong>{{ item.label }}</strong><time>{{ new Date(item.occurredAt).toLocaleString('zh-TW') }}</time></div></li>
      </ol><p class="privacy-note">此頁只顯示求職者可見狀態，不包含內部評分、備註與決策原因。</p></section>
      <section v-if="slots.length" class="timeline-card slot-picker"><h2>請選擇面試時間</h2><p>以下時段尚未正式保留，請選擇最適合你的時間。</p><article v-for="slot in slots" :key="slot.id"><div><strong>{{ new Date(slot.startsAt).toLocaleString('zh-TW') }}</strong><span>{{ slot.format }} · {{ slot.location || '地點將另行通知' }}</span></div><button class="primary-button" :disabled="selecting" @click="choose(slot)">{{ selecting === slot.id ? '確認中…' : '選擇此時段' }}</button></article></section>
      <p v-if="actionMessage" class="alert-message is-success">{{ actionMessage }}</p>
      <p v-if="actionError" class="alert-message is-error">{{ actionError }}</p>

      <section class="timeline-card resume-upload">
        <div><h2>PDF 履歷</h2><p>可在招募期間更新履歷；新版本會取代公司端的舊版本。</p></div>
        <p v-if="resumes.length"><strong>目前版本：</strong>{{ resumes[0].originalFilename }}（{{ Math.ceil(resumes[0].fileSize / 1024) }} KB）</p>
        <input id="resume-file" type="file" accept="application/pdf,.pdf" @change="resumeFile = $event.target.files[0] || null">
        <small>只接受 PDF，檔案上限 5 MB。</small>
        <button class="primary-button" :disabled="!resumeFile || uploading" @click="uploadResume">{{ uploading ? '上傳中…' : '更新 PDF 履歷' }}</button>
      </section>

      <section v-if="offer" class="timeline-card portal-offer">
        <div><p class="eyebrow">JOB OFFER</p><h2>{{ offer.title }}</h2><p>{{ offer.companyName }} 邀請你加入「{{ offer.jobTitle }}」。</p></div>
        <dl><div><dt>年薪</dt><dd>{{ Number(offer.annualSalary).toLocaleString() }} {{ offer.currency }}</dd></div><div><dt>預計到職日</dt><dd>{{ offer.proposedStartDate }}</dd></div><div><dt>回覆期限</dt><dd>{{ new Date(offer.expiresAt).toLocaleString('zh-TW') }}</dd></div><div><dt>Offer 狀態</dt><dd>{{ offer.status }}</dd></div></dl>
        <div class="portal-offer-message">{{ offer.message }}</div>
        <template v-if="offer.status === 'SENT'">
          <label class="offer-decline"><span>若要拒絕，請說明原因</span><textarea v-model.trim="declineReason" rows="3" maxlength="2000" /></label>
          <div class="portal-offer-actions"><button class="primary-button" :disabled="responding" @click="respondToOffer(true)">接受 Offer</button><button class="danger-button" :disabled="responding" @click="respondToOffer(false)">拒絕 Offer</button></div>
        </template>
        <p v-else class="alert-message is-success">此 Offer 已完成回覆：{{ offer.status === 'ACCEPTED' ? '已接受' : offer.status === 'DECLINED' ? '已拒絕' : '已結案' }}</p>
      </section>
    </template>
  </div>
</template>
