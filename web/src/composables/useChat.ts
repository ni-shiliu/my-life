import { ref, type ComputedRef, type Ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useGuestAuth } from '@/composables/useGuestAuth'
import { clearChatHistoryApi, getChatHistoryApi } from '@/api/chat'
import { getToken } from '@/utils/storage'
import type { ChatMessage, ChatScene, StreamChunkPayload, StreamEndPayload, ErrorPayload } from '@/types/chat'

export function useChat(agentUuid: Ref<string | undefined>, scene?: Ref<ChatScene | undefined> | ComputedRef<ChatScene | undefined>) {
  const messages = ref<ChatMessage[]>([])
  const streaming = ref(false)
  const connected = ref(true) // SSE 无需维护连接状态，始终为 true
  const error = ref('')

  const userStore = useUserStore()
  const { getOrFetchToken } = useGuestAuth()

  function getApiBaseUrl(): string {
    return import.meta.env.VITE_API_BASE_URL || window.location.origin
  }

  async function sendMessage(text: string) {
    if (streaming.value) return
    if (!agentUuid.value) return

    // Add user message locally
    messages.value.push({
      role: 'USER',
      content: text,
      gmtCreated: new Date().toISOString()
    })

    // Add placeholder for assistant response
    messages.value.push({
      role: 'ASSISTANT',
      content: '',
      streaming: true,
      gmtCreated: new Date().toISOString()
    })

    streaming.value = true
    error.value = ''

    try {
      const token = await resolveToken()
      const response = await fetch(`${getApiBaseUrl()}/v1/chat/send`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ agentUuid: agentUuid.value, message: text, scene: scene?.value })
      })

      if (!response.ok) {
        throw new Error(`请求失败: ${response.status}`)
      }

      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (!data) continue
            handleSseData(data)
          }
        }
      }

      // Process remaining buffer
      if (buffer.startsWith('data:')) {
        const data = buffer.substring(5).trim()
        if (data) handleSseData(data)
      }
    } catch (e: any) {
      error.value = e.message || '发送失败'
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg?.streaming) {
        lastMsg.streaming = false
        if (!lastMsg.content) {
          lastMsg.content = error.value
        }
      }
    } finally {
      streaming.value = false
    }
  }

  function handleSseData(data: string) {
    try {
      const msg = JSON.parse(data)
      const type = msg.type
      const payload = msg.payload

      switch (type) {
        case 'STREAM_CHUNK': {
          const chunk = (payload as StreamChunkPayload).chunk
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'ASSISTANT') {
            lastMsg.content += chunk
          }
          break
        }
        case 'STREAM_END': {
          const endPayload = payload as StreamEndPayload
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'ASSISTANT') {
            lastMsg.content = endPayload.fullText || lastMsg.content
            lastMsg.streaming = false
          }
          break
        }
        case 'TOOL_CALL': {
          // Future: render tool call steps
          break
        }
        case 'ERROR': {
          const errorPayload = payload as ErrorPayload
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg?.streaming) {
            lastMsg.streaming = false
            if (!lastMsg.content) {
              lastMsg.content = errorPayload.message
            }
          }
          error.value = errorPayload.message
          break
        }
      }
    } catch {
      // ignore parse errors
    }
  }

  async function resolveToken(): Promise<string> {
    if (userStore.isLoggedIn) {
      return getToken() || ''
    }
    return getOrFetchToken()
  }

  async function loadHistory() {
    if (!agentUuid.value || !userStore.isLoggedIn) return
    try {
      const { data } = await getChatHistoryApi(agentUuid.value, scene?.value)
      if (data?.data) {
        messages.value = data.data.map(m => ({ ...m, streaming: false }))
      }
    } catch {
      // silently fail
    }
  }

  function clearMessages() {
    messages.value = []
  }

  async function clearHistory() {
    if (!agentUuid.value) return
    try {
      await clearChatHistoryApi(agentUuid.value, scene?.value)
    } catch {
      // API 失败仍清空前端，后端数据会在下次加载时重新拉取
    }
    messages.value = []
  }

  return {
    messages,
    streaming,
    connected,
    error,
    sendMessage,
    loadHistory,
    clearMessages,
    clearHistory
  }
}
