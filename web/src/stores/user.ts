import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, clearToken } from '@/utils/storage'
import { loginApi, registerApi, getUserInfoApi } from '@/api/user'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(getToken() || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(phone: string, password: string) {
    const { data } = await loginApi({ phone, password })
    if (data.code !== '0') {
      throw new Error(data.message)
    }
    const d = data.data!
    accessToken.value = d.accessToken
    userInfo.value = { userId: d.userId, phone: d.phone, nickName: d.nickName }
    setToken(d.accessToken)
  }

  async function register(phone: string, password: string, nickName?: string) {
    const { data } = await registerApi({ phone, password, nickName })
    if (data.code !== '0') {
      throw new Error(data.message)
    }
    const d = data.data!
    accessToken.value = d.accessToken
    userInfo.value = { userId: d.userId, phone: d.phone, nickName: d.nickName }
    setToken(d.accessToken)
  }

  async function fetchUserInfo() {
    const { data } = await getUserInfoApi()
    if (data.data) {
      userInfo.value = {
        userId: data.data.userId,
        phone: data.data.phone,
        nickName: data.data.nickName
      }
    }
  }

  function resetUser() {
    accessToken.value = ''
    userInfo.value = null
    clearToken()
  }

  return { accessToken, userInfo, isLoggedIn, login, register, fetchUserInfo, resetUser }
})
