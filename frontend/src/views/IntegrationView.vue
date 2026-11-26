<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import {
  getCalendarSyncJobs,
  getEmailDeliveries,
  retryCalendarSync,
  retryEmail,
} from "@/services/integrationService";
const auth = useAuthStore(),
  emails = ref({ content: [] }),
  syncJobs = ref({ content: [] }),
  error = ref(""),
  message = ref("");
const canManage = computed(() => auth.hasAnyRole(["HR"]));
async function load() {
  try {
    if (canManage.value) {
      emails.value = await getEmailDeliveries();
      syncJobs.value = await getCalendarSyncJobs();
    }
  } catch (e) {
    error.value = e.response?.data?.message || "無法取得整合狀態";
  }
}
async function retryMail(id) {
  error.value = "";
  try {
    await retryEmail(id);
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "無法重試 Email 寄送";
  }
}
async function retrySync(id) {
  error.value = "";
  try {
    await retryCalendarSync(id);
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "無法重試 Calendar 同步";
  }
}
onMounted(load);
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">DELIVERY ACTIVITY</p>
        <h1>通知與同步紀錄</h1>
        <p>查看 Email 寄送與行事曆同步結果；失敗的工作可在此重試。</p>
      </div>
    </div>
    <p v-if="error" class="alert-message is-error">{{ error }}</p>
    <p v-if="message" class="alert-message is-success">{{ message }}</p>
    <template v-if="canManage"
      ><article class="data-card integration-table">
        <h2>Email 寄送紀錄</h2>
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>建立時間</th>
                <th>收件者</th>
                <th>主旨</th>
                <th>狀態</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="j in emails.content" :key="j.id">
                <td>{{ new Date(j.createdAt).toLocaleString("zh-TW") }}</td>
                <td>{{ j.recipientEmail }}</td>
                <td>{{ j.subject }}</td>
                <td>{{ j.status }}</td>
                <td>
                  <button
                    v-if="j.status === 'FAILED'"
                    class="secondary-button"
                    @click="retryMail(j.id)"
                  >
                    重試
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
      <article class="data-card integration-table">
        <h2>Google Calendar 同步紀錄</h2>
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>建立時間</th>
                <th>使用者</th>
                <th>面試</th>
                <th>操作</th>
                <th>狀態</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="j in syncJobs.content" :key="j.id">
                <td>{{ new Date(j.createdAt).toLocaleString("zh-TW") }}</td>
                <td>{{ j.userName }}</td>
                <td>#{{ j.interviewId }}</td>
                <td>{{ j.operation }}</td>
                <td>{{ j.status }}</td>
                <td>
                  <button
                    v-if="j.status === 'FAILED'"
                    class="secondary-button"
                    @click="retrySync(j.id)"
                  >
                    重試
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article></template
    >
  </section>
</template>
