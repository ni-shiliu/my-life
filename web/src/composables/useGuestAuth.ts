import { ref } from 'vue'
import { getGuestTokenApi } from '@/api/chat'

const GUEST_TOKEN_KEY = 'guest_token'

export function useGuestAuth() {
  const guestToken = ref<string>(localStorage.getItem(GUEST_TOKEN_KEY) || '')

  async function fetchGuestToken() {
    const result = await getGuestTokenApi()
    guestToken.value = result.token
    localStorage.setItem(GUEST_TOKEN_KEY, result.token)
    return result.token
  }

  function clearGuestToken() {
    guestToken.value = ''
    localStorage.removeItem(GUEST_TOKEN_KEY)
  }

  async function getOrFetchToken(): Promise<string> {
    if (guestToken.value) {
      return guestToken.value
    }
    return fetchGuestToken()
  }

  return { guestToken, fetchGuestToken, clearGuestToken, getOrFetchToken }
}
