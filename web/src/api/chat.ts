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
