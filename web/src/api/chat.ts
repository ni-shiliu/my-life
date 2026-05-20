import request from './request'
import type { ChatMessage, ChatScene, GuestTokenResponse } from '@/types/chat'

export function ensureChatRoomApi(agentUuid: string, scene?: ChatScene) {
  return request.post<{ code: string; data: { roomId: string } }>('/v1/chat/room', null, {
    params: { agentUuid, scene }
  })
}

export function getChatHistoryApi(roomId: string) {
  return request.post<{ code: string; data: ChatMessage[] }>('/v1/chat/history', null, {
    params: { roomId }
  })
}

export function clearChatHistoryApi(roomId: string) {
  return request.delete<{ code: string; data: null }>('/v1/chat/clear', {
    params: { roomId }
  })
}

export function clearMemoryApi(roomId: string) {
  return request.delete<{ code: string; data: null }>('/v1/chat/clear-memory', {
    params: { roomId }
  })
}

export async function getGuestTokenApi(): Promise<GuestTokenResponse> {
  const { data } = await request.get<{ code: string; data: GuestTokenResponse }>('/v1/chat/guest/token')
  return data.data!
}

export interface ClaimGuestResult {
  count: number
  agentUuids: string[]
}

// 直接 fetch 调用，绕开全局响应拦截器：guest_token 被后端判为 TOKEN_INVALID 时
// 不应当把当前已登录用户踢回登录页。
export async function claimGuestHistoryApi(guestToken: string): Promise<ClaimGuestResult> {
  const userToken = localStorage.getItem('ml_access_token') || ''
  const baseUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin
  const resp = await fetch(`${baseUrl}/v1/chat/claim-guest`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${userToken}`
    },
    body: JSON.stringify({ guestToken })
  })
  if (!resp.ok) {
    throw new Error(`claim failed: ${resp.status}`)
  }
  const json = await resp.json()
  if (json?.code !== '0' && json?.code !== 0) {
    throw new Error(json?.message || 'claim failed')
  }
  return json.data as ClaimGuestResult
}
