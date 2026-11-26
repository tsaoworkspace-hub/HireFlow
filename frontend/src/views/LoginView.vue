<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

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
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span class="brand-mark">H</span>
        <span>HireFlow</span>
      </div>
      <p class="eyebrow">SECURE WORKSPACE</p>
      <h1>登入招募工作台</h1>
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
    </section>

    <aside class="login-story">
      <p class="eyebrow">ROLE-BASED ACCESS</p>
      <h2>每個角色，只看見該做的事</h2>
      <p>登入狀態保存在伺服器 Session；即使繞過前端畫面，後端仍會再次驗證權限。</p>
      <ul>
        <li>未登入受保護 API 回傳 401</li>
        <li>角色不足回傳 403</li>
        <li>密碼使用 BCrypt 雜湊保存</li>
      </ul>
    </aside>
  </main>
</template>
