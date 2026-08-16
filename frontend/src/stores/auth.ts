import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import type { CurrentUser, LoginInput } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const initialized = ref(false)
  const isAuthenticated = computed(() => user.value !== null)

  async function signIn(input: LoginInput) {
    user.value = await authApi.login(input)
    initialized.value = true
  }

  async function loadCurrentUser() {
    try {
      user.value = await authApi.getCurrentUser()
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function signOut() {
    try {
      await authApi.logout()
    } finally {
      user.value = null
      initialized.value = true
    }
  }

  function updateDisplayName(displayName: string) {
    if (user.value) {
      user.value = { ...user.value, displayName }
    }
  }

  return {
    user,
    initialized,
    isAuthenticated,
    signIn,
    signOut,
    loadCurrentUser,
    updateDisplayName,
  }
})
