import { ref } from 'vue'
import { claimGuestHistoryApi, getGuestTokenApi } from '@/api/chat'

const GUEST_TOKEN_KEY = 'guest_token'
const GUEST_SESSION_ID_KEY = 'guest_session_id'

export function useGuestAuth() {
  const guestToken = ref<string>(localStorage.getItem(GUEST_TOKEN_KEY) || '')

  async function fetchGuestToken() {
    const result = await getGuestTokenApi()
    guestToken.value = result.token
    localStorage.setItem(GUEST_TOKEN_KEY, result.token)
    localStorage.setItem(GUEST_SESSION_ID_KEY, result.sessionId)
    return result.token
  }

  function clearGuestToken() {
    guestToken.value = ''
    localStorage.removeItem(GUEST_TOKEN_KEY)
    localStorage.removeItem(GUEST_SESSION_ID_KEY)
  }

  async function getOrFetchToken(): Promise<string> {
    if (guestToken.value) {
      return guestToken.value
    }
    return fetchGuestToken()
  }

  return { guestToken, fetchGuestToken, clearGuestToken, getOrFetchToken }
}

/**
 * 登录后/已登录态启动时调用，把残留在 localStorage 的访客对话归并到当前账号。
 * 任何错误都吞掉、并清掉本地 guest_token，确保不阻断正常流程。
 */
export async function claimGuestHistoryIfPending(): Promise<void> {
  const token = localStorage.getItem(GUEST_TOKEN_KEY)
  if (!token) return
  try {
    await claimGuestHistoryApi(token)
  } catch (e) {
    console.warn('[guest-claim] failed, clearing token anyway', e)
  } finally {
    localStorage.removeItem(GUEST_TOKEN_KEY)
    localStorage.removeItem(GUEST_SESSION_ID_KEY)
  }
}
