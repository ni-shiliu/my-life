import request from './request'
import type { ChatMessage, ChatScene, GuestTokenResponse } from '@/types/chat'

export function getChatHistoryApi(agentUuid: string, scene?: ChatScene) {
  return request.post<{ code: string; data: ChatMessage[] }>('/v1/chat/history', null, {
    params: { agentUuid, scene }
  })
}

export function clearChatHistoryApi(agentUuid: string, scene?: ChatScene) {
  return request.delete<{ code: string; data: null }>('/v1/chat/clear', {
    params: { agentUuid, scene }
  })
}

export async function getGuestTokenApi(): Promise<GuestTokenResponse> {
  const { data } = await request.get<{ code: string; data: GuestTokenResponse }>('/v1/chat/guest/token')
  return data.data!
}
