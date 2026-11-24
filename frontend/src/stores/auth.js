import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import * as authService from '@/services/authService'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const initialized = ref(false)
  const isAuthenticated = computed(() => Boolean(user.value))

  async function signIn(credentials) {
    user.value = await authService.login(credentials)
    initialized.value = true
  }

  async function signOut() {
    try {
      await authService.logout()
    } finally {
      user.value = null
      initialized.value = true
    }
  }

  async function restoreSession() {
    if (initialized.value) return
    try {
      user.value = await authService.getCurrentUser()
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  function hasAnyRole(roles = []) {
    if (!roles.length || user.value?.roles?.includes('ADMIN')) return true
    return roles.some((role) => user.value?.roles?.includes(role))
  }

  return { user, initialized, isAuthenticated, signIn, signOut, restoreSession, hasAnyRole }
})
