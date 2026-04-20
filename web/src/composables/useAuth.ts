import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { showToast } from '@/components/auth/toast-state'

export function useAuth() {
  const userStore = useUserStore()
  const router = useRouter()
  const loading = ref(false)
  const errorMessage = ref('')

  async function login(phone: string, password: string) {
    loading.value = true
    errorMessage.value = ''
    try {
      await userStore.login(phone, password)
      const redirect = (router.currentRoute.value.query.redirect as string) || '/'
      router.push(redirect)
    } catch (e: any) {
      errorMessage.value = e.message || '登录失败'
      showToast(errorMessage.value, 'error')
    } finally {
      loading.value = false
    }
  }

  async function register(phone: string, password: string, nickName?: string) {
    loading.value = true
    errorMessage.value = ''
    try {
      await userStore.register(phone, password, nickName)
      router.push('/')
    } catch (e: any) {
      errorMessage.value = e.message || '注册失败'
      showToast(errorMessage.value, 'error')
    } finally {
      loading.value = false
    }
  }

  function logout() {
    userStore.resetUser()
    router.push('/login')
  }

  return { login, register, logout, loading, errorMessage }
}
