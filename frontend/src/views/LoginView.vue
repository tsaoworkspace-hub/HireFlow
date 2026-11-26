<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ email: 'hr@hireflow.local', password: 'HireFlow123!' })

const demoAccounts = [
  { role: 'HR', email: 'hr@hireflow.local' },
  { role: '用人主管', email: 'manager@hireflow.local' },
  { role: '面試官', email: 'interviewer@hireflow.local' },
  { role: '管理員', email: 'admin@hireflow.local' },
]

function selectDemo(email) {
  form.email = email
  form.password = 'HireFlow123!'
  errorMessage.value = ''
}

async function submit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await authStore.signIn(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '登入失敗，請稍後再試'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-panel">
      <p class="eyebrow">企業工作台</p>
      <h1>歡迎回到 HireFlow</h1>
      <p class="login-intro">使用 Demo 帳號體驗不同角色的導覽與權限。</p>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>Email</span>
          <input v-model.trim="form.email" type="email" autocomplete="username" required>
        </label>
        <label>
          <span>密碼</span>
          <input v-model="form.password" type="password" autocomplete="current-password" required>
        </label>
        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <button class="primary-button" type="submit" :disabled="submitting">
          {{ submitting ? '登入中…' : '登入' }}
        </button>
      </form>

      <div class="demo-accounts">
        <span>快速選擇 Demo 身分</span>
        <div>
          <button
            v-for="account in demoAccounts"
            :key="account.email"
            type="button"
            @click="selectDemo(account.email)"
          >
            {{ account.role }}
          </button>
        </div>
        <small>共用密碼：HireFlow123!</small>
      </div>
      <p class="login-candidate-link">想應徵職缺？<RouterLink to="/careers">前往招募頁面 →</RouterLink></p>
    </section>

    <aside class="login-story">
      <p class="eyebrow">HIRING, MADE CLEAR</p>
      <h2>讓每一步招募，<br>都有清楚的進度。</h2>
      <p>從職缺、應徵到面試與決策，團隊在同一個工作台協作。</p>
      <RouterLink class="login-story-link" to="/careers">查看公開職缺 <span aria-hidden="true">↗</span></RouterLink>
    </aside>
  </div>
</template>
