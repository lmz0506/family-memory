import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '@/api'
import type { UserInfo } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(JSON.parse(localStorage.getItem('user') || 'null'))

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const currentFamilyId = computed(() => user.value?.currentFamily?.id || null)

  // Actions
  const login = async (username: string, password: string) => {
    const res = await api.login({ username, password })
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    return res.data
  }

  const register = async (data: api.RegisterParams) => {
    const res = await api.register(data)
    return res.data
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const fetchCurrentUser = async () => {
    if (!token.value) return null
    try {
      const res = await api.getCurrentUser()
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    } catch (e) {
      logout()
      return null
    }
  }

  const updateProfile = async (data: api.UpdateProfileParams) => {
    const res = await api.updateProfile(data)
    user.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  return {
    token,
    user,
    isLoggedIn,
    currentFamilyId,
    login,
    register,
    logout,
    fetchCurrentUser,
    updateProfile
  }
})
